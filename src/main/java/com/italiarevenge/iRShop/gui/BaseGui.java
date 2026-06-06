package com.italiarevenge.iRShop.gui;

import com.italiarevenge.iRShop.IRShop;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

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

    public Inventory getInventory() { return inventory; }

    protected void playSound(String key) {
        IRShop.get().getConfigManager().playSound(player, key);
    }
}
