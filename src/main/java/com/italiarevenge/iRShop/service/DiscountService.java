package com.italiarevenge.iRShop.service;

import com.italiarevenge.iRShop.IRShop;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Calculates permission-based buy discounts and sell multipliers.
 *
 * <h3>Discount permissions</h3>
 * Format: {@code irshop.discount.<value>} — value is a whole-number percentage.
 * The highest held permission wins.
 *
 * <h3>Sell multiplier permissions</h3>
 * Format: {@code irshop.sell.<value>} — value supports one decimal place (e.g. "1.5").
 * The highest held permission wins.
 */
public class DiscountService {

    private static final int[] DISCOUNT_TIERS = {90, 75, 50, 40, 30, 25, 20, 15, 10, 5};
    private static final double[] SELL_TIERS   = {5.0, 4.0, 3.0, 2.5, 2.0, 1.5, 1.25};

    private final IRShop plugin;

    public DiscountService(@NotNull IRShop plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns the highest buy discount (in percent) the player has permission for.
     * Returns {@code 0} if discounts are disabled or the player has no discount node.
     */
    public int getDiscount(@NotNull Player player) {
        if (!plugin.getConfigManager().areDiscountsEnabled()) return 0;
        int maxAllowed = plugin.getConfigManager().getMaxDiscount();
        for (int tier : DISCOUNT_TIERS) {
            if (tier <= maxAllowed && player.hasPermission("irshop.discount." + tier)) return tier;
        }
        return 0;
    }

    /**
     * Returns the highest sell multiplier the player has permission for.
     * Returns {@code 1.0} if multipliers are disabled or the player has none.
     */
    public double getSellMultiplier(@NotNull Player player) {
        if (!plugin.getConfigManager().areSellMultipliersEnabled()) return 1.0;
        for (double tier : SELL_TIERS) {
            // Convert 1.5 → "1.5", 2.0 → "2" for the permission node
            String node = tier == Math.floor(tier)
                    ? "irshop.sell." + (int) tier
                    : "irshop.sell." + tier;
            if (player.hasPermission(node)) return tier;
        }
        return 1.0;
    }

    /**
     * Applies a buy discount to a price.
     * {@code discount = 10} reduces price by 10%.
     */
    public double applyDiscount(double price, int discountPercent) {
        if (discountPercent <= 0) return price;
        return price * (1.0 - discountPercent / 100.0);
    }

    /**
     * Applies a sell multiplier to a base sell price.
     */
    public double applySellMultiplier(double sellPrice, double multiplier) {
        if (multiplier <= 1.0) return sellPrice;
        return sellPrice * multiplier;
    }

    /**
     * Returns the effective buy price for a player, after discount.
     */
    public double getEffectiveBuyPrice(double basePrice, @NotNull Player player) {
        return applyDiscount(basePrice, getDiscount(player));
    }

    /**
     * Returns the effective sell price for a player, after multiplier.
     */
    public double getEffectiveSellPrice(double basePrice, @NotNull Player player) {
        return applySellMultiplier(basePrice, getSellMultiplier(player));
    }
}
