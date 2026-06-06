package com.italiarevenge.iRShop.config;

import com.italiarevenge.iRShop.IRShop;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final IRShop plugin;
    private FileConfiguration config;

    public ConfigManager(IRShop plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        mergeDefaults();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        mergeDefaults();
    }

    /** Adds any missing keys from the bundled config.yml without touching existing values. */
    private void mergeDefaults() {
        try (java.io.InputStream in = plugin.getResource("config.yml")) {
            if (in == null) return;
            org.bukkit.configuration.file.YamlConfiguration defaults =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                            new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            config.setDefaults(defaults);
            config.options().copyDefaults(true);
            plugin.saveConfig();
            config = plugin.getConfig();
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Failed to merge config.yml defaults: " + e.getMessage());
        }
    }

    public boolean isConfirmPurchases() {
        return config.getBoolean("shop.confirm-purchases", true);
    }

    public boolean isQuickBuy() {
        return config.getBoolean("shop.quick-buy", true);
    }

    public boolean isSoundsEnabled() {
        return config.getBoolean("shop.sounds.enabled", true);
    }

    public boolean isDiscountsEnabled() {
        return config.getBoolean("discounts.enabled", true);
    }

    public double getMaxDiscount() {
        return config.getDouble("discounts.max-discount", 90.0);
    }

    public FileConfiguration raw() { return config; }
}
