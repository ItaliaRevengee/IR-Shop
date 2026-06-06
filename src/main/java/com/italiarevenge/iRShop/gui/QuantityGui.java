package com.italiarevenge.iRShop.gui;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.model.ShopItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class QuantityGui extends BaseGui {

    private static final int[] QUANTITIES = {1, 4, 8, 16, 32, 64};
    private static final int[] QTY_SLOTS  = {10, 11, 12, 14, 15, 16};

    private static final int SLOT_PREVIEW = 4;
    private static final int SLOT_BACK    = 22;

    private final Shop shop;
    private final ShopCategory category;
    private final ShopItem shopItem;
    private final ItemListGui parent;
    private final MessageManager msg;

    public QuantityGui(Player player, Shop shop, ShopCategory category,
                       ShopItem shopItem, ItemListGui parent) {
        super(player);
        this.shop     = shop;
        this.category = category;
        this.shopItem = shopItem;
        this.parent   = parent;
        this.msg      = IRShop.get().getMessageManager();
    }

    @Override
    public void open() {
        Component title = msg.getRaw("quantity-gui.title");
        inventory = Bukkit.createInventory(null, 27, title);

        // Glass filler
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        gm.displayName(Component.empty());
        glass.setItemMeta(gm);
        for (int i = 0; i < 27; i++) inventory.setItem(i, glass);

        // Item preview — slot 4 (centre top row), stack of 1
        inventory.setItem(SLOT_PREVIEW, buildPreview());

        // Quantity buttons — each is the real item with matching stack size
        for (int i = 0; i < QUANTITIES.length; i++) {
            inventory.setItem(QTY_SLOTS[i], buildQtyButton(i));
        }

        // Back button
        inventory.setItem(SLOT_BACK, navItem(Material.BARRIER, msg.getRaw("gui.back-name")));

        player.openInventory(inventory);
        GuiListener.register(player, this);
    }

    private ItemStack buildPreview() {
        ItemStack item = new ItemStack(shopItem.getMaterial(), 1);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MessageManager.parse("<white>" + prettify(shopItem.getMaterial())));
        meta.lore(List.of(
                MessageManager.parse("<gray>Base price: <green>"
                        + IRShop.get().getEconomyManager().format(shopItem.getBuyPrice()))
        ));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == SLOT_BACK) {
            parent.open();
            return;
        }

        for (int i = 0; i < QTY_SLOTS.length; i++) {
            if (slot == QTY_SLOTS[i]) {
                int qty = QUANTITIES[i];
                parent.executeBuy(shopItem, qty);
                // After buy: stay on quantity GUI so player can buy again
                open();
                return;
            }
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private ItemStack buildQtyButton(int index) {
        int qty   = QUANTITIES[index];
        // Cap display stack at the material's max stack size
        int displayAmt = Math.min(qty, shopItem.getMaterial().getMaxStackSize());
        double total = shopItem.getBuyPrice() * qty;
        String totalFormatted = IRShop.get().getEconomyManager().format(total);

        ItemStack item = new ItemStack(shopItem.getMaterial(), displayAmt);
        ItemMeta meta  = item.getItemMeta();

        meta.displayName(msg.getRaw("quantity-gui.button-name",
                Placeholder.parsed("qty", String.valueOf(qty))));

        meta.lore(msg.getList("quantity-gui.button-lore",
                Placeholder.parsed("qty",   String.valueOf(qty)),
                Placeholder.parsed("price", totalFormatted)));

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

    private String prettify(Material mat) {
        String[] words = mat.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return sb.toString().trim();
    }
}
