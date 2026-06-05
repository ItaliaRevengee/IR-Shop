package com.italiarevenge.iRShop.api.economy.vault;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.api.economy.CurrencyInfo;
import com.italiarevenge.iRShop.api.economy.EconomyProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Economy provider backed by Vault's {@link Economy} API.
 */
public class VaultEconomyProvider implements EconomyProvider {

    public static final String CURRENCY_ID = "vault";

    private final IRShop plugin;
    private Economy economy;

    public VaultEconomyProvider(@NotNull IRShop plugin) {
        this.plugin = plugin;
        setupVault();
    }

    private void setupVault() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) economy = rsp.getProvider();
    }

    @Override
    public @NotNull String getId() { return CURRENCY_ID; }

    @Override
    public @NotNull String getDisplayName() {
        return economy != null ? economy.currencyNamePlural() : "Money";
    }

    @Override
    public boolean isAvailable() { return economy != null; }

    @Override
    public double getBalance(@NotNull Player player) {
        return economy != null ? economy.getBalance(player) : 0;
    }

    @Override
    public boolean deposit(@NotNull Player player, double amount) {
        if (economy == null) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean withdraw(@NotNull Player player, double amount) {
        if (economy == null) return false;
        if (!economy.has(player, amount)) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public @NotNull String format(double amount) {
        return economy != null ? economy.format(amount) : String.format("$%.2f", amount);
    }

    @Override
    public @NotNull CurrencyInfo getCurrencyInfo() {
        String name = economy != null ? economy.currencyNamePlural() : "Money";
        String symbol = "$";
        return new CurrencyInfo(CURRENCY_ID, name, symbol, Material.GOLD_INGOT);
    }

    @Override
    public @NotNull List<String> getCurrencyIds() {
        return List.of(CURRENCY_ID);
    }
}
