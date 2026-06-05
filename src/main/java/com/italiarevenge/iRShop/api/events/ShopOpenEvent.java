package com.italiarevenge.iRShop.api.events;

import com.italiarevenge.iRShop.model.Shop;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player opens a {@link Shop}.
 * Cancelling this event prevents the shop GUI from being displayed.
 */
public class ShopOpenEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Shop shop;
    private boolean cancelled;

    public ShopOpenEvent(@NotNull Player player, @NotNull Shop shop) {
        this.player = player;
        this.shop = shop;
    }

    @NotNull
    public Player getPlayer() { return player; }

    @NotNull
    public Shop getShop() { return shop; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    @NotNull
    public HandlerList getHandlers() { return HANDLERS; }

    @NotNull
    public static HandlerList getHandlerList() { return HANDLERS; }
}
