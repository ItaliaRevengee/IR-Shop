package com.italiarevenge.iRShop.api.economy;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.api.economy.currencies.CurrenciesEconomyProvider;
import com.italiarevenge.iRShop.api.economy.vault.VaultEconomyProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages all registered {@link EconomyProvider}s and resolves the correct
 * provider for a given currency identifier at runtime.
 *
 * <p>Providers are registered on plugin enable and can be swapped/reloaded
 * without a server restart.
 */
public class EconomyManager {

    private final IRShop plugin;
    private final Logger log;

    /** Ordered map: currencyId → provider. Insertion order preserved for /shop list. */
    private final Map<String, EconomyProvider> providers = new LinkedHashMap<>();

    /** The default provider used when no currency-specific match is found. */
    @Nullable
    private EconomyProvider defaultProvider;

    public EconomyManager(@NotNull IRShop plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void initialize() {
        providers.clear();

        // Vault
        VaultEconomyProvider vault = new VaultEconomyProvider(plugin);
        if (vault.isAvailable()) {
            registerProvider(vault);
            log.info("Economy: Vault registered (currency id: vault).");
        } else {
            log.warning("Economy: Vault not found — Vault-based shops will be unavailable.");
        }

        // Currencies Plugin
        CurrenciesEconomyProvider currencies = new CurrenciesEconomyProvider(plugin);
        if (currencies.isAvailable()) {
            for (String currencyId : currencies.getCurrencyIds()) {
                // Each Currencies Plugin currency gets its own provider proxy
                registerProvider(currencies.forCurrency(currencyId));
            }
            log.info("Economy: Currencies Plugin registered (" + currencies.getCurrencyIds().size() + " currencies).");
        }

        // Set default from config
        String defaultId = plugin.getConfig().getString("economy.default-provider", "vault");
        defaultProvider = providers.get(defaultId);
        if (defaultProvider == null && !providers.isEmpty()) {
            defaultProvider = providers.values().iterator().next();
        }
        if (defaultProvider != null) {
            log.info("Economy: Default provider → " + defaultProvider.getId());
        } else {
            log.severe("Economy: No economy provider available! Shop transactions will fail.");
        }
    }

    public void reload() {
        initialize();
    }

    // ── Provider registry ─────────────────────────────────────────────────────

    /**
     * Registers or replaces a provider. Can be called by external plugins via
     * {@link com.italiarevenge.iRShop.api.IRShopAPI}.
     */
    public void registerProvider(@NotNull EconomyProvider provider) {
        providers.put(provider.getId(), provider);
    }

    /**
     * Returns the provider bound to {@code currencyId}, or the default provider
     * if none is registered for that id.
     *
     * @throws IllegalStateException when no provider is available at all
     */
    @NotNull
    public EconomyProvider getProvider(@NotNull String currencyId) {
        EconomyProvider p = providers.get(currencyId);
        if (p != null && p.isAvailable()) return p;

        // Fall back to default
        if (defaultProvider != null && defaultProvider.isAvailable()) return defaultProvider;

        throw new IllegalStateException("No available economy provider for currency: " + currencyId);
    }

    /** Returns the default provider (may be {@code null} if nothing is loaded). */
    @Nullable
    public EconomyProvider getDefaultProvider() {
        return defaultProvider;
    }

    @NotNull
    public Collection<EconomyProvider> getAllProviders() {
        return providers.values();
    }

    public boolean hasProvider(@NotNull String currencyId) {
        return providers.containsKey(currencyId) && providers.get(currencyId).isAvailable();
    }
}
