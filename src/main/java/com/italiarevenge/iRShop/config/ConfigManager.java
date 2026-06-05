package com.italiarevenge.iRShop.config;

import com.italiarevenge.iRShop.IRShop;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Thin wrapper around {@link FileConfiguration} that centralises config access
 * and avoids scattered {@code plugin.getConfig()} calls throughout the codebase.
 */
public class ConfigManager {

    private final IRShop plugin;
    private FileConfiguration config;

    public ConfigManager(@NotNull IRShop plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // ── Database ──────────────────────────────────────────────────────────────

    @NotNull
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite").toLowerCase();
    }

    // ── Economy ───────────────────────────────────────────────────────────────

    @NotNull
    public String getDefaultEconomyProvider() {
        return config.getString("economy.default-provider", "vault").toLowerCase();
    }

    @NotNull
    public String getDefaultCurrenciesCurrency() {
        return config.getString("economy.currencies-plugin.default-currency", "money");
    }

    // ── Shop ──────────────────────────────────────────────────────────────────

    public boolean isConfirmPurchases() { return config.getBoolean("shop.confirm-purchases", true); }
    public boolean isQuickBuyEnabled() { return config.getBoolean("shop.quick-buy", true); }
    public boolean isSearchEnabled() { return config.getBoolean("shop.search-enabled", true); }
    public int getSearchResultsPerPage() { return config.getInt("shop.search-results-per-page", 28); }

    @NotNull
    public String getDefaultLayout() {
        return config.getString("shop.default-layout", "classic");
    }

    // ── Sounds ────────────────────────────────────────────────────────────────

    public boolean areSoundsEnabled() { return config.getBoolean("shop.sounds.enabled", true); }

    @NotNull
    public String getSoundOpen() { return config.getString("shop.sounds.open", "BLOCK_CHEST_OPEN"); }
    @NotNull
    public String getSoundClose() { return config.getString("shop.sounds.close", "BLOCK_CHEST_CLOSE"); }
    @NotNull
    public String getSoundPurchase() { return config.getString("shop.sounds.purchase", "ENTITY_EXPERIENCE_ORB_PICKUP"); }
    @NotNull
    public String getSoundSell() { return config.getString("shop.sounds.sell", "ENTITY_VILLAGER_TRADE"); }
    @NotNull
    public String getSoundError() { return config.getString("shop.sounds.error", "ENTITY_VILLAGER_NO"); }
    @NotNull
    public String getSoundPageTurn() { return config.getString("shop.sounds.page-turn", "ITEM_BOOK_PAGE_TURN"); }

    // ── Discounts / multipliers ───────────────────────────────────────────────

    public boolean areDiscountsEnabled() { return config.getBoolean("discounts.enabled", true); }
    public int getMaxDiscount() { return config.getInt("discounts.max-discount", 90); }
    public boolean areSellMultipliersEnabled() { return config.getBoolean("sell-multipliers.enabled", true); }

    // ── Statistics ────────────────────────────────────────────────────────────

    public boolean areStatisticsEnabled() { return config.getBoolean("statistics.enabled", true); }

    // ── Bedrock ───────────────────────────────────────────────────────────────

    public boolean isBedrockSupportEnabled() { return config.getBoolean("bedrock.enabled", true); }

    @NotNull
    public String getFloodgatePrefix() {
        return config.getString("bedrock.floodgate-prefix", ".");
    }

    // ── Debug ─────────────────────────────────────────────────────────────────

    public boolean isDebug() { return config.getBoolean("debug", false); }

    // ── Generic access (for parts that need raw config) ───────────────────────

    @NotNull
    public FileConfiguration getRaw() { return config; }

    @Nullable
    public String getString(@NotNull String path, @Nullable String def) {
        return config.getString(path, def);
    }

    public boolean getBoolean(@NotNull String path, boolean def) {
        return config.getBoolean(path, def);
    }

    public int getInt(@NotNull String path, int def) {
        return config.getInt(path, def);
    }

    @NotNull
    public List<String> getStringList(@NotNull String path) {
        return config.getStringList(path);
    }
}
