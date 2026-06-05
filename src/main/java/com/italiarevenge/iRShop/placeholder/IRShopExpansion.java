package com.italiarevenge.iRShop.placeholder;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.model.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for IR-Shop.
 *
 * <h3>Available placeholders</h3>
 * <ul>
 *   <li>{@code %irshop_balance%} — Vault balance (formatted)</li>
 *   <li>{@code %irshop_balance_<currency>%} — Currencies Plugin balance for named currency</li>
 *   <li>{@code %irshop_currency%} — Default currency display name</li>
 *   <li>{@code %irshop_total_purchases%} — Total items purchased by player</li>
 *   <li>{@code %irshop_total_sales%} — Total items sold by player</li>
 *   <li>{@code %irshop_total_spent%} — Total money spent by player</li>
 *   <li>{@code %irshop_total_earned%} — Total money earned by player</li>
 *   <li>{@code %irshop_discount%} — Current buy discount percentage</li>
 *   <li>{@code %irshop_sell_multiplier%} — Current sell multiplier</li>
 * </ul>
 */
public class IRShopExpansion extends PlaceholderExpansion {

    private final IRShop plugin;

    public IRShopExpansion(@NotNull IRShop plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull public String getIdentifier() { return "irshop"; }
    @Override @NotNull public String getAuthor() { return "ItaliaRevengee"; }
    @Override @NotNull public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public boolean canRegister() { return true; }

    @Override
    @Nullable
    public String onRequest(@Nullable OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) return "";

        var player = offlinePlayer.getPlayer();

        return switch (params.toLowerCase()) {
            case "balance" -> {
                var provider = plugin.getEconomyManager().getDefaultProvider();
                if (provider == null || player == null) yield "0";
                yield provider.format(provider.getBalance(player));
            }
            case "currency" -> {
                var provider = plugin.getEconomyManager().getDefaultProvider();
                yield provider != null ? provider.getDisplayName() : "?";
            }
            case "total_purchases" -> {
                PlayerData data = plugin.getPlayerService().getCached(offlinePlayer.getUniqueId());
                yield data != null ? String.valueOf(data.getTotalPurchases()) : "0";
            }
            case "total_sales" -> {
                PlayerData data = plugin.getPlayerService().getCached(offlinePlayer.getUniqueId());
                yield data != null ? String.valueOf(data.getTotalSales()) : "0";
            }
            case "total_spent" -> {
                PlayerData data = plugin.getPlayerService().getCached(offlinePlayer.getUniqueId());
                yield data != null ? String.format("%.2f", data.getTotalSpent()) : "0.00";
            }
            case "total_earned" -> {
                PlayerData data = plugin.getPlayerService().getCached(offlinePlayer.getUniqueId());
                yield data != null ? String.format("%.2f", data.getTotalEarned()) : "0.00";
            }
            case "discount" -> {
                if (player == null) yield "0";
                yield String.valueOf(plugin.getDiscountService().getDiscount(player));
            }
            case "sell_multiplier" -> {
                if (player == null) yield "1.0";
                yield String.valueOf(plugin.getDiscountService().getSellMultiplier(player));
            }
            default -> {
                // Handle %irshop_balance_<currency>%
                if (params.startsWith("balance_")) {
                    String currency = params.substring("balance_".length());
                    if (player == null) yield "0";
                    try {
                        var prov = plugin.getEconomyManager().getProvider(currency);
                        yield prov.format(prov.getBalance(player));
                    } catch (Exception e) {
                        yield "0";
                    }
                }
                yield null;
            }
        };
    }
}
