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

public class ConfirmGui extends BaseGui {

    private static final int SLOT_PREVIEW = 13;
    private static final int SLOT_CONFIRM = 11;
    private static final int SLOT_CANCEL  = 15;

    private final Shop shop;
    private final ShopCategory category;
    private final ShopItem shopItem;
    private final ItemListGui parent;
    private final MessageManager msg;

    public ConfirmGui(Player player, Shop shop, ShopCategory category,
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
        inventory = Bukkit.createInventory(null, 27, msg.getRaw("confirm-gui.title"));

        // Glass filler
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        gm.displayName(Component.empty());
        glass.setItemMeta(gm);
        for (int i = 0; i < 27; i++) inventory.setItem(i, glass);

        // Item preview
        ItemStack preview = new ItemStack(shopItem.getMaterial());
        ItemMeta pm = preview.getItemMeta();
        pm.displayName(MessageManager.parse("<white>" + prettify(shopItem.getMaterial())));
        preview.setItemMeta(pm);
        inventory.setItem(SLOT_PREVIEW, preview);

        // Confirm button
        String priceFormatted = IRShop.get().getEconomyManager().format(shopItem.getBuyPrice());
        List<Component> confirmLore = msg.getList("confirm-gui.confirm-lore",
                Placeholder.parsed("item-name", prettify(shopItem.getMaterial())),
                Placeholder.parsed("amount",    "1"),
                Placeholder.parsed("price",     priceFormatted));
        inventory.setItem(SLOT_CONFIRM,
                button(Material.GREEN_CONCRETE, msg.getRaw("confirm-gui.confirm-name"), confirmLore));

        // Cancel button
        List<Component> cancelLore = msg.getList("confirm-gui.cancel-lore");
        inventory.setItem(SLOT_CANCEL,
                button(Material.RED_CONCRETE, msg.getRaw("confirm-gui.cancel-name"), cancelLore));

        player.openInventory(inventory);
        GuiListener.register(player, this);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == SLOT_CONFIRM) {
            parent.executeBuy(shopItem, 1);
            parent.open();
        } else if (slot == SLOT_CANCEL) {
            player.sendMessage(msg.get("purchase.cancelled"));
            parent.open();
        }
    }

    private ItemStack button(Material mat, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
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
