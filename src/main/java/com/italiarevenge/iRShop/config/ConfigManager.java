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
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
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
