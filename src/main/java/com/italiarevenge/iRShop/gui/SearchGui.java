package com.italiarevenge.iRShop.gui;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.gui.framework.PaginatedGui;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.model.ShopItem;
import com.italiarevenge.iRShop.util.ItemUtil;
import com.italiarevenge.iRShop.util.TextUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Search results GUI.
 *
 * <p>The search query is provided upfront (set via anvil sign or passed from command).
 * Results are fetched from {@link com.italiarevenge.iRShop.service.ItemService#search(String)}.
 */
public class SearchGui extends PaginatedGui {

    private final IRShop plugin;
    private final Shop shop;
    private final String query;
    private final List<ShopItem> results;
    /** Parallel list — which category each result belongs to (for transaction context). */
    private final List<ShopCategory> resultCategories = new ArrayList<>();

    public SearchGui(
            @NotNull IRShop plugin,
            @NotNull Player player,
            @NotNull Shop shop
    ) {
        this(plugin, player, shop, "");
    }

    public SearchGui(
            @NotNull IRShop plugin,
            @NotNull Player player,
            @NotNull Shop shop,
            @NotNull String query
    ) {
        super(player,
                plugin.getLayoutManager().getLayoutOrDefault(shop.getLayout()),
                TextUtil.parse("<dark_gray>Search: <white>" + (query.isEmpty() ? "…" : query)));
        this.plugin = plugin;
        this.shop = shop;
        this.query = query;
        this.results = query.isEmpty() ? List.of() : plugin.getItemService().search(query);
        plugin.getGuiManager().register(player, this);

        // Map each result to its category
        for (ShopItem item : results) {
            ShopCategory cat = plugin.getCategoryService().getCategory(item.getCategoryId());
            resultCategories.add(cat);
        }
    }

    @Override
    public void build() {
        pageItems.clear();

        if (results.isEmpty() && !query.isEmpty()) {
            // Single "no results" item in the middle
            pageItems.add(ItemUtil.build(
                    org.bukkit.Material.BARRIER,
                    "<red>No results for '<white>" + query + "</white>'",
                    "<gray>Try a different search term"));
        } else {
            for (ShopItem si : results) {
                pageItems.add(si.getItem().clone());
            }
        }

        renderPage();

        // Register click actions for results
        int perPage = layout.getItemsPerPage();
        int start = currentPage * perPage;
        for (int i = 0; i < perPage; i++) {
            int itemIndex = start + i;
            if (itemIndex >= results.size()) break;
            ShopItem si = results.get(itemIndex);
            ShopCategory cat = resultCategories.get(itemIndex);
            int invSlot = layout.getItemSlot(i);
            if (invSlot < 0 || cat == null) continue;
            setAction(invSlot, e -> {
                if (e.isLeftClick() && si.isBuyable()) {
                    if (plugin.getConfigManager().isConfirmPurchases()) {
                        new ConfirmGui(plugin, player, shop, cat, si, 1, this).open();
                    } else {
                        plugin.getTransactionService().buy(player, shop, cat, si, 1);
                        refresh();
                    }
                } else if (e.isRightClick() && si.isSellable()) {
                    plugin.getTransactionService().sell(player, shop, cat, si, 1);
                    refresh();
                }
            });
        }

        addNavButton("back",
                ItemUtil.build(layout.getBorderMaterial(), plugin.getMessageManager().get("gui.back-name")),
                () -> new ShopCategoryGui(plugin, player, shop).open());

        addNavButton("prev-page",
                ItemUtil.build(layout.getBorderMaterial(),
                        plugin.getMessageManager().get("gui.prev-page-name"),
                        Map.of("current", String.valueOf(currentPage + 1), "total", String.valueOf(getTotalPages()))),
                this::prevPage);

        addNavButton("next-page",
                ItemUtil.build(layout.getBorderMaterial(),
                        plugin.getMessageManager().get("gui.next-page-name"),
                        Map.of("current", String.valueOf(currentPage + 1), "total", String.valueOf(getTotalPages()))),
                this::nextPage);

        addNavButton("close",
                ItemUtil.build(layout.getBorderMaterial(), plugin.getMessageManager().get("gui.close-name")),
                () -> player.closeInventory());
    }
}
