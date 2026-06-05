package com.italiarevenge.iRShop.gui.editor;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.gui.framework.BaseGui;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.util.ItemUtil;
import com.italiarevenge.iRShop.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * In-game editor for an individual {@link Shop}.
 * Allows admins to rename, change icon, change currency, change layout,
 * manage categories and delete the shop.
 */
public class ShopEditorGui extends BaseGui {

    private final IRShop plugin;
    private final Shop shop;

    public ShopEditorGui(@NotNull IRShop plugin, @NotNull Player player, @NotNull Shop shop) {
        super(player, 4, TextUtil.parse("<dark_gray>[<gold>Editor</gold>] <white>" + shop.getName()));
        this.plugin = plugin;
        this.shop = shop;
        plugin.getGuiManager().register(player, this);
    }

    @Override
    public void build() {
        // Background
        var bg = ItemUtil.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 36; i++) set(i, bg);

        // Shop icon (preview)
        set(4, ItemUtil.build(
                shop.getIcon().getType(),
                "<yellow>Shop Icon",
                "<gray>Right-click to set icon to your held item"));

        setAction(4, e -> {
            if (e.isRightClick()) {
                var hand = player.getInventory().getItemInMainHand();
                if (hand != null && !hand.getType().isAir()) {
                    shop.setIcon(hand);
                    plugin.getShopService().saveShop(shop);
                    refresh();
                    plugin.getMessageManager().send(player, "admin.shop-edited");
                }
            }
        });

        // Display name
        set(11, ItemUtil.build(Material.NAME_TAG, "<yellow>Display Name",
                "<gray>Current: <white>" + shop.getDisplayName(),
                "<gray>Chat input to change"));
        setAction(11, e -> {
            player.closeInventory();
            plugin.getMessageManager().send(player, "admin.shop-edited");
        });

        // Currency
        set(13, ItemUtil.build(Material.GOLD_INGOT, "<yellow>Currency",
                "<gray>Current: <white>" + shop.getCurrencyId(),
                "<gray>Click to cycle currencies"));
        setAction(13, e -> cycleCurrency(e));

        // Layout
        set(15, ItemUtil.build(Material.PAINTING, "<yellow>Layout",
                "<gray>Current: <white>" + shop.getLayout(),
                "<gray>Click to cycle layouts"));
        setAction(15, e -> cycleLayout());

        // Manage Categories
        set(20, ItemUtil.build(Material.CHEST, "<aqua>Manage Categories",
                "<gray>Click to open category editor"));
        setAction(20, e -> new CategoryEditorGui(plugin, player, shop).open());

        // Delete shop
        set(24, ItemUtil.build(Material.BARRIER, "<red>Delete Shop",
                "<gray>Right-click to delete",
                "<red>This cannot be undone!"));
        setAction(24, e -> {
            if (e.isRightClick()) {
                String name = shop.getName();
                plugin.getShopService().deleteShop(shop.getId());
                player.closeInventory();
                plugin.getMessageManager().send(player, "admin.shop-deleted",
                        Map.of("name", name));
            }
        });

        // Back button
        set(31, ItemUtil.build(Material.ARROW, plugin.getMessageManager().get("gui.close-name")));
        setAction(31, e -> player.closeInventory());
    }

    private void cycleCurrency(@NotNull InventoryClickEvent e) {
        var providers = plugin.getEconomyManager().getAllProviders();
        var ids = providers.stream().map(p -> p.getId()).toList();
        int idx = ids.indexOf(shop.getCurrencyId());
        String next = ids.get((idx + 1) % ids.size());
        shop.setCurrencyId(next);
        plugin.getShopService().saveShop(shop);
        refresh();
    }

    private void cycleLayout() {
        var layouts = plugin.getLayoutManager().getAllLayouts().stream()
                .map(l -> l.getId()).toList();
        int idx = layouts.indexOf(shop.getLayout());
        String next = layouts.get((idx + 1) % layouts.size());
        shop.setLayout(next);
        plugin.getShopService().saveShop(shop);
        refresh();
    }
}
