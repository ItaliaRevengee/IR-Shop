package com.italiarevenge.iRShop.gui;

import com.italiarevenge.iRShop.IRShop;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

public abstract class BaseGui {

    protected final Player player;
    protected Inventory inventory;

    protected BaseGui(Player player) {
        this.player = player;
    }

    public abstract void open();
    public abstract void handleClick(InventoryClickEvent event);

    /** Called when the player closes this GUI. Override to cancel tasks/cleanup. */
    public void onClose() {}

    /**
     * Called when the player clicks in their own inventory while this GUI is open.
     * Override to handle shift-click interactions (e.g. SellGui input slots).
     */
    public void handlePlayerInventoryClick(InventoryClickEvent event) {}

    /**
     * Returns true if the given raw GUI slot allows free item interaction (drag & click).
     * Used by GuiListener to selectively un-cancel drag events.
     */
    public boolean isInteractiveSlot(int slot) { return false; }

    public Inventory getInventory() { return inventory; }

    protected void playSound(String key) {
        IRShop.get().getConfigManager().playSound(player, key);
    }
}
