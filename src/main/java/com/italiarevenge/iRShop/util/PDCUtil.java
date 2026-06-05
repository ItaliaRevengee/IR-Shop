package com.italiarevenge.iRShop.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper methods for reading and writing {@link PersistentDataContainer} values
 * on {@link ItemStack}s and other PDC holders.
 */
public final class PDCUtil {

    private PDCUtil() {}

    // ── Read ──────────────────────────────────────────────────────────────────

    @Nullable
    public static <T, Z> Z get(
            @NotNull ItemStack item,
            @NotNull NamespacedKey key,
            @NotNull PersistentDataType<T, Z> type
    ) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(key, type);
    }

    @Nullable
    public static String getString(@NotNull ItemStack item, @NotNull NamespacedKey key) {
        return get(item, key, PersistentDataType.STRING);
    }

    @Nullable
    public static Integer getInt(@NotNull ItemStack item, @NotNull NamespacedKey key) {
        return get(item, key, PersistentDataType.INTEGER);
    }

    @Nullable
    public static Double getDouble(@NotNull ItemStack item, @NotNull NamespacedKey key) {
        return get(item, key, PersistentDataType.DOUBLE);
    }

    public static boolean has(
            @NotNull ItemStack item,
            @NotNull NamespacedKey key,
            @NotNull PersistentDataType<?, ?> type
    ) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(key, type);
    }

    public static boolean has(@NotNull ItemStack item, @NotNull NamespacedKey key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.getKeys().contains(key);
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Writes a PDC value to an item and returns the mutated (cloned) ItemStack.
     * The original item is not modified.
     */
    @NotNull
    public static <T, Z> ItemStack set(
            @NotNull ItemStack item,
            @NotNull NamespacedKey key,
            @NotNull PersistentDataType<T, Z> type,
            @NotNull Z value
    ) {
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) return clone;
        meta.getPersistentDataContainer().set(key, type, value);
        clone.setItemMeta(meta);
        return clone;
    }

    @NotNull
    public static ItemStack setString(@NotNull ItemStack item, @NotNull NamespacedKey key, @NotNull String value) {
        return set(item, key, PersistentDataType.STRING, value);
    }

    @NotNull
    public static ItemStack setInt(@NotNull ItemStack item, @NotNull NamespacedKey key, int value) {
        return set(item, key, PersistentDataType.INTEGER, value);
    }

    @NotNull
    public static ItemStack setDouble(@NotNull ItemStack item, @NotNull NamespacedKey key, double value) {
        return set(item, key, PersistentDataType.DOUBLE, value);
    }

    // ── Remove ────────────────────────────────────────────────────────────────

    @NotNull
    public static ItemStack remove(@NotNull ItemStack item, @NotNull NamespacedKey key) {
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) return clone;
        meta.getPersistentDataContainer().remove(key);
        clone.setItemMeta(meta);
        return clone;
    }

    // ── Matching ──────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if both items have the same value for {@code key}.
     */
    public static <T, Z> boolean matches(
            @NotNull ItemStack a,
            @NotNull ItemStack b,
            @NotNull NamespacedKey key,
            @NotNull PersistentDataType<T, Z> type
    ) {
        Z va = get(a, key, type);
        Z vb = get(b, key, type);
        if (va == null && vb == null) return true;
        if (va == null || vb == null) return false;
        return va.equals(vb);
    }

    // ── Container-level helpers ───────────────────────────────────────────────

    @Nullable
    public static <T, Z> Z get(
            @NotNull PersistentDataContainer pdc,
            @NotNull NamespacedKey key,
            @NotNull PersistentDataType<T, Z> type
    ) {
        return pdc.get(key, type);
    }

    public static <T, Z> void set(
            @NotNull PersistentDataContainer pdc,
            @NotNull NamespacedKey key,
            @NotNull PersistentDataType<T, Z> type,
            @NotNull Z value
    ) {
        pdc.set(key, type, value);
    }
}
