package com.italiarevenge.iRShop.api.economy;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable metadata for a single currency.
 *
 * @param id       Unique identifier (e.g. "vault", "gems")
 * @param name     Human-readable name (e.g. "Dollars", "Gems")
 * @param symbol   Short symbol prepended/appended when formatting (e.g. "$", "♦")
 * @param icon     Material used to represent this currency in GUIs
 */
public record CurrencyInfo(
        @NotNull String id,
        @NotNull String name,
        @NotNull String symbol,
        @NotNull Material icon
) {

    /** Convenience factory with a default GOLD_NUGGET icon. */
    public static CurrencyInfo of(@NotNull String id, @NotNull String name, @NotNull String symbol) {
        return new CurrencyInfo(id, name, symbol, Material.GOLD_NUGGET);
    }

    /** Formats {@code amount} as "{symbol}{amount}" (e.g. "$100.00"). */
    @NotNull
    public String format(double amount) {
        return symbol + String.format("%.2f", amount);
    }
}
