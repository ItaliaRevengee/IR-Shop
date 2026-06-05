package com.italiarevenge.iRShop.layout;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Defines the visual structure of a shop GUI:
 * background fill, border material, navigation slot positions,
 * and which slots display shop items.
 */
public class Layout {

    private final String id;
    private final String displayName;
    private final int rows;
    private final Material backgroundMaterial;
    private final Material borderMaterial;
    private final Map<String, Integer> navSlots;
    private final List<Integer> itemSlots;

    public Layout(
            @NotNull String id,
            @NotNull String displayName,
            int rows,
            @NotNull Material backgroundMaterial,
            @NotNull Material borderMaterial,
            @NotNull Map<String, Integer> navSlots,
            @NotNull List<Integer> itemSlots
    ) {
        this.id = id;
        this.displayName = displayName;
        this.rows = Math.max(1, Math.min(6, rows));
        this.backgroundMaterial = backgroundMaterial;
        this.borderMaterial = borderMaterial;
        this.navSlots = Map.copyOf(navSlots);
        this.itemSlots = List.copyOf(itemSlots);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    @NotNull public String getId() { return id; }
    @NotNull public String getDisplayName() { return displayName; }
    public int getRows() { return rows; }
    public int getSize() { return rows * 9; }
    @NotNull public Material getBackgroundMaterial() { return backgroundMaterial; }
    @NotNull public Material getBorderMaterial() { return borderMaterial; }

    /** Returns the slot index for a named navigation button, or {@code -1} if not configured. */
    public int getNavSlot(@NotNull String key) {
        return navSlots.getOrDefault(key, -1);
    }

    @NotNull
    public Map<String, Integer> getNavSlots() { return navSlots; }

    /**
     * Returns the ordered list of inventory slots that hold shop items.
     * Use {@link #getItemSlot(int)} for index-safe access.
     */
    @NotNull
    public List<Integer> getItemSlots() { return itemSlots; }

    /**
     * Returns the inventory slot at position {@code index} within the item-slot list.
     * Returns {@code -1} when {@code index} is out of bounds.
     */
    public int getItemSlot(int index) {
        if (index < 0 || index >= itemSlots.size()) return -1;
        return itemSlots.get(index);
    }

    /** Maximum number of items visible per page with this layout. */
    public int getItemsPerPage() { return itemSlots.size(); }

    /** Returns {@code true} if the given inventory slot is a nav or border slot (not an item slot). */
    public boolean isDecorSlot(int slot) {
        return !itemSlots.contains(slot);
    }
}
