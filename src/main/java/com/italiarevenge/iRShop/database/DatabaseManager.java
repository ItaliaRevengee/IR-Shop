package com.italiarevenge.iRShop.database;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.database.impl.MySQLDatabase;
import com.italiarevenge.iRShop.database.impl.SQLiteDatabase;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages the active {@link Database} implementation and the shared
 * {@link ExecutorService} used for all async database operations.
 */
public class DatabaseManager {

    private final IRShop plugin;
    private Database database;

    /**
     * Single-thread executor — all DB operations are sequenced through this
     * thread to avoid connection-pool exhaustion on small servers.
     * Increase thread count only when switching to a dedicated async model.
     */
    private ExecutorService executor;

    public DatabaseManager(@NotNull IRShop plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        // Virtual threads (Project Loom) — available in Java 21
        executor = Executors.newVirtualThreadPerTaskExecutor();

        String type = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();

        database = switch (type) {
            case "mysql" -> new MySQLDatabase(plugin, executor);
            default      -> new SQLiteDatabase(plugin, executor);
        };

        try {
            database.initialize();
            plugin.getLogger().info("Database initialised (" + type.toUpperCase() + ").");
        } catch (Exception e) {
            plugin.getLogger().severe("Database initialisation failed: " + e.getMessage());
            throw new RuntimeException("IR-Shop database init failed", e);
        }
    }

    public void shutdown() {
        if (database != null) database.shutdown();

        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @NotNull
    public Database getDatabase() {
        if (database == null) throw new IllegalStateException("DatabaseManager not initialised.");
        return database;
    }

    @NotNull
    public ExecutorService getExecutor() {
        if (executor == null) throw new IllegalStateException("DatabaseManager not initialised.");
        return executor;
    }
}
