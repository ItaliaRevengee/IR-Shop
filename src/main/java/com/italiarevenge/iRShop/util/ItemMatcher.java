package com.italiarevenge.iRShop.util;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.model.PdcEntry;
import com.italiarevenge.iRShop.model.ShopItem;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Counts and removes matching items from a player's inventory.
 *
 * Matching strategy:
 * - {@link ShopItem#isSerialized()}: uses {@link ItemStack#isSimilar} against the
 *   deserialized template — full NBT comparison.
 * - No PDC entries: matches by {@link org.bukkit.Material} only.
 * - With PDC entries: matches by material AND all required PDC key-value pairs (filter by PDC).
 */
public final class ItemMatcher {

    private ItemMatcher() {}

    /** Returns true if the given ItemStack matches the ShopItem's matching rules. */
    public static boolean matchesStack(ItemStack stack, ShopItem shopItem) {
        ItemStack template = shopItem.isSerialized() ? deserializeTemplate(shopItem) : null;
        return matches(stack, shopItem, template);
    }

    /** Returns the total count of matching items in the player's inventory. */
    public static int count(Player player, ShopItem shopItem) {
        ItemStack template = shopItem.isSerialized() ? deserializeTemplate(shopItem) : null;
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (matches(stack, shopItem, template)) total += stack.getAmount();
        }
        return total;
    }

    /**
     * Removes {@code amount} matching items from the player's inventory.
     * Returns the number actually removed (may be less if the player doesn't have enough).
     */
    public static int remove(Player player, ShopItem shopItem, int amount) {
        ItemStack template = shopItem.isSerialized() ? deserializeTemplate(shopItem) : null;
        ItemStack[] contents = player.getInventory().getStorageContents();
        int remaining = amount;

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (!matches(stack, shopItem, template)) continue;

            if (stack.getAmount() <= remaining) {
                remaining -= stack.getAmount();
                contents[i] = null;
            } else {
                stack.setAmount(stack.getAmount() - remaining);
                remaining = 0;
            }
        }

        player.getInventory().setStorageContents(contents);
        return amount - remaining;
    }

    // ── internal ────────────────────────────────────────────────────────────

    private static boolean matches(ItemStack stack, ShopItem shopItem, ItemStack template) {
        if (stack == null || stack.getType().isAir()) return false;

        if (template != null) {
            // Full NBT comparison via isSimilar (ignores amount)
            return ItemBuilder.compareNbt(stack, template);
        }

        // Basic material check
        if (stack.getType() != shopItem.getMaterial()) return false;

        // PDC filter: all required entries must be present and match
        if (!shopItem.getPdcEntries().isEmpty()) {
            return pdcMatches(stack, shopItem);
        }

        return true;
    }

    private static boolean pdcMatches(ItemStack stack, ShopItem shopItem) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        for (PdcEntry entry : shopItem.getPdcEntries()) {
            NamespacedKey key;
            try {
                key = new NamespacedKey(entry.namespace(), entry.key());
            } catch (Exception e) {
                return false;
            }
            if (!pdcEntryMatches(pdc, key, entry)) return false;
        }
        return true;
    }

    private static boolean pdcEntryMatches(PersistentDataContainer pdc, NamespacedKey key, PdcEntry entry) {
        try {
            return switch (entry.type().toUpperCase()) {
                case "STRING" -> {
                    String v = pdc.get(key, PersistentDataType.STRING);
                    yield entry.rawValue().equals(v);
                }
                case "INTEGER", "INT" -> {
                    Integer v = pdc.get(key, PersistentDataType.INTEGER);
                    yield v != null && v == Integer.parseInt(entry.rawValue());
                }
                case "LONG" -> {
                    Long v = pdc.get(key, PersistentDataType.LONG);
                    yield v != null && v == Long.parseLong(entry.rawValue());
                }
                case "DOUBLE" -> {
                    Double v = pdc.get(key, PersistentDataType.DOUBLE);
                    yield v != null && v == Double.parseDouble(entry.rawValue());
                }
                case "FLOAT" -> {
                    Float v = pdc.get(key, PersistentDataType.FLOAT);
                    yield v != null && v == Float.parseFloat(entry.rawValue());
                }
                case "BYTE" -> {
                    Byte v = pdc.get(key, PersistentDataType.BYTE);
                    yield v != null && v == Byte.parseByte(entry.rawValue());
                }
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    private static ItemStack deserializeTemplate(ShopItem shopItem) {
        try {
            return ItemStack.deserializeBytes(shopItem.getSerializedBytes());
        } catch (Exception e) {
            IRShop.get().getLogger().warning("Failed to deserialize template for matching: " + e.getMessage());
            return null;
        }
    }
}
