package com.italiarevenge.iRShop.gui.framework;

import com.italiarevenge.iRShop.layout.Layout;
import com.italiarevenge.iRShop.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link BaseGui} with pagination support and layout-aware slot mapping.
 *
 * <p>Subclasses push items into the {@code pageItems} list and call
 * {@link #renderPage()} to display the current page.
 */
public abstract class PaginatedGui extends BaseGui {

    protected final Layout layout;
    protected final List<ItemStack> pageItems = new ArrayList<>();
    protected int currentPage = 0; // 0-indexed

    protected PaginatedGui(@NotNull Player player, @NotNull Layout layout, @NotNull Component title) {
        super(player, layout.getRows(), title);
        this.layout = layout;
    }

    // ── Pagination logic ──────────────────────────────────────────────────────

    protected int getTotalPages() {
        int perPage = layout.getItemsPerPage();
        if (perPage <= 0) return 1;
        return Math.max(1, (int) Math.ceil((double) pageItems.size() / perPage));
    }

    protected boolean hasNextPage() { return currentPage < getTotalPages() - 1; }
    protected boolean hasPrevPage() { return currentPage > 0; }

    protected void nextPage() { if (hasNextPage()) { currentPage++; refresh(); } }
    protected void prevPage() { if (hasPrevPage()) { currentPage--; refresh(); } }

    /**
     * Clears and rebuilds the current page without reopening the inventory.
     * Safer than calling {@link #open()} again from a click handler.
     */
    public void refresh() {
        clearAll();
        build();
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Renders the slice of {@code pageItems} for the current page into the
     * layout's item slots, and paints navigation buttons.
     */
    protected void renderPage() {
        int perPage = layout.getItemsPerPage();
        int start   = currentPage * perPage;

        // Place item-slot items
        for (int i = 0; i < perPage; i++) {
            int slot = layout.getItemSlot(i);
            if (slot < 0) continue;
            int itemIndex = start + i;
            if (itemIndex < pageItems.size()) {
                inventory.setItem(slot, pageItems.get(itemIndex));
            } else {
                inventory.setItem(slot, null);
                actions.remove(slot);
            }
        }

        // Fill background on every non-item, non-nav slot
        ItemStack bg = ItemUtil.filler(layout.getBackgroundMaterial());
        ItemStack border = ItemUtil.filler(layout.getBorderMaterial());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (!layout.getItemSlots().contains(slot) && inventory.getItem(slot) == null) {
                boolean isEdge = isEdgeSlot(slot, layout.getRows());
                inventory.setItem(slot, isEdge ? border : bg);
            }
        }
    }

    /**
     * Adds a navigation button at the layout-configured slot.
     * No-op if the layout does not define that button.
     */
    protected void addNavButton(@NotNull String key, @NotNull ItemStack icon,
                                @NotNull Runnable onClick) {
        int slot = layout.getNavSlot(key);
        if (slot < 0) return;
        set(slot, icon, e -> onClick.run());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if {@code slot} is on the top or bottom row (border row).
     */
    private boolean isEdgeSlot(int slot, int rows) {
        int row = slot / 9;
        return row == 0 || row == rows - 1;
    }

    /** Exposes the action map to subclasses (needed for item-slot actions set outside renderPage). */
    protected java.util.Map<Integer, java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent>> actions() {
        // Subclasses that need direct action access use setAction() instead.
        // This is intentionally package-private.
        return Map.of();
    }

    protected int itemIndexOnPage(int layoutItemIndex) {
        return currentPage * layout.getItemsPerPage() + layoutItemIndex;
    }
}
