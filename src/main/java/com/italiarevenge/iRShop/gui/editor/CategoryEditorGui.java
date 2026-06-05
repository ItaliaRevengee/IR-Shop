package com.italiarevenge.iRShop.gui.editor;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.gui.framework.PaginatedGui;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.util.ItemUtil;
import com.italiarevenge.iRShop.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Paginated editor listing all categories in a shop.
 * Left-click opens the category's item editor.
 * Right-click deletes the category.
 * A dedicated "Add" button creates a new category.
 */
public class CategoryEditorGui extends PaginatedGui {

    private final IRShop plugin;
    private final Shop shop;

    public CategoryEditorGui(@NotNull IRShop plugin, @NotNull Player player, @NotNull Shop shop) {
        super(player,
                plugin.getLayoutManager().getLayoutOrDefault(shop.getLayout()),
                TextUtil.parse("<dark_gray>[<gold>Categories</gold>] <white>" + shop.getName()));
        this.plugin = plugin;
        this.shop = shop;
        plugin.getGuiManager().register(player, this);
    }

    @Override
    public void build() {
        pageItems.clear();

        for (ShopCategory cat : shop.getCategories()) {
            pageItems.add(ItemUtil.build(cat.getIcon().getType(),
                    cat.getDisplayName(),
                    "<yellow>Left-click <gray>to edit items",
                    "<red>Right-click <gray>to delete category",
                    "<gray>Slot: <white>" + cat.getSlot()));
        }

        // "Add new" item
        pageItems.add(ItemUtil.build(Material.EMERALD, "<green>+ Add New Category",
                "<gray>Click to create a new category"));

        renderPage();

        // Register category actions
        int perPage = layout.getItemsPerPage();
        int start = currentPage * perPage;
        var categories = shop.getCategories();
        for (int i = 0; i < perPage; i++) {
            int itemIndex = start + i;
            int invSlot = layout.getItemSlot(i);
            if (invSlot < 0) continue;
            // Last entry is "Add new"
            if (itemIndex == categories.size()) {
                setAction(invSlot, e -> createCategory());
                continue;
            }
            if (itemIndex >= categories.size()) break;
            ShopCategory cat = categories.get(itemIndex);
            setAction(invSlot, e -> {
                if (e.isRightClick()) {
                    plugin.getCategoryService().deleteCategory(shop, cat.getId())
                            .thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, this::refresh));
                    plugin.getMessageManager().send(player, "admin.category-deleted");
                } else {
                    new ItemEditorGui(plugin, player, shop, cat).open();
                }
            });
        }

        addNavButton("back",
                ItemUtil.build(layout.getBorderMaterial(),
                        plugin.getMessageManager().get("gui.back-name")),
                () -> new ShopEditorGui(plugin, player, shop).open());

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

    private void createCategory() {
        String name = "category_" + shop.getCategories().size();
        plugin.getCategoryService().createCategory(shop, name, "<white>New Category", null, null,
                shop.getCategories().size())
                .thenAccept(cat -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getMessageManager().send(player, "admin.category-created", Map.of("name", name));
                    refresh();
                }));
    }
}
