package com.italiarevenge.iRShop.database;

import com.italiarevenge.iRShop.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Database abstraction layer.
 * All methods are non-blocking and return {@link CompletableFuture}s that
 * resolve on a background thread managed by the plugin's executor.
 */
public interface Database {

    /** Initialises the connection pool and creates tables. */
    void initialize() throws Exception;

    /** Gracefully closes all connections. */
    void shutdown();

    // ── Shops ─────────────────────────────────────────────────────────────────

    @NotNull CompletableFuture<List<Shop>>       getAllShops();
    @NotNull CompletableFuture<@Nullable Shop>   getShop(@NotNull String id);
    @NotNull CompletableFuture<@Nullable Shop>   getShopByName(@NotNull String name);
    @NotNull CompletableFuture<Void>             saveShop(@NotNull Shop shop);
    @NotNull CompletableFuture<Void>             deleteShop(@NotNull String id);

    // ── Categories ────────────────────────────────────────────────────────────

    @NotNull CompletableFuture<List<ShopCategory>>         getAllCategories(@NotNull String shopId);
    @NotNull CompletableFuture<@Nullable ShopCategory>     getCategory(@NotNull String id);
    @NotNull CompletableFuture<Void>                       saveCategory(@NotNull ShopCategory category);
    @NotNull CompletableFuture<Void>                       deleteCategory(@NotNull String id);

    // ── Items ─────────────────────────────────────────────────────────────────

    @NotNull CompletableFuture<List<ShopItem>>     getAllItems(@NotNull String categoryId);
    @NotNull CompletableFuture<@Nullable ShopItem> getItem(@NotNull String id);
    @NotNull CompletableFuture<Void>               saveItem(@NotNull ShopItem item);
    @NotNull CompletableFuture<Void>               deleteItem(@NotNull String id);
    @NotNull CompletableFuture<Void>               updateItemStock(@NotNull String id, int stock);

    // ── Transactions ──────────────────────────────────────────────────────────

    @NotNull CompletableFuture<Void>                   saveTransaction(@NotNull ShopTransaction transaction);
    @NotNull CompletableFuture<List<ShopTransaction>>  getTransactions(@NotNull UUID playerUuid, int limit);
    @NotNull CompletableFuture<List<ShopTransaction>>  getTransactionsByItem(@NotNull String itemId, int limit);

    // ── Player data ───────────────────────────────────────────────────────────

    @NotNull CompletableFuture<@Nullable PlayerData> getPlayerData(@NotNull UUID playerUuid);
    @NotNull CompletableFuture<Void>                 savePlayerData(@NotNull PlayerData data);

    // ── Statistics ────────────────────────────────────────────────────────────

    @NotNull CompletableFuture<List<ShopTransaction>> getTopTransactions(
            @NotNull ShopTransaction.Type type, int limit);

    @NotNull CompletableFuture<Double> getTotalVolume(
            @NotNull ShopTransaction.Type type, @NotNull String currency);
}
