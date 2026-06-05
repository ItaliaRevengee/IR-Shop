package com.italiarevenge.iRShop.service;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.database.Database;
import com.italiarevenge.iRShop.model.Shop;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Business-logic layer for shops.
 * Maintains an in-memory cache so GUI lookups are O(1) without hitting the DB.
 */
public class ShopService {

    private final IRShop plugin;
    private final Database db;

    /** id → shop */
    private final Map<String, Shop> shopById = new ConcurrentHashMap<>();
    /** lowercase name → id */
    private final Map<String, String> nameIndex = new ConcurrentHashMap<>();

    public ShopService(@NotNull IRShop plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager().getDatabase();
        loadAll();
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    private void loadAll() {
        db.getAllShops().thenAccept(shops -> {
            shopById.clear();
            nameIndex.clear();
            for (Shop shop : shops) cache(shop);
            plugin.getLogger().info("Loaded " + shops.size() + " shop(s) into cache.");
        }).exceptionally(e -> {
            plugin.getLogger().severe("Failed to load shops: " + e.getMessage());
            return null;
        });
    }

    public void reload() { loadAll(); }

    private void cache(@NotNull Shop shop) {
        shopById.put(shop.getId(), shop);
        nameIndex.put(shop.getName().toLowerCase(), shop.getId());
    }

    private void evict(@NotNull String id) {
        Shop removed = shopById.remove(id);
        if (removed != null) nameIndex.remove(removed.getName().toLowerCase());
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @NotNull
    public CompletableFuture<Shop> createShop(
            @NotNull String name,
            @NotNull String displayName,
            @Nullable String description,
            @Nullable ItemStack icon
    ) {
        String id = UUID.randomUUID().toString();
        ItemStack shopIcon = icon != null ? icon : new ItemStack(Material.CHEST);
        String layout = plugin.getConfigManager().getDefaultLayout();
        String currency = plugin.getConfigManager().getDefaultEconomyProvider();

        Shop shop = new Shop(id, name, displayName, description, shopIcon, currency, layout, null, shopById.size());

        return db.saveShop(shop).thenApply(v -> {
            cache(shop);
            return shop;
        });
    }

    @NotNull
    public CompletableFuture<Void> saveShop(@NotNull Shop shop) {
        return db.saveShop(shop).thenRun(() -> cache(shop));
    }

    @NotNull
    public CompletableFuture<Void> deleteShop(@NotNull String id) {
        return db.deleteShop(id).thenRun(() -> evict(id));
    }

    // ── Reads (all from cache) ────────────────────────────────────────────────

    @Nullable
    public Shop getShop(@NotNull String id) { return shopById.get(id); }

    @Nullable
    public Shop getShopByName(@NotNull String name) {
        String id = nameIndex.get(name.toLowerCase());
        return id != null ? shopById.get(id) : null;
    }

    @NotNull
    public List<Shop> getAllShops() {
        List<Shop> list = new ArrayList<>(shopById.values());
        list.sort(Comparator.comparingInt(Shop::getSortOrder));
        return list;
    }

    public boolean shopExists(@NotNull String name) {
        return nameIndex.containsKey(name.toLowerCase());
    }
}
