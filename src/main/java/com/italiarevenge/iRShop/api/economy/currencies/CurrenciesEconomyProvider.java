package com.italiarevenge.iRShop.api.economy.currencies;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.api.economy.CurrencyInfo;
import com.italiarevenge.iRShop.api.economy.EconomyProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Economy provider backed by ItaliaRevengee/currencies-plugin.
 *
 * <p>Integration uses reflection so the plugin compiles even if
 * currencies-plugin is not on the classpath. Replace the reflection
 * calls with direct API calls once you have confirmed the exact API
 * surface from the currencies-plugin source.
 *
 * <h3>Assumed API contract</h3>
 * <pre>{@code
 *   // Main class or API accessor
 *   Object api = plugin.getAPI();              // returns CurrenciesAPI
 *   double  getBalance(UUID playerId, String currency)
 *   boolean addBalance(UUID playerId, String currency, double amount)
 *   boolean removeBalance(UUID playerId, String currency, double amount)
 *   List<String> getCurrencies()
 *   String  getSymbol(String currency)
 *   String  format(String currency, double amount)
 * }</pre>
 */
public class CurrenciesEconomyProvider implements EconomyProvider {

    private final IRShop plugin;
    private final Logger log;
    private final String currencyId;

    @Nullable
    private Object apiInstance;
    @Nullable
    private Method methodGetBalance;
    @Nullable
    private Method methodAddBalance;
    @Nullable
    private Method methodRemoveBalance;
    @Nullable
    private Method methodGetCurrencies;
    @Nullable
    private Method methodGetSymbol;
    @Nullable
    private Method methodFormat;

    private boolean available = false;

    /** Root provider: scans all currencies. */
    public CurrenciesEconomyProvider(@NotNull IRShop plugin) {
        this(plugin, plugin.getConfig().getString("economy.currencies-plugin.default-currency", "money"));
    }

    /** Per-currency provider used internally by {@link com.italiarevenge.iRShop.api.economy.EconomyManager}. */
    private CurrenciesEconomyProvider(@NotNull IRShop plugin, @NotNull String currencyId) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.currencyId = currencyId;
        setupAPI();
    }

    /** Returns a provider proxy bound to a specific currency. */
    @NotNull
    public CurrenciesEconomyProvider forCurrency(@NotNull String currency) {
        CurrenciesEconomyProvider proxy = new CurrenciesEconomyProvider(plugin, currency);
        proxy.apiInstance = this.apiInstance;
        proxy.methodGetBalance = this.methodGetBalance;
        proxy.methodAddBalance = this.methodAddBalance;
        proxy.methodRemoveBalance = this.methodRemoveBalance;
        proxy.methodGetCurrencies = this.methodGetCurrencies;
        proxy.methodGetSymbol = this.methodGetSymbol;
        proxy.methodFormat = this.methodFormat;
        proxy.available = this.available;
        return proxy;
    }

    private void setupAPI() {
        Plugin cp = plugin.getServer().getPluginManager().getPlugin("CurrenciesPlugin");
        if (cp == null) {
            // Try alternative plugin name
            cp = plugin.getServer().getPluginManager().getPlugin("Currencies");
        }
        if (cp == null) return;

        try {
            Method getApi = cp.getClass().getMethod("getAPI");
            apiInstance = getApi.invoke(cp);
            if (apiInstance == null) return;

            Class<?> apiClass = apiInstance.getClass();
            methodGetBalance    = apiClass.getMethod("getBalance", java.util.UUID.class, String.class);
            methodAddBalance    = apiClass.getMethod("addBalance", java.util.UUID.class, String.class, double.class);
            methodRemoveBalance = apiClass.getMethod("removeBalance", java.util.UUID.class, String.class, double.class);
            methodGetCurrencies = apiClass.getMethod("getCurrencies");
            // These may not exist; wrap in individual try-catch
            try { methodGetSymbol = apiClass.getMethod("getSymbol", String.class); } catch (NoSuchMethodException ignored) {}
            try { methodFormat    = apiClass.getMethod("format", String.class, double.class); } catch (NoSuchMethodException ignored) {}

            available = true;
            log.info("Currencies Plugin API bound via reflection.");
        } catch (Exception e) {
            log.warning("Could not bind Currencies Plugin API: " + e.getMessage() +
                        " — update CurrenciesEconomyProvider to match the actual API.");
        }
    }

    @Override
    public @NotNull String getId() { return currencyId; }

    @Override
    public @NotNull String getDisplayName() {
        String sym = getSymbolFor(currencyId);
        return sym.isBlank() ? currencyId : currencyId + " (" + sym + ")";
    }

    @Override
    public boolean isAvailable() { return available; }

    @Override
    public double getBalance(@NotNull Player player) {
        if (!available || methodGetBalance == null) return 0;
        try {
            Object result = methodGetBalance.invoke(apiInstance, player.getUniqueId(), currencyId);
            return result instanceof Number n ? n.doubleValue() : 0;
        } catch (Exception e) {
            log.fine("getBalance error: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean deposit(@NotNull Player player, double amount) {
        if (!available || methodAddBalance == null) return false;
        try {
            Object result = methodAddBalance.invoke(apiInstance, player.getUniqueId(), currencyId, amount);
            return result instanceof Boolean b ? b : true;
        } catch (Exception e) {
            log.fine("deposit error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean withdraw(@NotNull Player player, double amount) {
        if (!available || methodRemoveBalance == null) return false;
        try {
            Object result = methodRemoveBalance.invoke(apiInstance, player.getUniqueId(), currencyId, amount);
            return result instanceof Boolean b ? b : true;
        } catch (Exception e) {
            log.fine("withdraw error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public @NotNull String format(double amount) {
        if (!available || methodFormat == null) return currencyId + " " + String.format("%.2f", amount);
        try {
            Object result = methodFormat.invoke(apiInstance, currencyId, amount);
            return result != null ? result.toString() : String.format("%.2f", amount);
        } catch (Exception e) {
            return String.format("%.2f", amount);
        }
    }

    @Override
    public @NotNull CurrencyInfo getCurrencyInfo() {
        String sym = getSymbolFor(currencyId);
        return new CurrencyInfo(currencyId, currencyId, sym, Material.EMERALD);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull List<String> getCurrencyIds() {
        if (!available || methodGetCurrencies == null) return List.of(currencyId);
        try {
            Object result = methodGetCurrencies.invoke(apiInstance);
            if (result instanceof List<?> list) return (List<String>) list;
        } catch (Exception e) {
            log.fine("getCurrencies error: " + e.getMessage());
        }
        return List.of(currencyId);
    }

    private String getSymbolFor(String currency) {
        if (!available || methodGetSymbol == null) return "";
        try {
            Object r = methodGetSymbol.invoke(apiInstance, currency);
            return r != null ? r.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
