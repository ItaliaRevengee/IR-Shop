package com.italiarevenge.iRShop.database.impl;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.database.Database;
import com.italiarevenge.iRShop.model.*;
import com.italiarevenge.iRShop.util.ItemSerializer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * SQLite implementation of {@link Database}.
 * Uses HikariCP with a single-connection pool (SQLite is not concurrent).
 */
public class SQLiteDatabase implements Database {

    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS shops (
                id           TEXT PRIMARY KEY,
                name         TEXT NOT NULL UNIQUE,
                display_name TEXT NOT NULL,
                description  TEXT,
                icon_data    TEXT NOT NULL,
                currency_id  TEXT NOT NULL DEFAULT 'vault',
                layout       TEXT NOT NULL DEFAULT 'classic',
                permission   TEXT,
                sort_order   INTEGER NOT NULL DEFAULT 0
            );
            CREATE TABLE IF NOT EXISTS categories (
                id           TEXT PRIMARY KEY,
                shop_id      TEXT NOT NULL,
                name         TEXT NOT NULL,
                display_name TEXT NOT NULL,
                description  TEXT,
                icon_data    TEXT NOT NULL,
                slot         INTEGER NOT NULL DEFAULT 0,
                page         INTEGER NOT NULL DEFAULT 1,
                sort_order   INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE
            );
            CREATE TABLE IF NOT EXISTS shop_items (
                id               TEXT PRIMARY KEY,
                category_id      TEXT NOT NULL,
                item_data        TEXT NOT NULL,
                buy_price        REAL NOT NULL DEFAULT -1,
                sell_price       REAL NOT NULL DEFAULT -1,
                currency_override TEXT,
                stock            INTEGER NOT NULL DEFAULT -1,
                max_stock        INTEGER NOT NULL DEFAULT -1,
                slot             INTEGER NOT NULL DEFAULT -1,
                sort_order       INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
            );
            CREATE TABLE IF NOT EXISTS transactions (
                id               TEXT PRIMARY KEY,
                player_uuid      TEXT NOT NULL,
                player_name      TEXT NOT NULL,
                item_id          TEXT NOT NULL,
                transaction_type TEXT NOT NULL,
                amount           INTEGER NOT NULL,
                price_per_unit   REAL NOT NULL,
                total_price      REAL NOT NULL,
                currency         TEXT NOT NULL,
                created_at       INTEGER NOT NULL
            );
            CREATE TABLE IF NOT EXISTS player_data (
                player_uuid      TEXT PRIMARY KEY,
                player_name      TEXT NOT NULL,
                total_purchases  INTEGER NOT NULL DEFAULT 0,
                total_sales      INTEGER NOT NULL DEFAULT 0,
                total_spent      REAL NOT NULL DEFAULT 0,
                total_earned     REAL NOT NULL DEFAULT 0,
                last_seen        INTEGER NOT NULL
            );
            PRAGMA foreign_keys = ON;
            """;

    private final IRShop plugin;
    private final ExecutorService executor;
    private HikariDataSource dataSource;

    public SQLiteDatabase(@NotNull IRShop plugin, @NotNull ExecutorService executor) {
        this.plugin = plugin;
        this.executor = executor;
    }

    @Override
    public void initialize() throws Exception {
        File dbFile = new File(plugin.getDataFolder(),
                plugin.getConfig().getString("database.sqlite.file", "irshop.db"));
        plugin.getDataFolder().mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection()) {
            for (String stmt : SCHEMA.split(";")) {
                String trimmed = stmt.trim();
                if (!trimmed.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(trimmed)) {
                        ps.execute();
                    }
                }
            }
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    // ── Shops ─────────────────────────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<List<Shop>> getAllShops() {
        return CompletableFuture.supplyAsync(() -> {
            List<Shop> shops = new ArrayList<>();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT * FROM shops ORDER BY sort_order ASC");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) shops.add(mapShop(rs));
            } catch (Exception e) { plugin.getLogger().severe("getAllShops: " + e.getMessage()); }
            return shops;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<@Nullable Shop> getShop(@NotNull String id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM shops WHERE id = ?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapShop(rs);
                }
            } catch (Exception e) { plugin.getLogger().severe("getShop: " + e.getMessage()); }
            return null;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<@Nullable Shop> getShopByName(@NotNull String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM shops WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapShop(rs);
                }
            } catch (Exception e) { plugin.getLogger().severe("getShopByName: " + e.getMessage()); }
            return null;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> saveShop(@NotNull Shop shop) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO shops (id,name,display_name,description,icon_data,currency_id,layout,permission,sort_order)
                VALUES (?,?,?,?,?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET
                  name=excluded.name, display_name=excluded.display_name,
                  description=excluded.description, icon_data=excluded.icon_data,
                  currency_id=excluded.currency_id, layout=excluded.layout,
                  permission=excluded.permission, sort_order=excluded.sort_order
                """;
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, shop.getId());
                ps.setString(2, shop.getName());
                ps.setString(3, shop.getDisplayName());
                ps.setString(4, shop.getDescription());
                ps.setString(5, ItemSerializer.toBase64(shop.getIcon()));
                ps.setString(6, shop.getCurrencyId());
                ps.setString(7, shop.getLayout());
                ps.setString(8, shop.getPermission());
                ps.setInt(9, shop.getSortOrder());
                ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("saveShop: " + e.getMessage()); }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> deleteShop(@NotNull String id) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM shops WHERE id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("deleteShop: " + e.getMessage()); }
        }, executor);
    }

    // ── Categories ────────────────────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<List<ShopCategory>> getAllCategories(@NotNull String shopId) {
        return CompletableFuture.supplyAsync(() -> {
            List<ShopCategory> categories = new ArrayList<>();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT * FROM categories WHERE shop_id = ? ORDER BY sort_order ASC")) {
                ps.setString(1, shopId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) categories.add(mapCategory(rs));
                }
            } catch (Exception e) { plugin.getLogger().severe("getAllCategories: " + e.getMessage()); }
            return categories;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<@Nullable ShopCategory> getCategory(@NotNull String id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM categories WHERE id = ?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapCategory(rs);
                }
            } catch (Exception e) { plugin.getLogger().severe("getCategory: " + e.getMessage()); }
            return null;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> saveCategory(@NotNull ShopCategory cat) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO categories (id,shop_id,name,display_name,description,icon_data,slot,page,sort_order)
                VALUES (?,?,?,?,?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET
                  name=excluded.name, display_name=excluded.display_name,
                  description=excluded.description, icon_data=excluded.icon_data,
                  slot=excluded.slot, page=excluded.page, sort_order=excluded.sort_order
                """;
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, cat.getId());
                ps.setString(2, cat.getShopId());
                ps.setString(3, cat.getName());
                ps.setString(4, cat.getDisplayName());
                ps.setString(5, cat.getDescription());
                ps.setString(6, ItemSerializer.toBase64(cat.getIcon()));
                ps.setInt(7, cat.getSlot());
                ps.setInt(8, cat.getPage());
                ps.setInt(9, cat.getSortOrder());
                ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("saveCategory: " + e.getMessage()); }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> deleteCategory(@NotNull String id) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM categories WHERE id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("deleteCategory: " + e.getMessage()); }
        }, executor);
    }

    // ── Items ─────────────────────────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<List<ShopItem>> getAllItems(@NotNull String categoryId) {
        return CompletableFuture.supplyAsync(() -> {
            List<ShopItem> items = new ArrayList<>();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT * FROM shop_items WHERE category_id = ? ORDER BY sort_order ASC")) {
                ps.setString(1, categoryId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) { ShopItem i = mapItem(rs); if (i != null) items.add(i); }
                }
            } catch (Exception e) { plugin.getLogger().severe("getAllItems: " + e.getMessage()); }
            return items;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<@Nullable ShopItem> getItem(@NotNull String id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM shop_items WHERE id = ?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapItem(rs);
                }
            } catch (Exception e) { plugin.getLogger().severe("getItem: " + e.getMessage()); }
            return null;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> saveItem(@NotNull ShopItem item) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO shop_items (id,category_id,item_data,buy_price,sell_price,currency_override,stock,max_stock,slot,sort_order)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET
                  item_data=excluded.item_data, buy_price=excluded.buy_price,
                  sell_price=excluded.sell_price, currency_override=excluded.currency_override,
                  stock=excluded.stock, max_stock=excluded.max_stock,
                  slot=excluded.slot, sort_order=excluded.sort_order
                """;
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, item.getId());
                ps.setString(2, item.getCategoryId());
                ps.setString(3, ItemSerializer.toBase64(item.getItem()));
                ps.setDouble(4, item.getBuyPrice());
                ps.setDouble(5, item.getSellPrice());
                ps.setString(6, item.getCurrencyOverride());
                ps.setInt(7, item.getStock());
                ps.setInt(8, item.getMaxStock());
                ps.setInt(9, item.getSlot());
                ps.setInt(10, item.getSortOrder());
                ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("saveItem: " + e.getMessage()); }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> deleteItem(@NotNull String id) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM shop_items WHERE id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("deleteItem: " + e.getMessage()); }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> updateItemStock(@NotNull String id, int stock) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "UPDATE shop_items SET stock = ? WHERE id = ?")) {
                ps.setInt(1, stock);
                ps.setString(2, id);
                ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("updateItemStock: " + e.getMessage()); }
        }, executor);
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<Void> saveTransaction(@NotNull ShopTransaction tx) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO transactions (id,player_uuid,player_name,item_id,transaction_type,amount,price_per_unit,total_price,currency,created_at) VALUES (?,?,?,?,?,?,?,?,?,?)";
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, tx.getId());
                ps.setString(2, tx.getPlayerUuid().toString());
                ps.setString(3, tx.getPlayerName());
                ps.setString(4, tx.getItemId());
                ps.setString(5, tx.getType().name());
                ps.setInt(6, tx.getAmount());
                ps.setDouble(7, tx.getPricePerUnit());
                ps.setDouble(8, tx.getTotalPrice());
                ps.setString(9, tx.getCurrency());
                ps.setLong(10, tx.getCreatedAt().toEpochMilli());
                ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("saveTransaction: " + e.getMessage()); }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<List<ShopTransaction>> getTransactions(@NotNull UUID playerUuid, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<ShopTransaction> list = new ArrayList<>();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT * FROM transactions WHERE player_uuid = ? ORDER BY created_at DESC LIMIT ?")) {
                ps.setString(1, playerUuid.toString());
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapTransaction(rs));
                }
            } catch (Exception e) { plugin.getLogger().severe("getTransactions: " + e.getMessage()); }
            return list;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<List<ShopTransaction>> getTransactionsByItem(@NotNull String itemId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<ShopTransaction> list = new ArrayList<>();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT * FROM transactions WHERE item_id = ? ORDER BY created_at DESC LIMIT ?")) {
                ps.setString(1, itemId);
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapTransaction(rs));
                }
            } catch (Exception e) { plugin.getLogger().severe("getTransactionsByItem: " + e.getMessage()); }
            return list;
        }, executor);
    }

    // ── Player data ───────────────────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<@Nullable PlayerData> getPlayerData(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT * FROM player_data WHERE player_uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapPlayerData(rs);
                }
            } catch (Exception e) { plugin.getLogger().severe("getPlayerData: " + e.getMessage()); }
            return null;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> savePlayerData(@NotNull PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO player_data (player_uuid,player_name,total_purchases,total_sales,total_spent,total_earned,last_seen)
                VALUES (?,?,?,?,?,?,?)
                ON CONFLICT(player_uuid) DO UPDATE SET
                  player_name=excluded.player_name,
                  total_purchases=excluded.total_purchases, total_sales=excluded.total_sales,
                  total_spent=excluded.total_spent, total_earned=excluded.total_earned,
                  last_seen=excluded.last_seen
                """;
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, data.getPlayerUuid().toString());
                ps.setString(2, data.getPlayerName());
                ps.setLong(3, data.getTotalPurchases());
                ps.setLong(4, data.getTotalSales());
                ps.setDouble(5, data.getTotalSpent());
                ps.setDouble(6, data.getTotalEarned());
                ps.setLong(7, data.getLastSeen().toEpochMilli());
                ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("savePlayerData: " + e.getMessage()); }
        }, executor);
    }

    // ── Statistics ────────────────────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<List<ShopTransaction>> getTopTransactions(
            @NotNull ShopTransaction.Type type, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<ShopTransaction> list = new ArrayList<>();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT * FROM transactions WHERE transaction_type = ? ORDER BY total_price DESC LIMIT ?")) {
                ps.setString(1, type.name());
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapTransaction(rs));
                }
            } catch (Exception e) { plugin.getLogger().severe("getTopTransactions: " + e.getMessage()); }
            return list;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Double> getTotalVolume(
            @NotNull ShopTransaction.Type type, @NotNull String currency) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT SUM(total_price) FROM transactions WHERE transaction_type = ? AND currency = ?")) {
                ps.setString(1, type.name());
                ps.setString(2, currency);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getDouble(1);
                }
            } catch (Exception e) { plugin.getLogger().severe("getTotalVolume: " + e.getMessage()); }
            return 0.0;
        }, executor);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private Shop mapShop(ResultSet rs) throws SQLException {
        String iconData = rs.getString("icon_data");
        ItemStack icon = iconData != null
                ? ItemSerializer.fromBase64OrFallback(iconData, new ItemStack(Material.CHEST))
                : new ItemStack(Material.CHEST);
        return new Shop(
                rs.getString("id"), rs.getString("name"),
                rs.getString("display_name"), rs.getString("description"),
                icon, rs.getString("currency_id"), rs.getString("layout"),
                rs.getString("permission"), rs.getInt("sort_order"));
    }

    private ShopCategory mapCategory(ResultSet rs) throws SQLException {
        String iconData = rs.getString("icon_data");
        ItemStack icon = iconData != null
                ? ItemSerializer.fromBase64OrFallback(iconData, new ItemStack(Material.BOOK))
                : new ItemStack(Material.BOOK);
        return new ShopCategory(
                rs.getString("id"), rs.getString("shop_id"),
                rs.getString("name"), rs.getString("display_name"),
                rs.getString("description"), icon,
                rs.getInt("slot"), rs.getInt("page"), rs.getInt("sort_order"));
    }

    @Nullable
    private ShopItem mapItem(ResultSet rs) throws SQLException {
        String itemData = rs.getString("item_data");
        if (itemData == null) return null;
        ItemStack item = ItemSerializer.fromBase64(itemData);
        if (item == null) return null;
        return new ShopItem(
                rs.getString("id"), rs.getString("category_id"),
                item, rs.getDouble("buy_price"), rs.getDouble("sell_price"),
                rs.getString("currency_override"),
                rs.getInt("stock"), rs.getInt("max_stock"),
                rs.getInt("slot"), rs.getInt("sort_order"));
    }

    private ShopTransaction mapTransaction(ResultSet rs) throws SQLException {
        return new ShopTransaction(
                rs.getString("id"),
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("player_name"),
                rs.getString("item_id"),
                ShopTransaction.Type.valueOf(rs.getString("transaction_type")),
                rs.getInt("amount"),
                rs.getDouble("price_per_unit"),
                rs.getDouble("total_price"),
                rs.getString("currency"),
                Instant.ofEpochMilli(rs.getLong("created_at")));
    }

    private PlayerData mapPlayerData(ResultSet rs) throws SQLException {
        return new PlayerData(
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("player_name"),
                rs.getLong("total_purchases"),
                rs.getLong("total_sales"),
                rs.getDouble("total_spent"),
                rs.getDouble("total_earned"),
                Instant.ofEpochMilli(rs.getLong("last_seen")));
    }
}
