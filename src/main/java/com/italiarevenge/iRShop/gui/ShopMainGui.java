package com.italiarevenge.iRShop.gui;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.api.events.ShopOpenEvent;
import com.italiarevenge.iRShop.gui.framework.PaginatedGui;
import com.italiarevenge.iRShop.layout.Layout;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.util.ItemUtil;
import com.italiarevenge.iRShop.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * GUI listing all available shops. Each shop is represented by its icon.
 * Clicking a shop icon opens {@link ShopCategoryGui}.
 */
public class ShopMainGui extends PaginatedGui {

    private final IRShop plugin;
    private final List<Shop> shops;

    public ShopMainGui(@NotNull IRShop plugin, @NotNull Player player) {
        super(player,
                plugin.getLayoutManager().getLayoutOrDefault(
                        plugin.getConfigManager().getDefaultLayout()),
                TextUtil.parse("<dark_gray>◈ <gradient:#FF6B35:#FFD166>IR-Shop</gradient>"));
        this.plugin = plugin;
        this.shops = plugin.getShopService().getAllShops().stream()
                .filter(s -> s.getPermission() == null || player.hasPermission(s.getPermission()))
                .toList();
        plugin.getGuiManager().register(player, this);
    }

    @Override
    public void build() {
        pageItems.clear();

        for (Shop shop : shops) {
            ItemStack icon = ItemUtil.build(
                    shop.getIcon().getType(),
                    shop.getDisplayName(),
                    shop.getDescription() != null ? List.of(shop.getDescription()) : null
            );
            pageItems.add(icon);
        }

        renderPage();

        // Register click actions for each visible shop
        int perPage = layout.getItemsPerPage();
        int start = currentPage * perPage;
        for (int i = 0; i < perPage; i++) {
            int itemIndex = start + i;
            if (itemIndex >= shops.size()) break;
            Shop shop = shops.get(itemIndex);
            int invSlot = layout.getItemSlot(i);
            if (invSlot < 0) continue;
            setAction(invSlot, e -> openShop(shop));
        }

        // Nav buttons
        addNavButton("prev-page",
                ItemUtil.build(layout.getBorderMaterial(),
                        plugin.getMessageManager().get("gui.prev-page-name"),
                        Map.of("current", String.valueOf(currentPage + 1),
                               "total",   String.valueOf(getTotalPages()))),
                this::prevPage);

        addNavButton("next-page",
                ItemUtil.build(layout.getBorderMaterial(),
                        plugin.getMessageManager().get("gui.next-page-name"),
                        Map.of("current", String.valueOf(currentPage + 1),
                               "total",   String.valueOf(getTotalPages()))),
                this::nextPage);

        addNavButton("close",
                ItemUtil.build(layout.getBorderMaterial(),
                        plugin.getMessageManager().get("gui.close-name")),
                () -> player.closeInventory());
    }

    private void openShop(@NotNull Shop shop) {
        ShopOpenEvent event = new ShopOpenEvent(player, shop);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        // Load categories first, then open the category GUI
        plugin.getCategoryService().loadCategories(shop)
                .thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    // Preload items for each category
                    for (var cat : shop.getCategories()) {
                        plugin.getItemService().loadItems(cat);
                    }
                    plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                            new ShopCategoryGui(plugin, player, shop).open(), 2L);
                }));
    }
}
