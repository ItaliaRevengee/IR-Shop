package com.italiarevenge.iRShop.gui;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.ConfigManager;
import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.economy.EconomyManager;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.model.ShopItem;
import com.italiarevenge.iRShop.util.ItemBuilder;
import com.italiarevenge.iRShop.util.ItemMatcher;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class ItemListGui extends BaseGui implements TransactionHost {

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
            // Build display item using ItemBuilder (preserves all metadata + appends prices)
            inventory.setItem(slot, ItemBuilder.buildDisplay(shopItem));
            slotItems[slot] = shopItem;
        }

        // Navigation
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

        if (slot == layout.slotBack)  { playSound("navigate-back"); new CategoryListGui(player, shop).open(); return; }
        if (slot == layout.slotClose) { playSound("navigate-back"); player.closeInventory(); return; }
        if (slot == layout.slotPrev)  { playSound("page-turn"); page--; render(); return; }
        if (slot == layout.slotNext)  { playSound("page-turn"); page++; render(); return; }

        ShopItem shopItem = (slot >= 0 && slot < slotItems.length) ? slotItems[slot] : null;
        if (shopItem == null) return;

        // Variant group → open sub-GUI for any click
        if (shopItem.hasVariants()) {
            playSound("page-turn");
            new VariantGui(player, shopItem, this).open();
            return;
        }

        if (type == ClickType.LEFT || type == ClickType.SHIFT_LEFT) {
            if (!shopItem.isBuyable()) { playSound("error"); player.sendMessage(msg.get("purchase.not-for-sale")); return; }
            // Shift+click = quick-buy 1 (skip quantity GUI)
            if (type == ClickType.SHIFT_LEFT && config.isQuickBuy()) {
                executeBuy(shopItem, 1);
            } else {
                playSound("navigate-back");
                new QuantityGui(player, shopItem, this).open();
            }
        } else if (type == ClickType.RIGHT) {
            if (!shopItem.isSellable()) { playSound("error"); player.sendMessage(msg.get("sell.not-sellable")); return; }
            executeSell(shopItem, 1);
        } else if (type == ClickType.SHIFT_RIGHT) {
            if (!shopItem.isSellable()) { playSound("error"); player.sendMessage(msg.get("sell.not-sellable")); return; }
            executeSell(shopItem, ItemMatcher.count(player, shopItem));
        }
    }

    // ── transaction methods (called by QuantityGui too) ──────────────────────

    public void executeBuy(ShopItem shopItem, int amount) {
        if (!economy.isAvailable()) { player.sendMessage(msg.get("economy.vault-not-found")); return; }

        double total = shopItem.getBuyPrice() * amount;
        if (!economy.has(player, total)) {
            playSound("error");
            player.sendMessage(msg.get("purchase.insufficient-funds",
                    Placeholder.parsed("currency", "money"),
                    Placeholder.parsed("needed",   economy.format(total - economy.getBalance(player)))));
            return;
        }

        boolean hasSpace = false;
        for (ItemStack s : player.getInventory().getStorageContents()) {
            if (s == null || s.getType() == Material.AIR) { hasSpace = true; break; }
        }
        if (!hasSpace) { playSound("error"); player.sendMessage(msg.get("purchase.inventory-full")); return; }

        economy.withdraw(player, total);
        // Give the "clean" item (no price lore overlay) in the correct quantity
        ItemStack toGive = ItemBuilder.buildClean(shopItem);
        toGive.setAmount(Math.min(amount, toGive.getType().getMaxStackSize()));
        // If amount > maxStackSize, give in multiple stacks
        int remaining = amount;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, toGive.getType().getMaxStackSize());
            ItemStack stack = ItemBuilder.buildClean(shopItem);
            stack.setAmount(stackSize);
            player.getInventory().addItem(stack);
            remaining -= stackSize;
        }

        playSound("purchase");
        player.sendMessage(msg.get("purchase.success",
                Placeholder.parsed("amount",    String.valueOf(amount)),
                Placeholder.parsed("item-name", itemDisplayName(shopItem)),
                Placeholder.parsed("price",     economy.format(total))));
    }

    public void executeSell(ShopItem shopItem, int amount) {
        if (!economy.isAvailable()) { player.sendMessage(msg.get("economy.vault-not-found")); return; }

        // ItemMatcher handles both simple material matching and PDC-filtered matching
        int available = ItemMatcher.count(player, shopItem);
        int sellAmount = Math.min(amount, available);
        if (sellAmount <= 0) { playSound("error"); player.sendMessage(msg.get("sell.no-items")); return; }

        double total = shopItem.getSellPrice() * sellAmount;
        ItemMatcher.remove(player, shopItem, sellAmount);
        economy.deposit(player, total);
        playSound("sell");
        player.sendMessage(msg.get("sell.success",
                Placeholder.parsed("amount",    String.valueOf(sellAmount)),
                Placeholder.parsed("item-name", itemDisplayName(shopItem)),
                Placeholder.parsed("price",     economy.format(total))));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void fillBackground() {
        ItemStack bg = new ItemStack(layout.bgMaterial);
        ItemMeta m   = bg.getItemMeta();
        m.displayName(Component.empty());
        bg.setItemMeta(m);
        for (int i = 0; i < layout.rows * 9; i++) inventory.setItem(i, bg);
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

    private String itemDisplayName(ShopItem shopItem) {
        if (shopItem.getCustomName() != null) return shopItem.getCustomName();
        if (shopItem.isSerialized()) {
            try {
                ItemStack built = ItemBuilder.buildClean(shopItem);
                ItemMeta m = built.getItemMeta();
                if (m != null && m.hasDisplayName()) {
                    return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                            .plainText().serialize(m.displayName());
                }
            } catch (Exception ignored) {}
        }
        return prettify(shopItem.getMaterial());
    }

    private String prettify(Material mat) {
        String[] words = mat.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return sb.toString().trim();
    }
}
