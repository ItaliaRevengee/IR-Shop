package com.italiarevenge.iRShop.service;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.model.PlayerData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages {@link PlayerData} with a write-back cache.
 * Data is loaded on first access and persisted asynchronously on change.
 */
public class PlayerService {

    private final IRShop plugin;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerService(@NotNull IRShop plugin) {
        this.plugin = plugin;
    }

    // ── Access ────────────────────────────────────────────────────────────────

    @NotNull
    public CompletableFuture<PlayerData> getOrCreate(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData cached = cache.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return plugin.getDatabaseManager().getDatabase().getPlayerData(uuid)
                .thenApply(data -> {
                    if (data == null) {
                        data = new PlayerData(uuid, player.getName(), 0, 0, 0, 0, Instant.now());
                    } else {
                        data.setPlayerName(player.getName());
                        data.setLastSeen(Instant.now());
                    }
                    cache.put(uuid, data);
                    return data;
                });
    }

    @Nullable
    public PlayerData getCached(@NotNull UUID uuid) { return cache.get(uuid); }

    // ── Mutation helpers (called from TransactionService on main thread) ───────

    public void recordPurchase(@NotNull Player player, double amount) {
        PlayerData data = cache.computeIfAbsent(player.getUniqueId(),
                k -> new PlayerData(k, player.getName(), 0, 0, 0, 0, Instant.now()));
        data.recordPurchase(amount);
        persist(data);
    }

    public void recordSale(@NotNull Player player, double amount) {
        PlayerData data = cache.computeIfAbsent(player.getUniqueId(),
                k -> new PlayerData(k, player.getName(), 0, 0, 0, 0, Instant.now()));
        data.recordSale(amount);
        persist(data);
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void persist(@NotNull PlayerData data) {
        plugin.getDatabaseManager().getDatabase().savePlayerData(data);
    }

    public void unload(@NotNull UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) persist(data);
    }

    public void flushAll() {
        cache.values().forEach(this::persist);
    }
}
