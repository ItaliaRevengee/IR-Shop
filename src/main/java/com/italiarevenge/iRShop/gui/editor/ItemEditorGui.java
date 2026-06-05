package com.italiarevenge.iRShop.gui.editor;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.gui.framework.PaginatedGui;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.model.ShopItem;
import com.italiarevenge.iRShop.util.ItemUtil;
import com.italiarevenge.iRShop.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Paginated editor for items within a {@link ShopCategory}.
 * Each slot shows the shop item with buy/sell price in lore.
 * Right-click removes an item; the "Add" button adds the held item.
 */
public class ItemEditorGui extends PaginatedGui {

    private final IRShop plugin;
    private final Shop shop;
    private final ShopCategory category;

    public ItemEditorGui(
            @NotNull IRShop plugin,
            @NotNull Player player,
            @NotNull Shop shop,
            @NotNull ShopCategory category
    ) {
        super(player,
                plugin.getLayoutManager().getLayoutOrDefault(shop.getLayout()),
                TextUtil.parse("<dark_gray>[<gold>Items</gold>] <white>" + category.getDisplayName()));
        this.plugin = plugin;
        this.shop = shop;
        this.category = category;
        plugin.getGuiManager().register(player, this);
    }

    @Override
    public void build() {
        pageItems.clear();

        for (ShopItem si : category.getItems()) {
            pageItems.add(ItemUtil.build(si.getItem().getType(),
                    "<white>" + si.getItem().getType().name().replace('_', ' '),
                    si.isBuyable() ? "<gray>Buy: <green>" + si.getBuyPrice() : "<red>Not for sale",
                    si.isSellable() ? "<gray>Sell: <gold>" + si.getSellPrice() : "<gray>Not sellable",
                    "",
                    "<yellow>Left-click <gray>to set prices",
                    "<red>Right-click <gray>to remove"));
        }

        // Add new item slot
        pageItems.add(ItemUtil.build(Material.EMERALD, "<green>+ Add Item",
                "<gray>Hold an item and click to add it",
                "<gray>Buy price will be set to 10.0",
                "<gray>Sell price will be set to 5.0"));

        renderPage();

        int perPage = layout.getItemsPerPage();
        int start = currentPage * perPage;
        var items = category.getItems();

        for (int i = 0; i < perPage; i++) {
            int itemIndex = start + i;
            int invSlot = layout.getItemSlot(i);
            if (invSlot < 0) continue;

            if (itemIndex == items.size()) {
                // "Add item" button
                setAction(invSlot, e -> addHeldItem());
                continue;
            }
            if (itemIndex >= items.size()) break;

            ShopItem si = items.get(itemIndex);
            setAction(invSlot, e -> {
                if (e.isRightClick()) {
                    plugin.getItemService().deleteItem(category, si.getId())
                            .thenRun(() -> plugin.getServer().getScheduler()
                                    .runTask(plugin, this::refresh));
                    plugin.getMessageManager().send(player, "admin.item-removed");
                } else {
                    // In a full implementation, open a price editor GUI or chat input
                    // For now, toggle buy price between current and -1
                    si.setBuyPrice(si.isBuyable() ? -1 : 10.0);
                    plugin.getItemService().saveItem(si);
                    refresh();
                }
            });
        }

        addNavButton("back",
                ItemUtil.build(layout.getBorderMaterial(), plugin.getMessageManager().get("gui.back-name")),
                () -> new CategoryEditorGui(plugin, player, shop).open());

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

    private void addHeldItem() {
        var held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            plugin.getMessageManager().send(player, "admin.item-added");
            return;
        }
        plugin.getItemService().addItem(category, held, 10.0, 5.0, category.getItems().size())
                .thenAccept(item -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getMessageManager().send(player, "admin.item-added");
                    refresh();
                }));
    }
}
