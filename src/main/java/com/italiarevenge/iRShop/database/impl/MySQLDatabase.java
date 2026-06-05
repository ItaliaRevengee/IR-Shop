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

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * MySQL implementation of {@link Database}.
 * Uses HikariCP with a configurable connection pool size.
 *
 * <p>The SQL dialect differs from SQLite in:
 * <ul>
 *   <li>UPSERT syntax: {@code INSERT ... ON DUPLICATE KEY UPDATE} instead of {@code ON CONFLICT}</li>
 *   <li>{@code MEDIUMTEXT} instead of {@code TEXT} for item_data</li>
 *   <li>Explicit {@code ENGINE=InnoDB} for FK support</li>
 * </ul>
 */
public class MySQLDatabase implements Database {

    private static final String[] SCHEMA = {
        """
        CREATE TABLE IF NOT EXISTS shops (
            id           VARCHAR(36)  NOT NULL PRIMARY KEY,
            name         VARCHAR(64)  NOT NULL UNIQUE,
            display_name TEXT         NOT NULL,
            description  TEXT,
            icon_data    MEDIUMTEXT   NOT NULL,
            currency_id  VARCHAR(64)  NOT NULL DEFAULT 'vault',
            layout       VARCHAR(64)  NOT NULL DEFAULT 'classic',
            permission   VARCHAR(128),
            sort_order   INT          NOT NULL DEFAULT 0
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """,
        """
        CREATE TABLE IF NOT EXISTS categories (
            id           VARCHAR(36)  NOT NULL PRIMARY KEY,
            shop_id      VARCHAR(36)  NOT NULL,
            name         VARCHAR(64)  NOT NULL,
            display_name TEXT         NOT NULL,
            description  TEXT,
            icon_data    MEDIUMTEXT   NOT NULL,
            slot         INT          NOT NULL DEFAULT 0,
            page         INT          NOT NULL DEFAULT 1,
            sort_order   INT          NOT NULL DEFAULT 0,
            FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """,
        """
        CREATE TABLE IF NOT EXISTS shop_items (
            id                VARCHAR(36)  NOT NULL PRIMARY KEY,
            category_id       VARCHAR(36)  NOT NULL,
            item_data         MEDIUMTEXT   NOT NULL,
            buy_price         DOUBLE       NOT NULL DEFAULT -1,
            sell_price        DOUBLE       NOT NULL DEFAULT -1,
            currency_override VARCHAR(64),
            stock             INT          NOT NULL DEFAULT -1,
            max_stock         INT          NOT NULL DEFAULT -1,
            slot              INT          NOT NULL DEFAULT -1,
            sort_order        INT          NOT NULL DEFAULT 0,
            FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """,
        """
        CREATE TABLE IF NOT EXISTS transactions (
            id               VARCHAR(36)  NOT NULL PRIMARY KEY,
            player_uuid      VARCHAR(36)  NOT NULL,
            player_name      VARCHAR(16)  NOT NULL,
            item_id          VARCHAR(36)  NOT NULL,
            transaction_type VARCHAR(4)   NOT NULL,
            amount           INT          NOT NULL,
            price_per_unit   DOUBLE       NOT NULL,
            total_price      DOUBLE       NOT NULL,
            currency         VARCHAR(64)  NOT NULL,
            created_at       BIGINT       NOT NULL,
            INDEX idx_player (player_uuid),
            INDEX idx_item   (item_id),
            INDEX idx_type   (transaction_type)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """,
        """
        CREATE TABLE IF NOT EXISTS player_data (
            player_uuid      VARCHAR(36)  NOT NULL PRIMARY KEY,
            player_name      VARCHAR(16)  NOT NULL,
            total_purchases  BIGINT       NOT NULL DEFAULT 0,
            total_sales      BIGINT       NOT NULL DEFAULT 0,
            total_spent      DOUBLE       NOT NULL DEFAULT 0,
            total_earned     DOUBLE       NOT NULL DEFAULT 0,
            last_seen        BIGINT       NOT NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """
    };

    private final IRShop plugin;
    private final ExecutorService executor;
    private HikariDataSource dataSource;

    public MySQLDatabase(@NotNull IRShop plugin, @NotNull ExecutorService executor) {
        this.plugin = plugin;
        this.executor = executor;
    }

    @Override
    public void initialize() throws Exception {
        var cfg = plugin.getConfig();
        String host     = cfg.getString("database.mysql.host", "localhost");
        int    port     = cfg.getInt("database.mysql.port", 3306);
        String database = cfg.getString("database.mysql.database", "irshop");
        String user     = cfg.getString("database.mysql.username", "root");
        String password = cfg.getString("database.mysql.password", "");

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database +
                      "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4");
        hc.setUsername(user);
        hc.setPassword(password);
        hc.setMaximumPoolSize(cfg.getInt("database.mysql.pool-size", 10));
        hc.setConnectionTimeout(cfg.getLong("database.mysql.connection-timeout", 30000));
        hc.setMaxLifetime(cfg.getLong("database.mysql.max-lifetime", 1800000));
        hc.setKeepaliveTime(cfg.getLong("database.mysql.keepalive-time", 30000));
        hc.addDataSourceProperty("cachePrepStmts", "true");
        hc.addDataSourceProperty("prepStmtCacheSize", "250");
        hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(hc);

        try (Connection conn = dataSource.getConnection()) {
            for (String stmt : SCHEMA) {
                try (PreparedStatement ps = conn.prepareStatement(stmt.trim())) {
                    ps.execute();
                }
            }
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    // ── All CRUD methods delegate to inner helpers ────────────────────────────
    // (identical logic to SQLiteDatabase — MySQL upsert uses ON DUPLICATE KEY)

    @Override
    public @NotNull CompletableFuture<List<Shop>> getAllShops() {
        return CompletableFuture.supplyAsync(() -> {
            List<Shop> list = new ArrayList<>();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM shops ORDER BY sort_order");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapShop(rs));
            } catch (Exception e) { plugin.getLogger().severe("MySQL getAllShops: " + e.getMessage()); }
            return list;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<@Nullable Shop> getShop(@NotNull String id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM shops WHERE id = ?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapShop(rs); }
            } catch (Exception e) { plugin.getLogger().severe("MySQL getShop: " + e.getMessage()); }
            return null;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<@Nullable Shop> getShopByName(@NotNull String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM shops WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapShop(rs); }
            } catch (Exception e) { plugin.getLogger().severe("MySQL getShopByName: " + e.getMessage()); }
            return null;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> saveShop(@NotNull Shop shop) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO shops (id,name,display_name,description,icon_data,currency_id,layout,permission,sort_order) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name),display_name=VALUES(display_name),description=VALUES(description),icon_data=VALUES(icon_data),currency_id=VALUES(currency_id),layout=VALUES(layout),permission=VALUES(permission),sort_order=VALUES(sort_order)";
            try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, shop.getId()); ps.setString(2, shop.getName());
                ps.setString(3, shop.getDisplayName()); ps.setString(4, shop.getDescription());
                ps.setString(5, ItemSerializer.toBase64(shop.getIcon())); ps.setString(6, shop.getCurrencyId());
                ps.setString(7, shop.getLayout()); ps.setString(8, shop.getPermission());
                ps.setInt(9, shop.getSortOrder()); ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("MySQL saveShop: " + e.getMessage()); }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> deleteShop(@NotNull String id) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM shops WHERE id = ?")) {
                ps.setString(1, id); ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("MySQL deleteShop: " + e.getMessage()); }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<List<ShopCategory>> getAllCategories(@NotNull String shopId) {
        return CompletableFuture.supplyAsync(() -> {
            List<ShopCategory> list = new ArrayList<>();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM categories WHERE shop_id = ? ORDER BY sort_order")) {
                ps.setString(1, shopId);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapCategory(rs)); }
            } catch (Exception e) { plugin.getLogger().severe("MySQL getAllCategories: " + e.getMessage()); }
            return list;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<@Nullable ShopCategory> getCategory(@NotNull String id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM categories WHERE id = ?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapCategory(rs); }
            } catch (Exception e) { plugin.getLogger().severe("MySQL getCategory: " + e.getMessage()); }
            return null;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> saveCategory(@NotNull ShopCategory cat) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO categories (id,shop_id,name,display_name,description,icon_data,slot,page,sort_order) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name),display_name=VALUES(display_name),description=VALUES(description),icon_data=VALUES(icon_data),slot=VALUES(slot),page=VALUES(page),sort_order=VALUES(sort_order)";
            try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, cat.getId()); ps.setString(2, cat.getShopId()); ps.setString(3, cat.getName());
                ps.setString(4, cat.getDisplayName()); ps.setString(5, cat.getDescription());
                ps.setString(6, ItemSerializer.toBase64(cat.getIcon())); ps.setInt(7, cat.getSlot());
                ps.setInt(8, cat.getPage()); ps.setInt(9, cat.getSortOrder()); ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("MySQL saveCategory: " + e.getMessage()); }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> deleteCategory(@NotNull String id) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM categories WHERE id = ?")) {
                ps.setString(1, id); ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("MySQL deleteCategory: " + e.getMessage()); }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<List<ShopItem>> getAllItems(@NotNull String categoryId) {
        return CompletableFuture.supplyAsync(() -> {
            List<ShopItem> list = new ArrayList<>();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM shop_items WHERE category_id = ? ORDER BY sort_order")) {
                ps.setString(1, categoryId);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) { ShopItem i = mapItem(rs); if (i != null) list.add(i); } }
            } catch (Exception e) { plugin.getLogger().severe("MySQL getAllItems: " + e.getMessage()); }
            return list;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<@Nullable ShopItem> getItem(@NotNull String id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM shop_items WHERE id = ?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapItem(rs); }
            } catch (Exception e) { plugin.getLogger().severe("MySQL getItem: " + e.getMessage()); }
            return null;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> saveItem(@NotNull ShopItem item) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO shop_items (id,category_id,item_data,buy_price,sell_price,currency_override,stock,max_stock,slot,sort_order) VALUES (?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE item_data=VALUES(item_data),buy_price=VALUES(buy_price),sell_price=VALUES(sell_price),currency_override=VALUES(currency_override),stock=VALUES(stock),max_stock=VALUES(max_stock),slot=VALUES(slot),sort_order=VALUES(sort_order)";
            try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, item.getId()); ps.setString(2, item.getCategoryId());
                ps.setString(3, ItemSerializer.toBase64(item.getItem()));
                ps.setDouble(4, item.getBuyPrice()); ps.setDouble(5, item.getSellPrice());
                ps.setString(6, item.getCurrencyOverride()); ps.setInt(7, item.getStock());
                ps.setInt(8, item.getMaxStock()); ps.setInt(9, item.getSlot()); ps.setInt(10, item.getSortOrder());
                ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("MySQL saveItem: " + e.getMessage()); }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> deleteItem(@NotNull String id) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM shop_items WHERE id = ?")) {
                ps.setString(1, id); ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("MySQL deleteItem: " + e.getMessage()); }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> updateItemStock(@NotNull String id, int stock) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("UPDATE shop_items SET stock = ? WHERE id = ?")) {
                ps.setInt(1, stock); ps.setString(2, id); ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("MySQL updateItemStock: " + e.getMessage()); }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> saveTransaction(@NotNull ShopTransaction tx) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT IGNORE INTO transactions (id,player_uuid,player_name,item_id,transaction_type,amount,price_per_unit,total_price,currency,created_at) VALUES (?,?,?,?,?,?,?,?,?,?)";
            try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, tx.getId()); ps.setString(2, tx.getPlayerUuid().toString());
                ps.setString(3, tx.getPlayerName()); ps.setString(4, tx.getItemId());
                ps.setString(5, tx.getType().name()); ps.setInt(6, tx.getAmount());
                ps.setDouble(7, tx.getPricePerUnit()); ps.setDouble(8, tx.getTotalPrice());
                ps.setString(9, tx.getCurrency()); ps.setLong(10, tx.getCreatedAt().toEpochMilli());
                ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("MySQL saveTransaction: " + e.getMessage()); }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<List<ShopTransaction>> getTransactions(@NotNull UUID playerUuid, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<ShopTransaction> list = new ArrayList<>();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM transactions WHERE player_uuid = ? ORDER BY created_at DESC LIMIT ?")) {
                ps.setString(1, playerUuid.toString()); ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapTransaction(rs)); }
            } catch (Exception e) { plugin.getLogger().severe("MySQL getTransactions: " + e.getMessage()); }
            return list;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<List<ShopTransaction>> getTransactionsByItem(@NotNull String itemId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<ShopTransaction> list = new ArrayList<>();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM transactions WHERE item_id = ? ORDER BY created_at DESC LIMIT ?")) {
                ps.setString(1, itemId); ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapTransaction(rs)); }
            } catch (Exception e) { plugin.getLogger().severe("MySQL getTransactionsByItem: " + e.getMessage()); }
            return list;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<@Nullable PlayerData> getPlayerData(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM player_data WHERE player_uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapPlayerData(rs); }
            } catch (Exception e) { plugin.getLogger().severe("MySQL getPlayerData: " + e.getMessage()); }
            return null;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> savePlayerData(@NotNull PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO player_data (player_uuid,player_name,total_purchases,total_sales,total_spent,total_earned,last_seen) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE player_name=VALUES(player_name),total_purchases=VALUES(total_purchases),total_sales=VALUES(total_sales),total_spent=VALUES(total_spent),total_earned=VALUES(total_earned),last_seen=VALUES(last_seen)";
            try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, data.getPlayerUuid().toString()); ps.setString(2, data.getPlayerName());
                ps.setLong(3, data.getTotalPurchases()); ps.setLong(4, data.getTotalSales());
                ps.setDouble(5, data.getTotalSpent()); ps.setDouble(6, data.getTotalEarned());
                ps.setLong(7, data.getLastSeen().toEpochMilli()); ps.executeUpdate();
            } catch (Exception e) { plugin.getLogger().severe("MySQL savePlayerData: " + e.getMessage()); }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<List<ShopTransaction>> getTopTransactions(
            @NotNull ShopTransaction.Type type, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<ShopTransaction> list = new ArrayList<>();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM transactions WHERE transaction_type = ? ORDER BY total_price DESC LIMIT ?")) {
                ps.setString(1, type.name()); ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapTransaction(rs)); }
            } catch (Exception e) { plugin.getLogger().severe("MySQL getTopTransactions: " + e.getMessage()); }
            return list;
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Double> getTotalVolume(
            @NotNull ShopTransaction.Type type, @NotNull String currency) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT SUM(total_price) FROM transactions WHERE transaction_type = ? AND currency = ?")) {
                ps.setString(1, type.name()); ps.setString(2, currency);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getDouble(1); }
            } catch (Exception e) { plugin.getLogger().severe("MySQL getTotalVolume: " + e.getMessage()); }
            return 0.0;
        }, executor);
    }

    // ── Mappers (identical to SQLite) ─────────────────────────────────────────

    private Shop mapShop(ResultSet rs) throws SQLException {
        String iconData = rs.getString("icon_data");
        ItemStack icon = iconData != null ? ItemSerializer.fromBase64OrFallback(iconData, new ItemStack(Material.CHEST)) : new ItemStack(Material.CHEST);
        return new Shop(rs.getString("id"), rs.getString("name"), rs.getString("display_name"), rs.getString("description"), icon, rs.getString("currency_id"), rs.getString("layout"), rs.getString("permission"), rs.getInt("sort_order"));
    }

    private ShopCategory mapCategory(ResultSet rs) throws SQLException {
        String iconData = rs.getString("icon_data");
        ItemStack icon = iconData != null ? ItemSerializer.fromBase64OrFallback(iconData, new ItemStack(Material.BOOK)) : new ItemStack(Material.BOOK);
        return new ShopCategory(rs.getString("id"), rs.getString("shop_id"), rs.getString("name"), rs.getString("display_name"), rs.getString("description"), icon, rs.getInt("slot"), rs.getInt("page"), rs.getInt("sort_order"));
    }

    @Nullable
    private ShopItem mapItem(ResultSet rs) throws SQLException {
        String itemData = rs.getString("item_data");
        if (itemData == null) return null;
        ItemStack item = ItemSerializer.fromBase64(itemData);
        if (item == null) return null;
        return new ShopItem(rs.getString("id"), rs.getString("category_id"), item, rs.getDouble("buy_price"), rs.getDouble("sell_price"), rs.getString("currency_override"), rs.getInt("stock"), rs.getInt("max_stock"), rs.getInt("slot"), rs.getInt("sort_order"));
    }

    private ShopTransaction mapTransaction(ResultSet rs) throws SQLException {
        return new ShopTransaction(rs.getString("id"), UUID.fromString(rs.getString("player_uuid")), rs.getString("player_name"), rs.getString("item_id"), ShopTransaction.Type.valueOf(rs.getString("transaction_type")), rs.getInt("amount"), rs.getDouble("price_per_unit"), rs.getDouble("total_price"), rs.getString("currency"), Instant.ofEpochMilli(rs.getLong("created_at")));
    }

    private PlayerData mapPlayerData(ResultSet rs) throws SQLException {
        return new PlayerData(UUID.fromString(rs.getString("player_uuid")), rs.getString("player_name"), rs.getLong("total_purchases"), rs.getLong("total_sales"), rs.getDouble("total_spent"), rs.getDouble("total_earned"), Instant.ofEpochMilli(rs.getLong("last_seen")));
    }
}
