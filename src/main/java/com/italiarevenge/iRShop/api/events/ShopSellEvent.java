package com.italiarevenge.iRShop.api.events;

import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.model.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before a player completes a sell.
 * Cancel to prevent the transaction; optionally set {@link #setCancelReason(String)}.
 */
public class ShopSellEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Shop shop;
    private final ShopCategory category;
    private final ShopItem shopItem;
    private int amount;
    private double totalPrice;
    private final String currency;
    private boolean cancelled;
    private String cancelReason;

    public ShopSellEvent(
            @NotNull Player player,
            @NotNull Shop shop,
            @NotNull ShopCategory category,
            @NotNull ShopItem shopItem,
            int amount,
            double totalPrice,
            @NotNull String currency
    ) {
        this.player = player;
        this.shop = shop;
        this.category = category;
        this.shopItem = shopItem;
        this.amount = amount;
        this.totalPrice = totalPrice;
        this.currency = currency;
    }

    @NotNull public Player getPlayer() { return player; }
    @NotNull public Shop getShop() { return shop; }
    @NotNull public ShopCategory getCategory() { return category; }
    @NotNull public ShopItem getShopItem() { return shopItem; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    @NotNull public String getCurrency() { return currency; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String reason) { this.cancelReason = reason; }

    @Override @NotNull public HandlerList getHandlers() { return HANDLERS; }
    @NotNull public static HandlerList getHandlerList() { return HANDLERS; }
}
