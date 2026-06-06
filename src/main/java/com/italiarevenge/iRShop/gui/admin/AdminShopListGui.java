package com.italiarevenge.iRShop.gui.admin;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.gui.BaseGui;
import com.italiarevenge.iRShop.gui.GuiListener;
import com.italiarevenge.iRShop.model.Shop;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AdminShopListGui extends BaseGui {

    private static final int SIZE = 54;
    private static final int SLOT_CLOSE = 49;

    private final List<Shop> shops;
    // slot → shop for click routing
    private final Shop[] slotShops = new Shop[SIZE];

    public AdminShopListGui(Player player) {
        super(player);
        Collection<Shop> all = IRShop.get().getShopLoader().getShops().values();
        this.shops = new ArrayList<>(all);
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(null, SIZE,
                MessageManager.parse("<dark_gray>[Admin] <gold>Shop List"));
        fillBackground();

        for (int i = 0; i < shops.size() && i < 45; i++) {
            Shop shop = shops.get(i);
            inventory.setItem(i, buildShopItem(shop));
            slotShops[i] = shop;
        }

        inventory.setItem(SLOT_CLOSE, navItem(Material.BARRIER,
                MessageManager.parse("<red>Chiudi")));

        player.openInventory(inventory);
        GuiListener.register(player, this);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        if (slot >= 0 && slot < SIZE && slotShops[slot] != null) {
            new AdminCategoryListGui(player, slotShops[slot]).open();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void fillBackground() {
        ItemStack bg = grayPane();
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, bg);
    }

    private ItemStack buildShopItem(Shop shop) {
        ItemStack item = new ItemStack(shop.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MessageManager.parse(shop.getDisplayName()));
        List<Component> lore = new ArrayList<>();
        if (!shop.getDescription().isEmpty()) {
            lore.add(MessageManager.parse("<gray>" + shop.getDescription()));
        }
        lore.add(Component.empty());
        lore.add(MessageManager.parse("<yellow>Categorie: <white>" + shop.getCategories().size()));
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
