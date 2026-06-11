package com.italiarevenge.iRShop.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiListener implements Listener {

    private static final Map<UUID, BaseGui> openGuis = new HashMap<>();

    public static void register(Player player, BaseGui gui) {
        openGuis.put(player.getUniqueId(), gui);
    }

    public static void unregister(Player player) {
        openGuis.remove(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        BaseGui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;

        Inventory clicked = event.getClickedInventory();
        event.setCancelled(true);

        if (clicked == null) return;
        if (clicked.equals(gui.getInventory())) {
            gui.handleClick(event);
        } else if (clicked.equals(player.getInventory())) {
            gui.handlePlayerInventoryClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        BaseGui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;

        int guiSize = gui.getInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < guiSize && !gui.isInteractiveSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        BaseGui gui = openGuis.remove(player.getUniqueId());
        if (gui != null) gui.onClose();
    }
}
