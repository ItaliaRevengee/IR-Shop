package com.italiarevenge.iRShop.gui.admin;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.gui.BaseGui;
import com.italiarevenge.iRShop.gui.GuiListener;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.model.ShopItem;
import com.italiarevenge.iRShop.util.CategorySaver;
import com.italiarevenge.iRShop.util.ItemBuilder;
import net.kyori.adventure.text.Component;
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

/**
 * Admin GUI that lists all variants of a variant-group item.
 * Left-click a variant → AdminVariantEditGui.
 * Shift+right-click a variant → remove it.
 * Hold item in hand + click Add → prompt for prices.
 */
public class AdminVariantListGui extends BaseGui {

    private static final int SIZE        = 54;
    private static final int CONTENT     = 45;
    private static final int SLOT_BACK   = 45;
    private static final int SLOT_ADD    = 46;
    private static final int SLOT_PREV   = 48;
    private static final int SLOT_CLOSE  = 49;
    private static final int SLOT_NEXT   = 50;

    private final Shop shop;
    private final ShopCategory category;
    private final ShopItem parentItem;
    private final int itemIndex;
    private final int itemPage;
    private int variantPage;

    private final int[] slotVariantIndex = new int[SIZE];

    public AdminVariantListGui(Player player, Shop shop, ShopCategory category,
                                ShopItem parentItem, int itemIndex, int itemPage, int variantPage) {
        super(player);
        this.shop        = shop;
        this.category    = category;
        this.parentItem  = parentItem;
        this.itemIndex   = itemIndex;
        this.itemPage    = itemPage;
        this.variantPage = variantPage;
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(null, SIZE,
                MessageManager.parse("<dark_gray>[Admin] <aqua>Varianti"));
        render();
        player.openInventory(inventory);
        GuiListener.register(player, this);
    }

    private void render() {
        inventory.clear();
        Arrays.fill(slotVariantIndex, -1);
        fillNavBackground();

        List<ShopItem> variants = parentItem.getVariants();
        int totalPages  = Math.max(1, (int) Math.ceil((double) variants.size() / CONTENT));
        variantPage = Math.min(Math.max(variantPage, 0), totalPages - 1);
        int offset = variantPage * CONTENT;

        for (int i = 0; i < CONTENT; i++) {
            int idx = offset + i;
            if (idx >= variants.size()) break;
            ShopItem variant = variants.get(idx);
            inventory.setItem(i, buildVariantDisplay(variant, idx));
            slotVariantIndex[i] = idx;
        }

        inventory.setItem(SLOT_BACK,  navItem(Material.ARROW,   MessageManager.parse("<yellow>← Indietro")));
        inventory.setItem(SLOT_CLOSE, navItem(Material.BARRIER, MessageManager.parse("<red>Chiudi")));

        ItemStack held = player.getInventory().getItemInMainHand();
        inventory.setItem(SLOT_ADD, held.getType().isAir() ? buildAddButtonEmpty() : buildAddButton(held));

        if (variantPage > 0)
            inventory.setItem(SLOT_PREV, navItemLore(Material.PAPER,
                    MessageManager.parse("<yellow>◀ Pagina precedente"),
                    List.of(MessageManager.parse("<gray>Pagina " + variantPage + " / " + totalPages))));
        if (variantPage < totalPages - 1)
            inventory.setItem(SLOT_NEXT, navItemLore(Material.PAPER,
                    MessageManager.parse("<yellow>Pagina successiva ▶"),
                    List.of(MessageManager.parse("<gray>Pagina " + (variantPage + 2) + " / " + totalPages))));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == SLOT_BACK)  { new AdminItemEditGui(player, shop, category, parentItem, itemIndex, itemPage).open(); return; }
        if (slot == SLOT_CLOSE) { player.closeInventory(); return; }
        if (slot == SLOT_PREV)  { variantPage--; render(); return; }
        if (slot == SLOT_NEXT)  { variantPage++; render(); return; }
        if (slot == SLOT_ADD)   { handleAdd(); return; }

        if (slot >= 0 && slot < CONTENT && slotVariantIndex[slot] >= 0) {
            int vIdx = slotVariantIndex[slot];
            ShopItem variant = parentItem.getVariants().get(vIdx);
            if (event.getClick() == ClickType.SHIFT_RIGHT) {
                handleRemove(vIdx);
            } else {
                new AdminVariantEditGui(player, shop, category, parentItem, itemIndex,
                        variant, vIdx, itemPage, variantPage).open();
            }
        }
    }

    private void handleAdd() {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            player.sendMessage(MessageManager.parse("<red>Tieni un oggetto in mano per aggiungerlo come variante."));
            return;
        }
        player.closeInventory();
        AdminChatInputListener.startAddVariant(player, held, shop.getId(), category.getId(),
                itemIndex, itemPage, variantPage);
    }

    private void handleRemove(int vIdx) {
        IRShop plugin = IRShop.get();
        CategorySaver.removeVariant(plugin, category.getId(), itemIndex, vIdx);
        plugin.reload();
        refreshAndReopen();
        player.sendMessage(MessageManager.parse("<green>Variante rimossa."));
    }

    private void refreshAndReopen() {
        IRShop plugin = IRShop.get();
        Shop updatedShop = plugin.getShopLoader().getShop(shop.getId());
        ShopCategory updatedCat = AdminChatInputListener.findCategory(plugin, shop.getId(), category.getId());
        if (updatedShop == null || updatedCat == null) { player.closeInventory(); return; }
        List<ShopItem> items = updatedCat.getItems();
        if (itemIndex >= 0 && itemIndex < items.size()) {
            new AdminVariantListGui(player, updatedShop, updatedCat, items.get(itemIndex),
                    itemIndex, itemPage, variantPage).open();
        } else {
            player.closeInventory();
        }
    }

    // ── item builders ────────────────────────────────────────────────────────

    private ItemStack buildVariantDisplay(ShopItem variant, int idx) {
        ItemStack base = ItemBuilder.buildDisplay(variant);
        ItemMeta meta = base.getItemMeta();
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MessageManager.parse("<dark_gray>Index: " + idx));
        lore.add(MessageManager.parse("<aqua>Click <gray>per modificare prezzi"));
        lore.add(MessageManager.parse("<red>Shift+Right <gray>per rimuovere"));
        meta.lore(lore);
        base.setItemMeta(meta);
        return base;
    }

    private ItemStack buildAddButton(ItemStack held) {
        ItemStack btn = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = btn.getItemMeta();
        meta.displayName(MessageManager.parse("<green>+ Aggiungi variante"));
        meta.lore(List.of(
                MessageManager.parse("<gray>Materiale: <white>" + held.getType().name()),
                Component.empty(),
                MessageManager.parse("<yellow>Click <gray>per aggiungere")));
        btn.setItemMeta(meta);
        return btn;
    }

    private ItemStack buildAddButtonEmpty() {
        ItemStack btn = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = btn.getItemMeta();
        meta.displayName(MessageManager.parse("<gray>+ Aggiungi variante"));
        meta.lore(List.of(MessageManager.parse("<dark_gray>Tieni un oggetto in mano")));
        btn.setItemMeta(meta);
        return btn;
    }

    // ── nav helpers ──────────────────────────────────────────────────────────

    private void fillNavBackground() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.empty());
        pane.setItemMeta(meta);
        for (int i = CONTENT; i < SIZE; i++) inventory.setItem(i, pane);
    }

    private static ItemStack navItem(Material mat, Component name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack navItemLore(Material mat, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
