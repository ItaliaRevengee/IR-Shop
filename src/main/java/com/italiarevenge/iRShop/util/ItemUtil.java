package com.italiarevenge.iRShop.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder and helpers for creating GUI/shop display items.
 */
public final class ItemUtil {

    private ItemUtil() {}

    // ── Quick builder ─────────────────────────────────────────────────────────

    /**
     * Creates a display item with a MiniMessage name and optional lore lines.
     */
    @NotNull
    public static ItemStack build(
            @NotNull Material material,
            @Nullable String mmName,
            @Nullable List<String> mmLore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (mmName != null) meta.displayName(TextUtil.parse(mmName));
        if (mmLore != null && !mmLore.isEmpty()) meta.lore(TextUtil.parseList(mmLore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
        return item;
    }

    @NotNull
    public static ItemStack build(@NotNull Material material, @Nullable String mmName, String... mmLore) {
        return build(material, mmName, mmLore.length == 0 ? null : Arrays.asList(mmLore));
    }

    /**
     * Builds an item applying {@code nameReplacements} as MiniMessage placeholder substitution
     * on the name. No lore added. Useful for nav buttons like "Page {current}/{total}".
     */
    @NotNull
    public static ItemStack build(
            @NotNull Material material,
            @Nullable String mmName,
            @NotNull Map<String, String> nameReplacements
    ) {
        String resolved = mmName;
        if (resolved != null) {
            for (var e : nameReplacements.entrySet()) {
                resolved = resolved.replace("<" + e.getKey() + ">", e.getValue());
            }
        }
        return build(material, resolved, (List<String>) null);
    }

    /** Glass pane used as a GUI filler — no name, no lore. */
    @NotNull
    public static ItemStack filler(@NotNull Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ── Mutation helpers ──────────────────────────────────────────────────────

    /**
     * Clones {@code item} and appends {@code extraLore} at the end.
     * Existing lore is preserved.
     */
    @NotNull
    public static ItemStack appendLore(@NotNull ItemStack item, @NotNull List<Component> extraLore) {
        if (extraLore.isEmpty()) return item.clone();
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) return clone;

        List<Component> existing = meta.lore();
        java.util.List<Component> combined = new java.util.ArrayList<>();
        if (existing != null) combined.addAll(existing);
        combined.addAll(extraLore);
        meta.lore(combined);
        clone.setItemMeta(meta);
        return clone;
    }

    /**
     * Adds a glow effect (hidden enchant + HIDE_ENCHANTS flag) to a cloned item.
     */
    @NotNull
    public static ItemStack addGlow(@NotNull ItemStack item) {
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) return clone;
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        clone.setItemMeta(meta);
        return clone;
    }

    /**
     * Clones an item and changes its display name.
     */
    @NotNull
    public static ItemStack rename(@NotNull ItemStack item, @NotNull String mmName) {
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(mmName));
            clone.setItemMeta(meta);
        }
        return clone;
    }

    /**
     * Replaces placeholders in lore and name via simple string substitution.
     * Returns a fully independent clone.
     */
    @NotNull
    public static ItemStack withPlaceholders(
            @NotNull ItemStack item,
            @NotNull Map<String, String> replacements
    ) {
        if (replacements.isEmpty()) return item.clone();
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) return clone;

        // Name
        if (meta.hasDisplayName() && meta.displayName() != null) {
            String serialised = TextUtil.serialize(meta.displayName());
            meta.displayName(TextUtil.parse(serialised, replacements));
        }

        // Lore
        if (meta.hasLore() && meta.lore() != null) {
            List<Component> newLore = meta.lore().stream()
                    .map(line -> TextUtil.parse(TextUtil.serialize(line), replacements))
                    .toList();
            meta.lore(newLore);
        }

        clone.setItemMeta(meta);
        return clone;
    }

    // ── Matching ──────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if {@code a} and {@code b} have the same material,
     * display name and item type (ignores stack size and PDC).
     */
    public static boolean isSimilar(@NotNull ItemStack a, @NotNull ItemStack b) {
        return a.isSimilar(b);
    }
}
