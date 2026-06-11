package com.italiarevenge.iRShop.economy;

import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.italiarevenge.iRShop.IRShop;

import net.milkbowl.vault.economy.Economy;

public class EconomyManager {

    private final IRShop plugin;
    private Economy economy;

    public EconomyManager(IRShop plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean isAvailable() { return economy != null; }

    public double getBalance(Player player) {
        return economy != null ? economy.getBalance(player) : 0;
    }

    public boolean has(Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (economy == null) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        if (economy == null) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        if (economy != null) {
            String s = economy.format(amount);
            // If Vault's formatter lost decimal precision (e.g. 0.40 → "0"), fall back
            String digits = s.replaceAll("[^0-9.]", "");
            if (!digits.isEmpty()) {
                try {
                    if (Math.abs(Double.parseDouble(digits) - amount) <= 0.005)
                        return s;
                } catch (NumberFormatException ignored) {}
            }
        }
        // Own formatter: integer if whole number, otherwise strip trailing zeros
        if (amount == Math.floor(amount) && !Double.isInfinite(amount))
            return String.valueOf((long) amount);
        return new java.math.BigDecimal(String.valueOf(amount)).stripTrailingZeros().toPlainString();
    }
}
