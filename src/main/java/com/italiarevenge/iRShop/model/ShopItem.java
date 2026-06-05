package com.italiarevenge.iRShop.model;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An item listed inside a {@link ShopCategory}.
 *
 * <p>A price of {@code -1} means the operation (buy or sell) is disabled for this item.
 * Stock of {@code -1} means unlimited stock.
 */
public class ShopItem {

    private final String id;
    private final String categoryId;
    /** The full ItemStack with all metadata preserved. */
    private ItemStack item;
    /** Buy price; {@code -1} disables buying. */
    private double buyPrice;
    /** Sell price; {@code -1} disables selling. */
    private double sellPrice;
    /**
     * Per-item currency override. When set, this takes precedence over the shop's
     * default currency. {@code null} = use shop currency.
     */
    @Nullable
    private String currencyOverride;
    /** Current stock. {@code -1} = unlimited. */
    private int stock;
    /** Max stack the player may hold (inventory limit check). {@code -1} = unlimited. */
    private int maxStock;
    /** Slot in the category's item-list GUI (0-based within layout item-slots). */
    private int slot;
    private int sortOrder;

    public ShopItem(
            @NotNull String id,
            @NotNull String categoryId,
            @NotNull ItemStack item,
            double buyPrice,
            double sellPrice,
            @Nullable String currencyOverride,
            int stock,
            int maxStock,
            int slot,
            int sortOrder
    ) {
        this.id = id;
        this.categoryId = categoryId;
        this.item = item.clone();
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.currencyOverride = currencyOverride;
        this.stock = stock;
        this.maxStock = maxStock;
        this.slot = slot;
        this.sortOrder = sortOrder;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    @NotNull public String getId() { return id; }
    @NotNull public String getCategoryId() { return categoryId; }
    @NotNull public ItemStack getItem() { return item.clone(); }
    public double getBuyPrice() { return buyPrice; }
    public double getSellPrice() { return sellPrice; }
    @Nullable public String getCurrencyOverride() { return currencyOverride; }
    public int getStock() { return stock; }
    public int getMaxStock() { return maxStock; }
    public int getSlot() { return slot; }
    public int getSortOrder() { return sortOrder; }

    public boolean isBuyable() { return buyPrice >= 0; }
    public boolean isSellable() { return sellPrice >= 0; }
    public boolean hasUnlimitedStock() { return stock == -1; }
    public boolean isInStock() { return stock == -1 || stock > 0; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setItem(@NotNull ItemStack item) { this.item = item.clone(); }
    public void setBuyPrice(double buyPrice) { this.buyPrice = buyPrice; }
    public void setSellPrice(double sellPrice) { this.sellPrice = sellPrice; }
    public void setCurrencyOverride(@Nullable String currencyOverride) { this.currencyOverride = currencyOverride; }

    public void setStock(int stock) { this.stock = stock; }

    /** Decrements stock by {@code amount}. No-op when stock is unlimited ({@code -1}). */
    public void decrementStock(int amount) {
        if (stock != -1) stock = Math.max(0, stock - amount);
    }

    /** Increments stock by {@code amount} up to {@code maxStock} (if set). */
    public void incrementStock(int amount) {
        if (stock == -1) return;
        stock += amount;
        if (maxStock > 0) stock = Math.min(stock, maxStock);
    }

    public void setMaxStock(int maxStock) { this.maxStock = maxStock; }
    public void setSlot(int slot) { this.slot = slot; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    @Override
    public String toString() {
        return "ShopItem{id='" + id + "', buy=" + buyPrice + ", sell=" + sellPrice + "}";
    }
}
