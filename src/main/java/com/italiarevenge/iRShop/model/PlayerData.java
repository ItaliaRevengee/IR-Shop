package com.italiarevenge.iRShop.model;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent statistics and metadata for a player.
 */
public class PlayerData {

    private final UUID playerUuid;
    private String playerName;
    private long totalPurchases;
    private long totalSales;
    private double totalSpent;
    private double totalEarned;
    private Instant lastSeen;

    public PlayerData(
            @NotNull UUID playerUuid,
            @NotNull String playerName,
            long totalPurchases,
            long totalSales,
            double totalSpent,
            double totalEarned,
            @NotNull Instant lastSeen
    ) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.totalPurchases = totalPurchases;
        this.totalSales = totalSales;
        this.totalSpent = totalSpent;
        this.totalEarned = totalEarned;
        this.lastSeen = lastSeen;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    @NotNull public UUID getPlayerUuid() { return playerUuid; }
    @NotNull public String getPlayerName() { return playerName; }
    public long getTotalPurchases() { return totalPurchases; }
    public long getTotalSales() { return totalSales; }
    public double getTotalSpent() { return totalSpent; }
    public double getTotalEarned() { return totalEarned; }
    @NotNull public Instant getLastSeen() { return lastSeen; }

    // ── Setters / accumulators ────────────────────────────────────────────────

    public void setPlayerName(@NotNull String playerName) { this.playerName = playerName; }
    public void setLastSeen(@NotNull Instant lastSeen) { this.lastSeen = lastSeen; }

    public void recordPurchase(double amount) {
        totalPurchases++;
        totalSpent += amount;
    }

    public void recordSale(double amount) {
        totalSales++;
        totalEarned += amount;
    }

    @Override
    public String toString() {
        return "PlayerData{uuid=" + playerUuid + ", purchases=" + totalPurchases + ", sales=" + totalSales + "}";
    }
}
