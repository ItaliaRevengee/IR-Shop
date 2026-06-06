package com.italiarevenge.iRShop.gui;

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

    public Inventory getInventory() { return inventory; }
}
