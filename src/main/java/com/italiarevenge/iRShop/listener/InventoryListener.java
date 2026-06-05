package com.italiarevenge.iRShop.listener;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.gui.framework.BaseGui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Routes all inventory events to the appropriate {@link BaseGui} via {@link com.italiarevenge.iRShop.gui.framework.GuiManager}.
 */
public class InventoryListener implements Listener {

    private final IRShop plugin;

    public InventoryListener(@NotNull IRShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BaseGui) {
            plugin.getGuiManager().handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        // Prevent any item dragging inside IR-Shop GUIs
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BaseGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BaseGui) {
            plugin.getGuiManager().handleClose(event);
        }
    }
}
