package com.italiarevenge.iRShop.service;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.database.Database;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages CRUD operations and in-memory cache for {@link ShopCategory} objects.
 */
public class CategoryService {

    private final IRShop plugin;
    private final Database db;

    /** categoryId → category */
    private final Map<String, ShopCategory> categoryById = new ConcurrentHashMap<>();

    public CategoryService(@NotNull IRShop plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager().getDatabase();
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    /**
     * Loads all categories for {@code shop} into the cache AND populates the shop's
     * in-memory category list.
     */
    @NotNull
    public CompletableFuture<List<ShopCategory>> loadCategories(@NotNull Shop shop) {
        return db.getAllCategories(shop.getId()).thenApply(categories -> {
            shop.clearCategories();
            for (ShopCategory cat : categories) {
                categoryById.put(cat.getId(), cat);
                shop.addCategory(cat);
            }
            return categories;
        });
    }

    public void reload() {
        categoryById.clear();
        for (Shop shop : plugin.getShopService().getAllShops()) {
            loadCategories(shop);
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @NotNull
    public CompletableFuture<ShopCategory> createCategory(
            @NotNull Shop shop,
            @NotNull String name,
            @NotNull String displayName,
            @Nullable String description,
            @Nullable ItemStack icon,
            int slot
    ) {
        String id = UUID.randomUUID().toString();
        ItemStack catIcon = icon != null ? icon : new ItemStack(Material.BOOK);
        int page = calculatePage(shop, slot);
        ShopCategory category = new ShopCategory(
                id, shop.getId(), name, displayName, description, catIcon, slot, page, shop.getCategories().size());

        return db.saveCategory(category).thenApply(v -> {
            categoryById.put(category.getId(), category);
            shop.addCategory(category);
            return category;
        });
    }

    @NotNull
    public CompletableFuture<Void> saveCategory(@NotNull ShopCategory category) {
        return db.saveCategory(category).thenRun(() -> categoryById.put(category.getId(), category));
    }

    @NotNull
    public CompletableFuture<Void> deleteCategory(@NotNull Shop shop, @NotNull String categoryId) {
        return db.deleteCategory(categoryId).thenRun(() -> {
            categoryById.remove(categoryId);
            shop.removeCategory(categoryId);
        });
    }

    // ── Reads ─────────────────────────────────────────────────────────────────

    @Nullable
    public ShopCategory getCategory(@NotNull String id) { return categoryById.get(id); }

    @NotNull
    public List<ShopCategory> getCategories(@NotNull Shop shop) {
        return shop.getCategories();
    }

    private int calculatePage(@NotNull Shop shop, int slot) {
        int layout_slots = plugin.getLayoutManager()
                .getLayoutOrDefault(shop.getLayout()).getItemsPerPage();
        return (slot / layout_slots) + 1;
    }
}
