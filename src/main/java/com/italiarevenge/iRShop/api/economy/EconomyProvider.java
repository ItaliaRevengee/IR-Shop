package com.italiarevenge.iRShop.api.economy;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Abstraction layer over any economy backend (Vault, Currencies Plugin, …).
 *
 * <p>All balance-changing methods return {@code true} on success and {@code false}
 * when the operation could not be completed (e.g. insufficient funds, provider
 * unavailable).
 */
public interface EconomyProvider {

    /** Unique identifier used in config / database (e.g. "vault", "gems", "tokens"). */
    @NotNull
    String getId();

    /** Human-readable name shown in shop GUIs. */
    @NotNull
    String getDisplayName();

    /** Returns {@code true} if the underlying plugin/service is available and ready. */
    boolean isAvailable();

    /**
     * Returns the player's current balance for this currency.
     * Callers must ensure the player is online.
     */
    double getBalance(@NotNull Player player);

    /**
     * Returns {@code true} if the player has at least {@code amount} in this currency.
     */
    default boolean hasBalance(@NotNull Player player, double amount) {
        return getBalance(player) >= amount;
    }

    /**
     * Deposits {@code amount} into the player's balance.
     *
     * @return {@code true} on success
     */
    boolean deposit(@NotNull Player player, double amount);

    /**
     * Withdraws {@code amount} from the player's balance.
     *
     * @return {@code true} on success; {@code false} if insufficient funds
     */
    boolean withdraw(@NotNull Player player, double amount);

    /**
     * Formats {@code amount} using this currency's symbol/format string
     * (e.g. "$1,234.00", "1,234 Gems").
     */
    @NotNull
    String format(double amount);

    /**
     * Returns metadata about this currency (name, symbol, icon material).
     */
    @NotNull
    CurrencyInfo getCurrencyInfo();

    /**
     * Returns all currency identifiers exposed by this provider.
     * For single-currency providers (Vault) this is a one-element list.
     * For multi-currency providers (Currencies Plugin) this lists every available currency.
     */
    @NotNull
    List<String> getCurrencyIds();
}
