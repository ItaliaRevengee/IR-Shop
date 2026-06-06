package com.italiarevenge.iRShop.gui.admin;

import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.gui.BaseGui;
import com.italiarevenge.iRShop.gui.GuiListener;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AdminCategoryListGui extends BaseGui {

    private static final int SIZE = 54;
    private static final int SLOT_BACK  = 45;
    private static final int SLOT_CLOSE = 49;

    private final Shop shop;
    private final ShopCategory[] slotCats = new ShopCategory[SIZE];

    public AdminCategoryListGui(Player player, Shop shop) {
        super(player);
        this.shop = shop;
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(null, SIZE,
                MessageManager.parse("<dark_gray>[Admin] <gold>" + shop.getDisplayName() + " <gray>— Categorie"));
        fillBackground();

        List<ShopCategory> cats = shop.getCategories();
        for (int i = 0; i < cats.size() && i < 45; i++) {
            ShopCategory cat = cats.get(i);
            inventory.setItem(i, buildCatItem(cat));
            slotCats[i] = cat;
        }

        inventory.setItem(SLOT_BACK,  navItem(Material.ARROW,   MessageManager.parse("<yellow>← Indietro")));
        inventory.setItem(SLOT_CLOSE, navItem(Material.BARRIER,  MessageManager.parse("<red>Chiudi")));

        player.openInventory(inventory);
        GuiListener.register(player, this);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == SLOT_BACK)  { new AdminShopListGui(player).open(); return; }
        if (slot == SLOT_CLOSE) { player.closeInventory(); return; }

        if (slot >= 0 && slot < SIZE && slotCats[slot] != null) {
            new AdminItemListGui(player, shop, slotCats[slot], 0).open();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void fillBackground() {
        ItemStack bg = grayPane();
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, bg);
    }

    private ItemStack buildCatItem(ShopCategory cat) {
        ItemStack item = new ItemStack(cat.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MessageManager.parse(cat.getDisplayName()));
        List<Component> lore = new ArrayList<>();
        if (!cat.getDescription().isEmpty()) {
            lore.add(MessageManager.parse("<gray>" + cat.getDescription()));
        }
        lore.add(Component.empty());
        lore.add(MessageManager.parse("<yellow>Oggetti: <white>" + cat.getItems().size()));
        lore.add(Component.empty());
        lore.add(MessageManager.parse("<aqua>Click <gray>per gestire"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack grayPane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack navItem(Material mat, Component name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
