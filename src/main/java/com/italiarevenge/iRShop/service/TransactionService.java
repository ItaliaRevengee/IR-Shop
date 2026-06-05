package com.italiarevenge.iRShop.service;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.api.economy.EconomyProvider;
import com.italiarevenge.iRShop.api.events.ShopPurchaseEvent;
import com.italiarevenge.iRShop.api.events.ShopSellEvent;
import com.italiarevenge.iRShop.model.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles the business logic for buy and sell transactions.
 *
 * <p>Flow: validate → fire event → charge/pay → update stock → persist transaction → update stats.
 */
public class TransactionService {

    /** Result of a transaction attempt. */
    public record TransactionResult(boolean success, @NotNull String messageKey) {}

    private final IRShop plugin;

    public TransactionService(@NotNull IRShop plugin) {
        this.plugin = plugin;
    }

    // ── Buy ───────────────────────────────────────────────────────────────────

    /**
     * Attempts to purchase {@code amount} units of {@code shopItem} for {@code player}.
     * All operations are performed on the caller's thread (must be main thread for
     * inventory manipulation) with async DB write kicked off afterwards.
     */
    @NotNull
    public TransactionResult buy(
            @NotNull Player player,
            @NotNull Shop shop,
            @NotNull ShopCategory category,
            @NotNull ShopItem shopItem,
            int amount
    ) {
        // ── Validation ────────────────────────────────────────────────────────
        if (!shopItem.isBuyable()) return new TransactionResult(false, "purchase.not-for-sale");
        if (!shopItem.isInStock()) return new TransactionResult(false, "purchase.out-of-stock");
        if (shopItem.getStock() != -1 && shopItem.getStock() < amount) amount = shopItem.getStock();

        double unitPrice = plugin.getDiscountService().getEffectiveBuyPrice(shopItem.getBuyPrice(), player);
        double totalPrice = unitPrice * amount;

        String currencyId = shopItem.getCurrencyOverride() != null
                ? shopItem.getCurrencyOverride() : shop.getCurrencyId();
        EconomyProvider economy = plugin.getEconomyManager().getProvider(currencyId);

        if (!economy.hasBalance(player, totalPrice)) {
            return new TransactionResult(false, "purchase.insufficient-funds");
        }

        // Inventory space check
        int available = countInventorySpace(player, shopItem.getItem());
        if (available < amount) {
            if (available == 0) return new TransactionResult(false, "purchase.inventory-full");
            amount = available;
            totalPrice = unitPrice * amount;
        }

        // ── Fire event ────────────────────────────────────────────────────────
        ShopPurchaseEvent event = new ShopPurchaseEvent(player, shop, category, shopItem, amount, totalPrice, currencyId);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            if (event.getCancelReason() != null) {
                plugin.getMessageManager().send(player, "general.no-permission");
            }
            return new TransactionResult(false, "purchase.cancelled");
        }

        int finalAmount = event.getAmount();
        double finalPrice = event.getTotalPrice();

        // ── Charge & deliver ──────────────────────────────────────────────────
        if (!economy.withdraw(player, finalPrice)) {
            return new TransactionResult(false, "purchase.insufficient-funds");
        }

        ItemStack toGive = shopItem.getItem();
        toGive.setAmount(finalAmount);
        player.getInventory().addItem(toGive);

        // ── Stock decrement ───────────────────────────────────────────────────
        shopItem.decrementStock(finalAmount);
        plugin.getItemService().updateStock(shopItem, shopItem.getStock());

        // ── Persist + stats (async) ───────────────────────────────────────────
        int af = finalAmount;
        double fp = finalPrice;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            ShopTransaction tx = new ShopTransaction(
                    UUID.randomUUID().toString(),
                    player.getUniqueId(), player.getName(),
                    shopItem.getId(), ShopTransaction.Type.BUY,
                    af, fp / af, fp, currencyId, Instant.now());
            plugin.getDatabaseManager().getDatabase().saveTransaction(tx);
            plugin.getPlayerService().recordPurchase(player, fp);
        });

        return new TransactionResult(true, "purchase.success");
    }

    // ── Sell ──────────────────────────────────────────────────────────────────

    @NotNull
    public TransactionResult sell(
            @NotNull Player player,
            @NotNull Shop shop,
            @NotNull ShopCategory category,
            @NotNull ShopItem shopItem,
            int amount
    ) {
        if (!shopItem.isSellable()) return new TransactionResult(false, "sell.not-sellable");

        // Count matching items in player inventory
        int held = countMatchingItems(player, shopItem.getItem());
        if (held == 0) return new TransactionResult(false, "sell.no-items");
        if (amount > held) amount = held;

        double unitPrice = plugin.getDiscountService().getEffectiveSellPrice(shopItem.getSellPrice(), player);
        double totalPrice = unitPrice * amount;

        String currencyId = shopItem.getCurrencyOverride() != null
                ? shopItem.getCurrencyOverride() : shop.getCurrencyId();
        EconomyProvider economy = plugin.getEconomyManager().getProvider(currencyId);

        // ── Fire event ────────────────────────────────────────────────────────
        ShopSellEvent event = new ShopSellEvent(player, shop, category, shopItem, amount, totalPrice, currencyId);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return new TransactionResult(false, "sell.cancelled");

        int finalAmount = event.getAmount();
        double finalPrice = event.getTotalPrice();

        // ── Remove items & pay ────────────────────────────────────────────────
        removeItems(player, shopItem.getItem(), finalAmount);
        economy.deposit(player, finalPrice);

        // ── Stock increment (if shop has limited stock) ───────────────────────
        shopItem.incrementStock(finalAmount);
        plugin.getItemService().updateStock(shopItem, shopItem.getStock());

        // ── Persist (async) ───────────────────────────────────────────────────
        int af = finalAmount;
        double fp = finalPrice;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            ShopTransaction tx = new ShopTransaction(
                    UUID.randomUUID().toString(),
                    player.getUniqueId(), player.getName(),
                    shopItem.getId(), ShopTransaction.Type.SELL,
                    af, fp / af, fp, currencyId, Instant.now());
            plugin.getDatabaseManager().getDatabase().saveTransaction(tx);
            plugin.getPlayerService().recordSale(player, fp);
        });

        return new TransactionResult(true, "sell.success");
    }

    // ── Inventory helpers ─────────────────────────────────────────────────────

    private int countInventorySpace(@NotNull Player player, @NotNull ItemStack item) {
        int space = 0;
        int maxStack = item.getMaxStackSize();
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType().isAir()) {
                space += maxStack;
            } else if (slot.isSimilar(item)) {
                space += maxStack - slot.getAmount();
            }
        }
        return space;
    }

    private int countMatchingItems(@NotNull Player player, @NotNull ItemStack item) {
        int count = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot != null && slot.isSimilar(item)) count += slot.getAmount();
        }
        return count;
    }

    private void removeItems(@NotNull Player player, @NotNull ItemStack item, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack slot = contents[i];
            if (slot != null && slot.isSimilar(item)) {
                if (slot.getAmount() <= remaining) {
                    remaining -= slot.getAmount();
                    contents[i] = null;
                } else {
                    slot.setAmount(slot.getAmount() - remaining);
                    remaining = 0;
                }
            }
        }
        player.getInventory().setStorageContents(contents);
    }

    /** Returns a future of transaction history for a player. */
    @NotNull
    public CompletableFuture<java.util.List<ShopTransaction>> getHistory(@NotNull Player player, int limit) {
        return plugin.getDatabaseManager().getDatabase().getTransactions(player.getUniqueId(), limit);
    }
}
