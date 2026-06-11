package com.italiarevenge.iRShop.gui;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.ConfigManager;
import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.model.ShopItem;
import com.italiarevenge.iRShop.util.ItemBuilder;
import com.italiarevenge.iRShop.util.ItemMatcher;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Sub-GUI shown when the player clicks a variant-group item.
 * Displays all variant items; left-click buys, right-click sells.
 */
public class VariantGui extends BaseGui implements TransactionHost {

    private final ShopItem parentItem;
    private final ItemListGui parentGui;
    private final MessageManager msg;
    private final ConfigManager config;

    private final int rows;
    private final int navRowStart;
    private final int slotBack;
    private final int slotClose;
    private final ShopItem[] slotItems;

    public VariantGui(Player player, ShopItem parentItem, ItemListGui parentGui) {
        super(player);
        this.parentItem = parentItem;
        this.parentGui  = parentGui;
        this.msg        = IRShop.get().getMessageManager();
        this.config     = IRShop.get().getConfigManager();

        int varCount = parentItem.getVariants().size();
        // Enough rows for all variants + 1 nav row (min 2, max 6)
        int contentRows = Math.max(1, (int) Math.ceil(varCount / 9.0));
        this.rows        = Math.min(6, contentRows + 1);
        this.navRowStart = (rows - 1) * 9;
        this.slotBack    = navRowStart + 4;
        this.slotClose   = navRowStart + 8;
        this.slotItems   = new ShopItem[rows * 9];
    }

    @Override
    public void open() {
        Component title = resolveTitle();
        inventory = Bukkit.createInventory(null, rows * 9, title);
        render();
        player.openInventory(inventory);
        playSound("open");
        GuiListener.register(player, this);
    }

    private void render() {
        inventory.clear();
        Arrays.fill(slotItems, null);
        fillBackground();

        List<ShopItem> variants = parentItem.getVariants();
        for (int i = 0; i < variants.size() && i < navRowStart; i++) {
            ShopItem variant = variants.get(i);
            inventory.setItem(i, ItemBuilder.buildDisplay(variant, player));
            slotItems[i] = variant;
        }

        inventory.setItem(slotBack,  navItem(Material.ARROW,   msg.getRaw("gui.back-name")));
        inventory.setItem(slotClose, navItem(Material.BARRIER, msg.getRaw("gui.close-name")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot       = event.getRawSlot();
        ClickType type = event.getClick();

        if (slot == slotBack)  { playSound("page-turn"); parentGui.open(); return; }
        if (slot == slotClose) { playSound("close");     player.closeInventory(); return; }

        ShopItem variant = (slot >= 0 && slot < slotItems.length) ? slotItems[slot] : null;
        if (variant == null) return;

        if (type == ClickType.LEFT || type == ClickType.SHIFT_LEFT) {
            if (!variant.isBuyable()) { playSound("error"); player.sendMessage(msg.get("purchase.not-for-sale")); return; }
            if (type == ClickType.SHIFT_LEFT && config.isQuickBuy()) {
                executeBuy(variant, 1);
            } else {
                new QuantityGui(player, variant, this).open();
            }
        } else if (type == ClickType.RIGHT) {
            if (!variant.isSellable()) { playSound("error"); player.sendMessage(msg.get("sell.not-sellable")); return; }
            executeSell(variant, 1);
        } else if (type == ClickType.SHIFT_RIGHT) {
            if (!variant.isSellable()) { playSound("error"); player.sendMessage(msg.get("sell.not-sellable")); return; }
            executeSell(variant, ItemMatcher.count(player, variant));
        }
    }

    // TransactionHost — delegate to the parent ItemListGui so transaction logic stays in one place
    @Override
    public void executeBuy(ShopItem shopItem, int amount) {
        parentGui.executeBuy(shopItem, amount);
    }

    private void executeSell(ShopItem variant, int amount) {
        parentGui.executeSell(variant, amount);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Component resolveTitle() {
        String raw = msg.raw().getString("gui.variant-gui-title", "<dark_gray>Choose <yellow>Variant");
        if (parentItem.getCustomName() != null) {
            raw = parentItem.getCustomName();
        }
        return MessageManager.parse(raw);
    }

    private void fillBackground() {
        ItemStack bg   = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  meta = bg.getItemMeta();
        meta.displayName(Component.empty());
        bg.setItemMeta(meta);
        for (int i = navRowStart; i < rows * 9; i++) inventory.setItem(i, bg);
    }

    private ItemStack navItem(Material mat, Component name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
