package com.italiarevenge.iRShop.model;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a top-level shop that contains one or more {@link ShopCategory} objects.
 * All fields except {@code categories} are persisted to the database.
 */
public class Shop {

    private final String id;
    private String name;
    private String displayName;
    private String description;
    private ItemStack icon;
    /** Currency provider key: "vault" or a Currencies Plugin currency name. */
    private String currencyId;
    private String layout;
    /** Optional permission node required to open this shop. {@code null} = open for all. */
    @Nullable
    private String permission;
    private int sortOrder;

    /** In-memory category list — populated by {@link com.italiarevenge.iRShop.service.CategoryService}. */
    private final List<ShopCategory> categories = new ArrayList<>();

    public Shop(
            @NotNull String id,
            @NotNull String name,
            @NotNull String displayName,
            @Nullable String description,
            @NotNull ItemStack icon,
            @NotNull String currencyId,
            @NotNull String layout,
            @Nullable String permission,
            int sortOrder
    ) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.currencyId = currencyId;
        this.layout = layout;
        this.permission = permission;
        this.sortOrder = sortOrder;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    @NotNull public String getId() { return id; }
    @NotNull public String getName() { return name; }
    @NotNull public String getDisplayName() { return displayName; }
    @Nullable public String getDescription() { return description; }
    @NotNull public ItemStack getIcon() { return icon.clone(); }
    @NotNull public String getCurrencyId() { return currencyId; }
    @NotNull public String getLayout() { return layout; }
    @Nullable public String getPermission() { return permission; }
    public int getSortOrder() { return sortOrder; }

    @NotNull
    public List<ShopCategory> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setName(@NotNull String name) { this.name = name; }
    public void setDisplayName(@NotNull String displayName) { this.displayName = displayName; }
    public void setDescription(@Nullable String description) { this.description = description; }
    public void setIcon(@NotNull ItemStack icon) { this.icon = icon.clone(); }
    public void setCurrencyId(@NotNull String currencyId) { this.currencyId = currencyId; }
    public void setLayout(@NotNull String layout) { this.layout = layout; }
    public void setPermission(@Nullable String permission) { this.permission = permission; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    // ── Category management (in-memory only) ─────────────────────────────────

    public void addCategory(@NotNull ShopCategory category) {
        categories.add(category);
        categories.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
    }

    public void removeCategory(@NotNull String categoryId) {
        categories.removeIf(c -> c.getId().equals(categoryId));
    }

    public void clearCategories() {
        categories.clear();
    }

    @Nullable
    public ShopCategory getCategoryById(@NotNull String categoryId) {
        return categories.stream()
                .filter(c -> c.getId().equals(categoryId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        return "Shop{id='" + id + "', name='" + name + "', categories=" + categories.size() + "}";
    }
}
