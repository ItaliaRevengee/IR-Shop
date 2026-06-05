package com.italiarevenge.iRShop.util;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.logging.Logger;

/**
 * Serialises and deserialises {@link ItemStack}s to/from Base64 strings for
 * database storage.
 *
 * <p>Uses Paper's {@link ItemStack#serializeAsBytes()} which captures ALL item
 * data: material, count, display name, lore, enchantments, potion effects,
 * PDC, custom model data, NBT components, armor trims, etc.
 * This is the safest approach on Paper 1.21.1+.
 */
public final class ItemSerializer {

    private static final Logger LOG = Logger.getLogger("IR-Shop/ItemSerializer");

    private ItemSerializer() {}

    /**
     * Serialises an {@link ItemStack} to a Base64 string.
     *
     * @return Base64-encoded bytes, or {@code null} on error
     */
    @Nullable
    public static String toBase64(@NotNull ItemStack item) {
        try {
            byte[] bytes = item.serializeAsBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            LOG.warning("Failed to serialise item " + item.getType() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Deserialises a Base64 string back into an {@link ItemStack}.
     *
     * @return the restored ItemStack, or {@code null} on error
     */
    @Nullable
    public static ItemStack fromBase64(@NotNull String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            LOG.warning("Failed to deserialise item from Base64: " + e.getMessage());
            return null;
        }
    }

    /**
     * Deserialises and returns a fallback item on failure.
     * Never returns {@code null}.
     */
    @NotNull
    public static ItemStack fromBase64OrFallback(@NotNull String base64, @NotNull ItemStack fallback) {
        ItemStack result = fromBase64(base64);
        return result != null ? result : fallback;
    }
}
