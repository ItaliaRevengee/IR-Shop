package com.italiarevenge.iRShop.gui.admin;

import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.gui.BaseGui;
import com.italiarevenge.iRShop.gui.GuiListener;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.model.ShopItem;
import com.italiarevenge.iRShop.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Admin GUI for editing the buy/sell price of a single variant.
 *
 * Layout (27 slots = 3 rows):
 *  [bg][bg][bg][bg][ITEM][bg][bg][bg][bg]   0-8
 *  [bg][BUY][bg][bg][bg][bg][SELL][bg][bg]  9-17
 *  [BACK][bg][bg][bg][bg][bg][bg][bg][bg]  18-26
 */
public class AdminVariantEditGui extends BaseGui {

    private static final int SIZE      = 27;
    private static final int SLOT_ITEM = 4;
    private static final int SLOT_BUY  = 10;
    private static final int SLOT_SELL = 16;
    private static final int SLOT_BACK = 18;

    private final Shop shop;
    private final ShopCategory category;
    private final ShopItem parentItem;
    private final int itemIndex;
    private final ShopItem variant;
    private final int variantIndex;
    private final int itemPage;
    private final int variantPage;

    public AdminVariantEditGui(Player player, Shop shop, ShopCategory category,
                                ShopItem parentItem, int itemIndex,
                                ShopItem variant, int variantIndex,
                                int itemPage, int variantPage) {
        super(player);
        this.shop         = shop;
        this.category     = category;
        this.parentItem   = parentItem;
        this.itemIndex    = itemIndex;
        this.variant      = variant;
        this.variantIndex = variantIndex;
        this.itemPage     = itemPage;
        this.variantPage  = variantPage;
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(null, SIZE,
                MessageManager.parse("<dark_gray>[Admin] <aqua>Modifica Variante"));
        fillBackground();

        inventory.setItem(SLOT_ITEM, ItemBuilder.buildDisplay(variant));
        inventory.setItem(SLOT_BUY,  buildPriceButton(true));
        inventory.setItem(SLOT_SELL, buildPriceButton(false));
        inventory.setItem(SLOT_BACK, navItem(Material.ARROW, MessageManager.parse("<yellow>← Indietro")));

        player.openInventory(inventory);
        GuiListener.register(player, this);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        switch (slot) {
            case SLOT_BACK -> new AdminVariantListGui(player, shop, category, parentItem,
                    itemIndex, itemPage, variantPage).open();
            case SLOT_BUY  -> {
                player.closeInventory();
                AdminChatInputListener.startEditVariantBuy(player, shop.getId(), category.getId(),
                        itemIndex, variantIndex, itemPage, variantPage);
            }
            case SLOT_SELL -> {
                player.closeInventory();
                AdminChatInputListener.startEditVariantSell(player, shop.getId(), category.getId(),
                        itemIndex, variantIndex, itemPage, variantPage);
            }
        }
    }

    // ── builders ─────────────────────────────────────────────────────────────

    private ItemStack buildPriceButton(boolean isBuy) {
        Material mat   = isBuy ? Material.EMERALD : Material.GOLD_INGOT;
        double price   = isBuy ? variant.getBuyPrice() : variant.getSellPrice();
        String label   = isBuy ? "<green>Prezzo Acquisto" : "<gold>Prezzo Vendita";
        String current = price > 0 ? String.valueOf(price) : "<red>Non disponibile";

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MessageManager.parse(label));
        meta.lore(List.of(
                MessageManager.parse("<gray>Attuale: <white>" + current),
                Component.empty(),
                MessageManager.parse("<yellow>Click <gray>per modificare")));
        item.setItemMeta(meta);
        return item;
    }

    private void fillBackground() {
        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = bg.getItemMeta();
        meta.displayName(Component.empty());
        bg.setItemMeta(meta);
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, bg);
    }

    private static ItemStack navItem(Material mat, Component name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
