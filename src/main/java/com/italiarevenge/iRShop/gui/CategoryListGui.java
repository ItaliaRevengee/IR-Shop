package com.italiarevenge.iRShop.gui;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.economy.EconomyManager;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class CategoryListGui extends BaseGui {

    private final Shop shop;
    private final Layout layout;
    private final MessageManager msg;
    private final EconomyManager economy;

    public CategoryListGui(Player player, Shop shop) {
        super(player);
        this.shop    = shop;
        this.layout  = IRShop.get().getLayoutLoader().get(shop.getLayout());
        this.msg     = IRShop.get().getMessageManager();
        this.economy = IRShop.get().getEconomyManager();
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

        inventory.setItem(playerInfoSlot(), buildPlayerHead());
        inventory.setItem(closeSlot(),
                navItem(Material.BARRIER, msg.getRaw("gui.close-name")));

        player.openInventory(inventory);
        GuiListener.register(player, this);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == closeSlot()) {
            playSound("navigate-back");
            player.closeInventory();
            return;
        }

        for (ShopCategory cat : shop.getCategories()) {
            if (cat.getSlot() == slot) {
                playSound("navigate-back");
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

    private int playerInfoSlot() {
        return (layout.rows - 1) * 9;
    }

    private int closeSlot() {
        return layout.rows * 9 - 1;
    }

    private ItemStack buildPlayerHead() {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(MessageManager.parse("<gold>" + player.getName()));

        List<Component> lore = new ArrayList<>();
        if (economy.isAvailable()) {
            lore.add(MessageManager.parse("<gray>Balance: <green>" + economy.format(economy.getBalance(player))));
        }
        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack navItem(Material mat, Component name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
