package com.italiarevenge.iRShop.model;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A category within a {@link Shop} that groups related {@link ShopItem}s together.
 */
public class ShopCategory {

    private final String id;
    private final String shopId;
    private String name;
    private String displayName;
    private String description;
    private ItemStack icon;
    /** Slot in the shop's category-selection GUI (0-based). */
    private int slot;
    /** Page in the category-selection GUI (1-based). */
    private int page;
    private int sortOrder;

    /** In-memory item list — populated by {@link com.italiarevenge.iRShop.service.ItemService}. */
    private final List<ShopItem> items = new ArrayList<>();

    public ShopCategory(
            @NotNull String id,
            @NotNull String shopId,
            @NotNull String name,
            @NotNull String displayName,
            @Nullable String description,
            @NotNull ItemStack icon,
            int slot,
            int page,
            int sortOrder
    ) {
        this.id = id;
        this.shopId = shopId;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.slot = slot;
        this.page = page;
        this.sortOrder = sortOrder;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    @NotNull public String getId() { return id; }
    @NotNull public String getShopId() { return shopId; }
    @NotNull public String getName() { return name; }
    @NotNull public String getDisplayName() { return displayName; }
    @Nullable public String getDescription() { return description; }
    @NotNull public ItemStack getIcon() { return icon.clone(); }
    public int getSlot() { return slot; }
    public int getPage() { return page; }
    public int getSortOrder() { return sortOrder; }

    @NotNull
    public List<ShopItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setName(@NotNull String name) { this.name = name; }
    public void setDisplayName(@NotNull String displayName) { this.displayName = displayName; }
    public void setDescription(@Nullable String description) { this.description = description; }
    public void setIcon(@NotNull ItemStack icon) { this.icon = icon.clone(); }
    public void setSlot(int slot) { this.slot = slot; }
    public void setPage(int page) { this.page = page; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    // ── Item management (in-memory only) ─────────────────────────────────────

    public void addItem(@NotNull ShopItem item) {
        items.add(item);
        items.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
    }

    public void removeItem(@NotNull String itemId) {
        items.removeIf(i -> i.getId().equals(itemId));
    }

    public void clearItems() {
        items.clear();
    }

    @Nullable
    public ShopItem getItemById(@NotNull String itemId) {
        return items.stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        return "ShopCategory{id='" + id + "', name='" + name + "', items=" + items.size() + "}";
    }
}
