package com.italiarevenge.iRShop.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after IR-Shop completes a reload (/shopadmin reload).
 * Non-cancellable — the reload has already happened.
 */
public class ShopReloadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final long durationMs;

    public ShopReloadEvent(long durationMs) {
        this.durationMs = durationMs;
    }

    /** Returns the time the reload took in milliseconds. */
    public long getDurationMs() { return durationMs; }

    @Override @NotNull public HandlerList getHandlers() { return HANDLERS; }
    @NotNull public static HandlerList getHandlerList() { return HANDLERS; }
}
