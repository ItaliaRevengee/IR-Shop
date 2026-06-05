package com.italiarevenge.iRShop.gui;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.gui.framework.BaseGui;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.model.ShopItem;
import com.italiarevenge.iRShop.service.TransactionService;
import com.italiarevenge.iRShop.util.ItemUtil;
import com.italiarevenge.iRShop.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Purchase confirmation GUI — 3 rows.
 * Confirm (green) on the left, item preview in the middle, cancel (red) on the right.
 */
public class ConfirmGui extends BaseGui {

    private static final int CONFIRM_SLOT = 11;
    private static final int PREVIEW_SLOT = 13;
    private static final int CANCEL_SLOT  = 15;

    private final IRShop plugin;
    private final Shop shop;
    private final ShopCategory category;
    private final ShopItem shopItem;
    private final int amount;
    private final BaseGui returnGui;

    public ConfirmGui(
            @NotNull IRShop plugin,
            @NotNull Player player,
            @NotNull Shop shop,
            @NotNull ShopCategory category,
            @NotNull ShopItem shopItem,
            int amount,
            @NotNull BaseGui returnGui
    ) {
        super(player, 3, TextUtil.parse(
                plugin.getMessageManager().get("confirm-gui.title") != null
                        ? plugin.getMessageManager().get("confirm-gui.title")
                        : "<dark_gray>Confirm <green>Purchase</green>"));
        this.plugin = plugin;
        this.shop = shop;
        this.category = category;
        this.shopItem = shopItem;
        this.amount = amount;
        this.returnGui = returnGui;
        plugin.getGuiManager().register(player, this);
    }

    @Override
    public void build() {
        // Background filler
        var bg = ItemUtil.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) set(i, bg);

        // Item preview
        set(PREVIEW_SLOT, shopItem.getItem());

        // Effective price
        double unitPrice = plugin.getDiscountService().getEffectiveBuyPrice(shopItem.getBuyPrice(), player);
        double total = unitPrice * amount;
        String currency = shopItem.getCurrencyOverride() != null ? shopItem.getCurrencyOverride() : shop.getCurrencyId();
        String formatted = plugin.getEconomyManager().getProvider(currency).format(total);
        String itemName = shopItem.getItem().getType().name().replace('_', ' ').toLowerCase();

        // Confirm button
        set(CONFIRM_SLOT,
                ItemUtil.build(Material.LIME_STAINED_GLASS_PANE,
                        plugin.getMessageManager().get("confirm-gui.confirm-name") != null
                                ? plugin.getMessageManager().get("confirm-gui.confirm-name")
                                : "<green><bold>✔ Confirm</bold></green>",
                        "<gray>Item: <white>" + itemName,
                        "<gray>Amount: <white>" + amount,
                        "<gray>Total: <white>" + formatted,
                        "",
                        "<green>Click to confirm"),
                e -> confirmPurchase());

        // Cancel button
        set(CANCEL_SLOT,
                ItemUtil.build(Material.RED_STAINED_GLASS_PANE,
                        plugin.getMessageManager().get("confirm-gui.cancel-name") != null
                                ? plugin.getMessageManager().get("confirm-gui.cancel-name")
                                : "<red><bold>✗ Cancel</bold></red>",
                        "<red>Click to cancel"),
                e -> cancelPurchase());
    }

    private void confirmPurchase() {
        TransactionService.TransactionResult result =
                plugin.getTransactionService().buy(player, shop, category, shopItem, amount);

        String itemName = shopItem.getItem().getType().name().replace('_', ' ').toLowerCase();
        double total = plugin.getDiscountService().getEffectiveBuyPrice(shopItem.getBuyPrice(), player) * amount;
        String currency = shopItem.getCurrencyOverride() != null ? shopItem.getCurrencyOverride() : shop.getCurrencyId();
        String formatted = plugin.getEconomyManager().getProvider(currency).format(total);

        plugin.getMessageManager().send(player, result.messageKey(), Map.of(
                "item-name", itemName,
                "amount", String.valueOf(amount),
                "price", formatted,
                "currency", currency));

        returnGui.refresh();
        player.openInventory(returnGui.getInventory());
        plugin.getGuiManager().register(player, returnGui);
    }

    private void cancelPurchase() {
        plugin.getMessageManager().send(player, "purchase.cancelled");
        returnGui.refresh();
        player.openInventory(returnGui.getInventory());
        plugin.getGuiManager().register(player, returnGui);
    }
}
