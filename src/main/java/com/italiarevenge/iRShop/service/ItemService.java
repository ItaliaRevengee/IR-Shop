package com.italiarevenge.iRShop.service;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.database.Database;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.model.ShopItem;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Manages CRUD for {@link ShopItem}s with in-memory caching.
 */
public class ItemService {

    private final IRShop plugin;
    private final Database db;

    /** itemId → item */
    private final Map<String, ShopItem> itemById = new ConcurrentHashMap<>();

    public ItemService(@NotNull IRShop plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager().getDatabase();
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    @NotNull
    public CompletableFuture<List<ShopItem>> loadItems(@NotNull ShopCategory category) {
        return db.getAllItems(category.getId()).thenApply(items -> {
            category.clearItems();
            for (ShopItem item : items) {
                itemById.put(item.getId(), item);
                category.addItem(item);
            }
            return items;
        });
    }

    public void reload() {
        itemById.clear();
        for (Shop shop : plugin.getShopService().getAllShops()) {
            for (ShopCategory cat : shop.getCategories()) {
                loadItems(cat);
            }
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @NotNull
    public CompletableFuture<ShopItem> addItem(
            @NotNull ShopCategory category,
            @NotNull ItemStack item,
            double buyPrice,
            double sellPrice,
            int slot
    ) {
        String id = UUID.randomUUID().toString();
        ShopItem shopItem = new ShopItem(
                id, category.getId(), item, buyPrice, sellPrice,
                null, -1, -1, slot, category.getItems().size());

        return db.saveItem(shopItem).thenApply(v -> {
            itemById.put(shopItem.getId(), shopItem);
            category.addItem(shopItem);
            return shopItem;
        });
    }

    @NotNull
    public CompletableFuture<Void> saveItem(@NotNull ShopItem item) {
        return db.saveItem(item).thenRun(() -> itemById.put(item.getId(), item));
    }

    @NotNull
    public CompletableFuture<Void> deleteItem(@NotNull ShopCategory category, @NotNull String itemId) {
        return db.deleteItem(itemId).thenRun(() -> {
            itemById.remove(itemId);
            category.removeItem(itemId);
        });
    }

    @NotNull
    public CompletableFuture<Void> updateStock(@NotNull ShopItem item, int newStock) {
        item.setStock(newStock);
        return db.updateItemStock(item.getId(), newStock);
    }

    // ── Reads ─────────────────────────────────────────────────────────────────

    @Nullable
    public ShopItem getItem(@NotNull String id) { return itemById.get(id); }

    @NotNull
    public List<ShopItem> getItems(@NotNull ShopCategory category) {
        return category.getItems();
    }

    /**
     * Searches all loaded items for those whose display name or material name
     * contains {@code query} (case-insensitive).
     */
    @NotNull
    public List<ShopItem> search(@NotNull String query) {
        String lowerQuery = query.toLowerCase();
        List<ShopItem> results = new ArrayList<>();
        for (ShopItem item : itemById.values()) {
            String matName = item.getItem().getType().name().toLowerCase().replace('_', ' ');
            if (matName.contains(lowerQuery)) {
                results.add(item);
            }
        }
        return results;
    }
}
