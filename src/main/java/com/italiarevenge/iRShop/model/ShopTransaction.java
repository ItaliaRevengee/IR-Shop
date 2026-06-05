package com.italiarevenge.iRShop.model;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable record of a single buy or sell transaction.
 */
public final class ShopTransaction {

    /** Transaction direction. */
    public enum Type { BUY, SELL }

    private final String id;
    private final UUID playerUuid;
    private final String playerName;
    private final String itemId;
    private final Type type;
    private final int amount;
    private final double pricePerUnit;
    private final double totalPrice;
    private final String currency;
    private final Instant createdAt;

    public ShopTransaction(
            @NotNull String id,
            @NotNull UUID playerUuid,
            @NotNull String playerName,
            @NotNull String itemId,
            @NotNull Type type,
            int amount,
            double pricePerUnit,
            double totalPrice,
            @NotNull String currency,
            @NotNull Instant createdAt
    ) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.itemId = itemId;
        this.type = type;
        this.amount = amount;
        this.pricePerUnit = pricePerUnit;
        this.totalPrice = totalPrice;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    @NotNull public String getId() { return id; }
    @NotNull public UUID getPlayerUuid() { return playerUuid; }
    @NotNull public String getPlayerName() { return playerName; }
    @NotNull public String getItemId() { return itemId; }
    @NotNull public Type getType() { return type; }
    public int getAmount() { return amount; }
    public double getPricePerUnit() { return pricePerUnit; }
    public double getTotalPrice() { return totalPrice; }
    @NotNull public String getCurrency() { return currency; }
    @NotNull public Instant getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "ShopTransaction{id='" + id + "', player='" + playerName +
               "', type=" + type + ", amount=" + amount + ", total=" + totalPrice + "}";
    }
}
