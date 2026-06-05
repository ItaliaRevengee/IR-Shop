package com.italiarevenge.iRShop.listener;

import com.italiarevenge.iRShop.IRShop;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Handles player join/quit for data pre-loading and cleanup.
 */
public class PlayerListener implements Listener {

    private final IRShop plugin;

    public PlayerListener(@NotNull IRShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        // Pre-load player data asynchronously on join so it's ready when they open the shop
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                plugin.getPlayerService().getOrCreate(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        // Close any open GUI to trigger cleanup
        var gui = plugin.getGuiManager().getOpenGui(event.getPlayer());
        if (gui != null) {
            plugin.getGuiManager().unregister(event.getPlayer());
        }
        // Flush player data to database
        plugin.getPlayerService().unload(event.getPlayer().getUniqueId());
    }
}
