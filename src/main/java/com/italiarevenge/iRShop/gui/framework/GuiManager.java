package com.italiarevenge.iRShop.gui.framework;

import com.italiarevenge.iRShop.IRShop;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry of open {@link BaseGui} instances.
 *
 * <p>{@link com.italiarevenge.iRShop.listener.InventoryListener} routes all inventory
 * events here. This keeps all GUI state management in one place.
 */
public class GuiManager {

    private final IRShop plugin;
    /** uuid → currently open GUI */
    private final Map<UUID, BaseGui> openGuis = new ConcurrentHashMap<>();

    public GuiManager(@NotNull IRShop plugin) {
        this.plugin = plugin;
    }

    // ── Open / close tracking ─────────────────────────────────────────────────

    /** Registers a GUI as open for the given player. Called from {@link BaseGui#open()}. */
    public void register(@NotNull Player player, @NotNull BaseGui gui) {
        openGuis.put(player.getUniqueId(), gui);
    }

    public void unregister(@NotNull Player player) {
        openGuis.remove(player.getUniqueId());
    }

    /** Closes all open GUIs (called on plugin disable). */
    public void closeAll() {
        for (UUID uuid : openGuis.keySet()) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) p.closeInventory();
        }
        openGuis.clear();
    }

    // ── Event routing ─────────────────────────────────────────────────────────

    /**
     * Routes an inventory click to the correct {@link BaseGui}.
     * Returns {@code true} if a GUI handled the event.
     */
    public boolean handleClick(@NotNull InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BaseGui gui)) return false;
        gui.handleClick(event);
        return true;
    }

    /**
     * Routes an inventory close to the correct GUI and unregisters it.
     */
    public void handleClose(@NotNull InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof BaseGui gui)) return;
        gui.onClose(event);
        openGuis.remove(event.getPlayer().getUniqueId());
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Nullable
    public BaseGui getOpenGui(@NotNull Player player) {
        return openGuis.get(player.getUniqueId());
    }

    public boolean hasOpenGui(@NotNull Player player) {
        return openGuis.containsKey(player.getUniqueId());
    }

    public int getOpenCount() { return openGuis.size(); }
}
