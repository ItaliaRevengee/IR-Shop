package com.italiarevenge.iRShop.service;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.model.ShopTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Provides server-wide shop statistics by querying the database.
 */
public class StatisticsService {

    private final IRShop plugin;

    public StatisticsService(@NotNull IRShop plugin) {
        this.plugin = plugin;
    }

    @NotNull
    public CompletableFuture<List<ShopTransaction>> getTopPurchases(int limit) {
        return plugin.getDatabaseManager().getDatabase()
                .getTopTransactions(ShopTransaction.Type.BUY, limit);
    }

    @NotNull
    public CompletableFuture<List<ShopTransaction>> getTopSales(int limit) {
        return plugin.getDatabaseManager().getDatabase()
                .getTopTransactions(ShopTransaction.Type.SELL, limit);
    }

    @NotNull
    public CompletableFuture<Double> getTotalPurchaseVolume(@NotNull String currency) {
        return plugin.getDatabaseManager().getDatabase()
                .getTotalVolume(ShopTransaction.Type.BUY, currency);
    }

    @NotNull
    public CompletableFuture<Double> getTotalSaleVolume(@NotNull String currency) {
        return plugin.getDatabaseManager().getDatabase()
                .getTotalVolume(ShopTransaction.Type.SELL, currency);
    }
}
