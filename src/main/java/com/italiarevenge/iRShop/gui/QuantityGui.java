package com.italiarevenge.iRShop.gui;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.model.ShopItem;
import com.italiarevenge.iRShop.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class QuantityGui extends BaseGui {

    private static final int[] QUANTITIES = {1, 4, 8, 16, 32, 64};
    private static final int[] QTY_SLOTS  = {10, 11, 12, 14, 15, 16};

    private static final int SLOT_PREVIEW = 4;
    private static final int SLOT_BACK    = 22;

    private final ShopItem shopItem;
    private final TransactionHost parent;
    private final MessageManager msg;

    public QuantityGui(Player player, ShopItem shopItem, TransactionHost parent) {
        super(player);
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
        playSound("open");
        GuiListener.register(player, this);
    }

    private ItemStack buildPreview() {
        return ItemBuilder.buildDisplay(shopItem);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == SLOT_BACK) {
            playSound("page-turn");
            parent.open();
            return;
        }

        for (int i = 0; i < QTY_SLOTS.length; i++) {
            if (slot == QTY_SLOTS[i]) {
                int qty = QUANTITIES[i];
                parent.executeBuy(shopItem, qty);
                return;
            }
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private ItemStack buildQtyButton(int index) {
        int qty        = QUANTITIES[index];
        int displayAmt = Math.min(qty, shopItem.getMaterial().getMaxStackSize());
        double total   = shopItem.getBuyPrice() * qty;
        String totalFormatted = IRShop.get().getEconomyManager().format(total);

        // Start from the real item (preserves custom name, enchants, NBT, etc.)
        ItemStack item = ItemBuilder.buildClean(shopItem);
        item.setAmount(displayAmt);

        // Only override lore to show qty/price; keep the item's own display name
        ItemMeta meta = item.getItemMeta();
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

}
