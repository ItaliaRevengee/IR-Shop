package com.italiarevenge.iRShop.gui.framework;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base class for all IR-Shop GUI windows.
 *
 * <p>Slot actions are registered via {@link #setAction(int, Consumer)} and
 * dispatched by {@link GuiManager} through {@link #handleClick(InventoryClickEvent)}.
 *
 * <p>Subclasses implement {@link #build()} to populate the inventory.
 */
public abstract class BaseGui implements InventoryHolder {

    protected final Player player;
    protected final Inventory inventory;
    protected final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected BaseGui(@NotNull Player player, int rows, @NotNull Component title) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    // ── Abstract ──────────────────────────────────────────────────────────────

    /** Populates the inventory with items and registers slot actions. */
    public abstract void build();

    // ── Action registration ───────────────────────────────────────────────────

    protected void setAction(int slot, @Nullable Consumer<InventoryClickEvent> action) {
        if (action == null) actions.remove(slot);
        else actions.put(slot, action);
    }

    protected void clearActions() { actions.clear(); }

    // ── Event dispatch ────────────────────────────────────────────────────────

    /**
     * Called by {@link GuiManager} on every click inside this GUI.
     * Returns {@code true} if the event was consumed (action existed for slot).
     */
    public boolean handleClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(true); // always cancel — players cannot move items by default
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return false;
        Consumer<InventoryClickEvent> action = actions.get(slot);
        if (action != null) {
            action.accept(event);
            return true;
        }
        return false;
    }

    /** Called by {@link GuiManager} when this GUI is closed. Override to add cleanup. */
    public void onClose(@NotNull InventoryCloseEvent event) {}

    // ── Helpers ───────────────────────────────────────────────────────────────

    protected void set(int slot, @Nullable ItemStack item) {
        inventory.setItem(slot, item);
    }

    protected void set(int slot, @Nullable ItemStack item, @Nullable Consumer<InventoryClickEvent> action) {
        inventory.setItem(slot, item);
        setAction(slot, action);
    }

    protected void clear(int slot) {
        inventory.setItem(slot, null);
        actions.remove(slot);
    }

    protected void clearAll() {
        inventory.clear();
        actions.clear();
    }

    // ── Open ──────────────────────────────────────────────────────────────────

    /** Builds and opens this GUI for the player. */
    public void open() {
        build();
        player.openInventory(inventory);
    }

    /**
     * Clears and rebuilds the GUI contents without reopening the inventory.
     * Override in paginated subclasses to also reset page state if needed.
     */
    public void refresh() {
        clearAll();
        build();
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    @NotNull public Player getPlayer() { return player; }

    @Override
    @NotNull public Inventory getInventory() { return inventory; }
}
