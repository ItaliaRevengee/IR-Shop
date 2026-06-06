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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminItemListGui extends BaseGui {

    private static final int SIZE       = 54;
    private static final int CONTENT    = 45; // slots 0-44
    private static final int SLOT_BACK  = 45;
    private static final int SLOT_ADD   = 46;
    private static final int SLOT_PREV  = 48;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_NEXT  = 50;

    private final Shop shop;
    private final ShopCategory category;
    private int page;

    // slot → item index in category.getItems()
    private final int[] slotIndex = new int[SIZE];

    public AdminItemListGui(Player player, Shop shop, ShopCategory category, int page) {
        super(player);
        this.shop     = shop;
        this.category = category;
        this.page     = page;
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(null, SIZE,
                MessageManager.parse("<dark_gray>[Admin] <gold>" + category.getDisplayName()));
        render();
        player.openInventory(inventory);
        GuiListener.register(player, this);
    }

    private void render() {
        inventory.clear();
        Arrays.fill(slotIndex, -1);
        fillNavBackground();

        List<ShopItem> items = category.getItems();
        int perPage    = CONTENT;
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / perPage));
        page = Math.min(Math.max(page, 0), totalPages - 1);
        int offset = page * perPage;

        for (int i = 0; i < perPage; i++) {
            int idx = offset + i;
            if (idx >= items.size()) break;
            ShopItem shopItem = items.get(idx);
            ItemStack display = buildAdminDisplay(shopItem, idx);
            inventory.setItem(i, display);
            slotIndex[i] = idx;
        }

        // Nav row
        inventory.setItem(SLOT_BACK,  navItem(Material.ARROW,   MessageManager.parse("<yellow>← Indietro")));
        inventory.setItem(SLOT_CLOSE, navItem(Material.BARRIER,  MessageManager.parse("<red>Chiudi")));

        // Add item button — only show if player holds something
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!held.getType().isAir()) {
            inventory.setItem(SLOT_ADD, buildAddButton(held));
        } else {
            inventory.setItem(SLOT_ADD, buildAddButtonEmpty());
        }

        if (page > 0) {
            inventory.setItem(SLOT_PREV, navItemLore(Material.PAPER,
                    MessageManager.parse("<yellow>◀ Pagina precedente"),
                    List.of(MessageManager.parse("<gray>Pagina " + page + " / " + totalPages))));
        }
        if (page < totalPages - 1) {
            inventory.setItem(SLOT_NEXT, navItemLore(Material.PAPER,
                    MessageManager.parse("<yellow>Pagina successiva ▶"),
                    List.of(MessageManager.parse("<gray>Pagina " + (page + 2) + " / " + totalPages))));
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == SLOT_BACK)  { new AdminCategoryListGui(player, shop).open(); return; }
        if (slot == SLOT_CLOSE) { player.closeInventory(); return; }
        if (slot == SLOT_PREV)  { page--; render(); return; }
        if (slot == SLOT_NEXT)  { page++; render(); return; }

        if (slot == SLOT_ADD) {
            handleAddItem();
            return;
        }

        if (slot >= 0 && slot < CONTENT && slotIndex[slot] >= 0) {
            int idx = slotIndex[slot];
            ShopItem item = category.getItems().get(idx);
            if (event.getClick() == ClickType.SHIFT_RIGHT) {
                // Shift+right-click = quick remove
                handleRemove(idx);
            } else {
                new AdminItemEditGui(player, shop, category, item, idx, page).open();
            }
        }
    }

    // ── actions ──────────────────────────────────────────────────────────────

    private void handleAddItem() {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            player.sendMessage(MessageManager.parse("<red>Tieni un oggetto in mano per aggiungerlo."));
            return;
        }
        player.closeInventory();
        AdminChatInputListener.startAddItem(player, held, shop.getId(), category.getId(), page);
    }

    private void handleRemove(int itemIndex) {
        com.italiarevenge.iRShop.IRShop plugin = com.italiarevenge.iRShop.IRShop.get();
        com.italiarevenge.iRShop.util.CategorySaver.removeItem(plugin, category.getId(), itemIndex);
        plugin.reload();
        // Reopen with refreshed data
        ShopCategory updated = AdminChatInputListener.findCategory(plugin, shop.getId(), category.getId());
        Shop updatedShop = plugin.getShopLoader().getShop(shop.getId());
        if (updated != null && updatedShop != null) {
            new AdminItemListGui(player, updatedShop, updated, page).open();
        } else {
            player.closeInventory();
        }
        player.sendMessage(MessageManager.parse("<green>Oggetto rimosso."));
    }

    // ── item builders ────────────────────────────────────────────────────────

    private ItemStack buildAdminDisplay(ShopItem shopItem, int idx) {
        ItemStack base = ItemBuilder.buildDisplay(shopItem);
        ItemMeta meta = base.getItemMeta();
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MessageManager.parse("<dark_gray>Index: " + idx));
        lore.add(MessageManager.parse("<aqua>Click <gray>per modificare"));
        lore.add(MessageManager.parse("<red>Shift+Right <gray>per rimuovere"));
        meta.lore(lore);
        base.setItemMeta(meta);
        return base;
    }

    private ItemStack buildAddButton(ItemStack held) {
        ItemStack btn = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = btn.getItemMeta();
        meta.displayName(MessageManager.parse("<green>+ Aggiungi oggetto in mano"));
        meta.lore(List.of(
                MessageManager.parse("<gray>Oggetto: <white>" + held.getType().name()),
                Component.empty(),
                MessageManager.parse("<yellow>Click <gray>per aggiungere")));
        btn.setItemMeta(meta);
        return btn;
    }

    private ItemStack buildAddButtonEmpty() {
        ItemStack btn = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = btn.getItemMeta();
        meta.displayName(MessageManager.parse("<gray>+ Aggiungi oggetto"));
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
