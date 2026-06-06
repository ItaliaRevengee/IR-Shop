package com.italiarevenge.iRShop.gui;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.MessageManager;
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

public class CategoryListGui extends BaseGui {

    private final Shop shop;
    private final Layout layout;
    private final MessageManager msg;

    public CategoryListGui(Player player, Shop shop) {
        super(player);
        this.shop   = shop;
        this.layout = IRShop.get().getLayoutLoader().get(shop.getLayout());
        this.msg    = IRShop.get().getMessageManager();
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(null, layout.rows * 9,
                MessageManager.parse(shop.getDisplayName()));

        fillBackground();

        for (ShopCategory cat : shop.getCategories()) {
            int slot = cat.getSlot();
            if (slot >= 0 && slot < layout.rows * 9) {
                inventory.setItem(slot, buildCategoryItem(cat));
            }
        }

        if (layout.slotClose >= 0) {
            inventory.setItem(layout.slotClose,
                    navItem(Material.BARRIER, msg.getRaw("gui.close-name")));
        }

        player.openInventory(inventory);
        playSound("open");
        GuiListener.register(player, this);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == layout.slotClose) {
            playSound("close");
            player.closeInventory();
            return;
        }

        for (ShopCategory cat : shop.getCategories()) {
            if (cat.getSlot() == slot) {
                playSound("page-turn");
                new ItemListGui(player, shop, cat, 0).open();
                return;
            }
        }
    }

    private void fillBackground() {
        ItemStack bg = new ItemStack(layout.bgMaterial);
        ItemMeta m = bg.getItemMeta();
        m.displayName(Component.empty());
        bg.setItemMeta(m);
        for (int i = 0; i < layout.rows * 9; i++) inventory.setItem(i, bg);
    }

    private ItemStack buildCategoryItem(ShopCategory cat) {
        ItemStack item = new ItemStack(cat.getIcon());
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MessageManager.parse(cat.getDisplayName()));

        List<Component> lore = new ArrayList<>();
        if (!cat.getDescription().isEmpty()) {
            lore.add(MessageManager.parse("<gray>" + cat.getDescription()));
        }
        lore.add(Component.empty());
        lore.add(MessageManager.parse("<yellow>Click <gray>to browse"));
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
}
