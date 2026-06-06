package com.italiarevenge.iRShop.gui;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.ConfigManager;
import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.economy.EconomyManager;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.model.ShopItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemListGui extends BaseGui {

    private final Shop shop;
    private final ShopCategory category;
    private final Layout layout;
    private final MessageManager msg;
    private final EconomyManager economy;
    private final ConfigManager config;

    private int page;
    private final ShopItem[] slotItems;

    public ItemListGui(Player player, Shop shop, ShopCategory category, int page) {
        super(player);
        this.shop     = shop;
        this.category = category;
        this.page     = page;
        this.layout   = IRShop.get().getLayoutLoader().get(shop.getLayout());
        this.msg      = IRShop.get().getMessageManager();
        this.economy  = IRShop.get().getEconomyManager();
        this.config   = IRShop.get().getConfigManager();
        this.slotItems = new ShopItem[layout.rows * 9];
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(null, layout.rows * 9,
                MessageManager.parse(category.getDisplayName()));
        render();
        player.openInventory(inventory);
        GuiListener.register(player, this);
    }

    private void render() {
        inventory.clear();
        Arrays.fill(slotItems, null);
        fillBackground();

        List<ShopItem> items = category.getItems();
        int perPage    = layout.itemSlots.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / perPage));
        page = Math.min(Math.max(page, 0), totalPages - 1);
        int offset = page * perPage;

        for (int i = 0; i < perPage; i++) {
            int idx = offset + i;
            if (idx >= items.size()) break;
            ShopItem shopItem = items.get(idx);
            int slot = layout.itemSlots.get(i);
            inventory.setItem(slot, buildItemStack(shopItem));
            slotItems[slot] = shopItem;
        }

        // Navigation buttons
        if (layout.slotBack >= 0)
            inventory.setItem(layout.slotBack, navItem(Material.ARROW, msg.getRaw("gui.back-name")));
        if (layout.slotClose >= 0)
            inventory.setItem(layout.slotClose, navItem(Material.BARRIER, msg.getRaw("gui.close-name")));

        if (page > 0 && layout.slotPrev >= 0) {
            List<Component> lore = msg.getList("gui.prev-page-lore",
                    Placeholder.parsed("current", String.valueOf(page + 1)),
                    Placeholder.parsed("total",   String.valueOf(totalPages)));
            inventory.setItem(layout.slotPrev, navItemLore(Material.PAPER, msg.getRaw("gui.prev-page-name"), lore));
        }
        if (page < totalPages - 1 && layout.slotNext >= 0) {
            List<Component> lore = msg.getList("gui.next-page-lore",
                    Placeholder.parsed("current", String.valueOf(page + 1)),
                    Placeholder.parsed("total",   String.valueOf(totalPages)));
            inventory.setItem(layout.slotNext, navItemLore(Material.PAPER, msg.getRaw("gui.next-page-name"), lore));
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot       = event.getRawSlot();
        ClickType type = event.getClick();

        if (slot == layout.slotBack) {
            new CategoryListGui(player, shop).open();
            return;
        }
        if (slot == layout.slotClose) {
            player.closeInventory();
            return;
        }
        if (slot == layout.slotPrev) { page--; render(); return; }
        if (slot == layout.slotNext) { page++; render(); return; }

        ShopItem shopItem = (slot >= 0 && slot < slotItems.length) ? slotItems[slot] : null;
        if (shopItem == null) return;

        if (type == ClickType.LEFT || type == ClickType.SHIFT_LEFT) {
            if (!shopItem.isBuyable()) {
                player.sendMessage(msg.get("purchase.not-for-sale"));
                return;
            }
            // Shift+click = quick-buy 1 (skip quantity GUI)
            if (type == ClickType.SHIFT_LEFT && config.isQuickBuy()) {
                executeBuy(shopItem, 1);
            } else {
                new QuantityGui(player, shop, category, shopItem, this).open();
            }
        } else if (type == ClickType.RIGHT) {
            if (!shopItem.isSellable()) { player.sendMessage(msg.get("sell.not-sellable")); return; }
            executeSell(shopItem, 1);
        } else if (type == ClickType.SHIFT_RIGHT) {
            if (!shopItem.isSellable()) { player.sendMessage(msg.get("sell.not-sellable")); return; }
            executeSell(shopItem, countInInventory(shopItem.getMaterial()));
        }
    }

    public void executeBuy(ShopItem shopItem, int amount) {
        if (!economy.isAvailable()) {
            player.sendMessage(msg.get("economy.vault-not-found"));
            return;
        }
        double total = shopItem.getBuyPrice() * amount;
        if (!economy.has(player, total)) {
            double needed = total - economy.getBalance(player);
            player.sendMessage(msg.get("purchase.insufficient-funds",
                    Placeholder.parsed("currency", "money"),
                    Placeholder.parsed("needed",   economy.format(needed))));
            return;
        }
        boolean hasSpace = false;
        for (ItemStack s : player.getInventory().getStorageContents()) {
            if (s == null || s.getType() == Material.AIR) { hasSpace = true; break; }
        }
        if (!hasSpace) {
            player.sendMessage(msg.get("purchase.inventory-full"));
            return;
        }

        economy.withdraw(player, total);
        player.getInventory().addItem(new ItemStack(shopItem.getMaterial(), amount));
        player.sendMessage(msg.get("purchase.success",
                Placeholder.parsed("amount",    String.valueOf(amount)),
                Placeholder.parsed("item-name", prettify(shopItem.getMaterial())),
                Placeholder.parsed("price",     economy.format(total))));
    }

    public void executeSell(ShopItem shopItem, int amount) {
        if (!economy.isAvailable()) {
            player.sendMessage(msg.get("economy.vault-not-found"));
            return;
        }
        int sellAmount = Math.min(amount, countInInventory(shopItem.getMaterial()));
        if (sellAmount <= 0) {
            player.sendMessage(msg.get("sell.no-items"));
            return;
        }
        double total = shopItem.getSellPrice() * sellAmount;
        removeFromInventory(shopItem.getMaterial(), sellAmount);
        economy.deposit(player, total);
        player.sendMessage(msg.get("sell.success",
                Placeholder.parsed("amount",    String.valueOf(sellAmount)),
                Placeholder.parsed("item-name", prettify(shopItem.getMaterial())),
                Placeholder.parsed("price",     economy.format(total))));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private void fillBackground() {
        ItemStack bg = new ItemStack(layout.bgMaterial);
        ItemMeta m   = bg.getItemMeta();
        m.displayName(Component.empty());
        bg.setItemMeta(m);
        for (int i = 0; i < layout.rows * 9; i++) inventory.setItem(i, bg);
    }

    private ItemStack buildItemStack(ShopItem shopItem) {
        ItemStack item = new ItemStack(shopItem.getMaterial());
        ItemMeta meta  = item.getItemMeta();

        if (shopItem.getCustomName() != null) {
            meta.displayName(MessageManager.parse(shopItem.getCustomName()));
        } else {
            meta.displayName(MessageManager.parse("<white>" + prettify(shopItem.getMaterial())));
        }

        List<Component> lore = new ArrayList<>();
        for (String line : shopItem.getCustomLore()) lore.add(MessageManager.parse(line));

        if (shopItem.isBuyable()) {
            lore.addAll(msg.getList("gui.item-buy-lore-append",
                    Placeholder.parsed("buy-price", economy.format(shopItem.getBuyPrice())),
                    Placeholder.parsed("currency",  "money")));
        } else {
            String raw = msg.raw().getString("gui.item-not-for-sale", "<red>✗ Not for sale");
            lore.add(MessageManager.parse(raw));
        }

        if (shopItem.isSellable()) {
            lore.addAll(msg.getList("gui.item-sell-lore-append",
                    Placeholder.parsed("sell-price", economy.format(shopItem.getSellPrice())),
                    Placeholder.parsed("currency",   "money")));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack navItem(Material mat, Component name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack navItemLore(Material mat, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private int countInInventory(Material material) {
        int count = 0;
        for (ItemStack s : player.getInventory().getStorageContents())
            if (s != null && s.getType() == material) count += s.getAmount();
        return count;
    }

    private void removeFromInventory(Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            if (contents[i] == null || contents[i].getType() != material) continue;
            if (contents[i].getAmount() <= remaining) {
                remaining -= contents[i].getAmount();
                contents[i] = null;
            } else {
                contents[i].setAmount(contents[i].getAmount() - remaining);
                remaining = 0;
            }
        }
        player.getInventory().setStorageContents(contents);
    }

    private String prettify(Material mat) {
        String[] words = mat.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return sb.toString().trim();
    }
}
