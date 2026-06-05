package com.italiarevenge.iRShop.api.events;

import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player opens a {@link ShopCategory} within a {@link Shop}.
 */
public class ShopCategoryOpenEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Shop shop;
    private final ShopCategory category;
    private boolean cancelled;

    public ShopCategoryOpenEvent(
            @NotNull Player player,
            @NotNull Shop shop,
            @NotNull ShopCategory category
    ) {
        this.player = player;
        this.shop = shop;
        this.category = category;
    }

    @NotNull public Player getPlayer() { return player; }
    @NotNull public Shop getShop() { return shop; }
    @NotNull public ShopCategory getCategory() { return category; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override @NotNull public HandlerList getHandlers() { return HANDLERS; }
    @NotNull public static HandlerList getHandlerList() { return HANDLERS; }
}
