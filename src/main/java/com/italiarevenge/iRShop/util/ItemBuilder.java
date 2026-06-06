package com.italiarevenge.iRShop.util;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.model.AttributeEntry;
import com.italiarevenge.iRShop.model.PdcEntry;
import com.italiarevenge.iRShop.model.ShopItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Converts a {@link ShopItem} config model into a live {@link ItemStack},
 * applying all custom metadata: name, lore, enchantments, item flags,
 * attribute modifiers, custom model data, leather RGB color, and PDC entries.
 *
 * For serialized items ({@link ShopItem#isSerialized()}), the bytes are
 * deserialized via Paper's {@code ItemStack.deserializeBytes()} so ALL NBT
 * (including tags not exposed by the Bukkit API) is preserved.
 */
public final class ItemBuilder {

    private ItemBuilder() {}

    // ── public API ──────────────────────────────────────────────────────────

    /**
     * Builds the "pure" item exactly as it should be given to a player on purchase.
     * No price lore is appended here.
     */
    public static ItemStack buildClean(ShopItem shopItem) {
        if (shopItem.isSerialized()) {
            try {
                return ItemStack.deserializeBytes(shopItem.getSerializedBytes()).clone();
            } catch (Exception e) {
                IRShop.get().getLogger().warning("Failed to deserialize item bytes, falling back to material: " + e.getMessage());
            }
        }
        return applyMeta(new ItemStack(shopItem.getMaterial()), shopItem, false);
    }

    /**
     * Builds the display item for the shop GUI.
     * Appends buy/sell price lore so the player sees prices in the tooltip.
     */
    public static ItemStack buildDisplay(ShopItem shopItem) {
        ItemStack base;
        if (shopItem.isSerialized()) {
            try {
                base = ItemStack.deserializeBytes(shopItem.getSerializedBytes()).clone();
                appendPriceLore(base, shopItem);
                return base;
            } catch (Exception e) {
                IRShop.get().getLogger().warning("Failed to deserialize item bytes for display: " + e.getMessage());
            }
        }
        return applyMeta(new ItemStack(shopItem.getMaterial()), shopItem, true);
    }

    /**
     * Clones an existing ItemStack preserving ALL NBT (wrapper around {@link ItemStack#clone()}).
     */
    public static ItemStack cloneNbt(ItemStack original) {
        return original.clone();
    }

    /**
     * Compares two ItemStacks by full NBT similarity (ignores stack amount).
     */
    public static boolean compareNbt(ItemStack a, ItemStack b) {
        return a != null && a.isSimilar(b);
    }

    /**
     * Serializes an ItemStack to bytes (full NBT).
     * Store Base64 of the result in the YAML {@code serialized} field.
     */
    public static byte[] serializeNbt(ItemStack item) {
        return item.serializeAsBytes();
    }

    // ── internal ────────────────────────────────────────────────────────────

    private static ItemStack applyMeta(ItemStack item, ShopItem shopItem, boolean appendPrices) {
        ItemMeta meta = item.getItemMeta();

        // Display name
        if (shopItem.getCustomName() != null) {
            meta.displayName(MessageManager.parse(shopItem.getCustomName()));
        } else {
            meta.displayName(MessageManager.parse("<white>" + prettify(shopItem.getMaterial())));
        }

        // Custom model data (Paper 1.21.5+ uses CustomModelDataComponent)
        if (shopItem.getCustomModelData() >= 0) {
            var cmdComp = meta.getCustomModelDataComponent();
            cmdComp.setFloats(java.util.List.of((float) shopItem.getCustomModelData()));
            meta.setCustomModelDataComponent(cmdComp);
        }

        // Item flags
        if (!shopItem.getItemFlags().isEmpty()) {
            meta.addItemFlags(shopItem.getItemFlags().toArray(new ItemFlag[0]));
        }

        // Attribute modifiers
        for (AttributeEntry ae : shopItem.getAttributeModifiers()) {
            AttributeModifier mod = new AttributeModifier(
                    NamespacedKey.minecraft(ae.attribute().getKey().getKey() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)),
                    ae.amount(),
                    ae.operation(),
                    ae.slotGroup()
            );
            meta.addAttributeModifier(ae.attribute(), mod);
        }

        // Leather color (RGB)
        if (shopItem.getLeatherColor() != null && meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(shopItem.getLeatherColor());
        }

        // PDC — write all entries
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (PdcEntry entry : shopItem.getPdcEntries()) {
            writePdc(pdc, entry);
        }

        // Lore (custom + optional price overlay)
        List<Component> lore = buildLore(shopItem, appendPrices);
        meta.lore(lore);

        item.setItemMeta(meta);

        // Enchantments — added after meta (unsafe = allow levels beyond cap)
        for (Map.Entry<Enchantment, Integer> e : shopItem.getEnchantments().entrySet()) {
            item.addUnsafeEnchantment(e.getKey(), e.getValue());
        }

        return item;
    }

    private static void appendPriceLore(ItemStack item, ShopItem shopItem) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();
        else lore = new ArrayList<>(lore);
        appendPriceLines(lore, shopItem);
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    private static List<Component> buildLore(ShopItem shopItem, boolean appendPrices) {
        List<Component> lore = new ArrayList<>();
        for (String line : shopItem.getCustomLore()) {
            lore.add(MessageManager.parse(line));
        }
        if (appendPrices) {
            appendPriceLines(lore, shopItem);
        }
        return lore;
    }

    private static void appendPriceLines(List<Component> lore, ShopItem shopItem) {
        var msg     = IRShop.get().getMessageManager();
        var economy = IRShop.get().getEconomyManager();

        if (shopItem.hasVariants()) {
            List<ShopItem> variants = shopItem.getVariants();
            double firstBuy  = variants.get(0).getBuyPrice();
            double firstSell = variants.get(0).getSellPrice();
            boolean uniformBuy  = variants.stream().allMatch(v -> v.getBuyPrice()  == firstBuy);
            boolean uniformSell = variants.stream().allMatch(v -> v.getSellPrice() == firstSell);
            boolean anyBuyable  = variants.stream().anyMatch(ShopItem::isBuyable);
            boolean anySellable = variants.stream().anyMatch(ShopItem::isSellable);

            if (anyBuyable && uniformBuy && variants.get(0).isBuyable()) {
                lore.addAll(msg.getList("gui.item-buy-price-only",
                        Placeholder.parsed("buy-price", economy.format(firstBuy)),
                        Placeholder.parsed("currency",  "money")));
            } else if (!anyBuyable) {
                lore.add(MessageManager.parse(msg.raw().getString("gui.item-not-for-sale", "<red>✗ Not for sale")));
            }
            if (anySellable && uniformSell && variants.get(0).isSellable()) {
                lore.addAll(msg.getList("gui.item-sell-price-only",
                        Placeholder.parsed("sell-price", economy.format(firstSell)),
                        Placeholder.parsed("currency",   "money")));
            }
            lore.add(MessageManager.parse(msg.raw().getString("gui.item-variants-hint", "<yellow>Click <gray>to choose a variant")));
            return;
        }

        if (shopItem.isBuyable()) {
            lore.addAll(msg.getList("gui.item-buy-lore-append",
                    Placeholder.parsed("buy-price", economy.format(shopItem.getBuyPrice())),
                    Placeholder.parsed("currency",  "money")));
        } else {
            String raw = msg.raw().getString("gui.item-not-for-sale", "<red>✗ Not for sale");
            lore.add(MessageManager.parse(raw));
        }
        if (shopItem.isSellable()) {
            lore.addAll(msg.getList("gui.item-sell-lore-append",
                    Placeholder.parsed("sell-price", economy.format(shopItem.getSellPrice())),
                    Placeholder.parsed("currency",   "money")));
        }
    }

    // ── PDC helpers ─────────────────────────────────────────────────────────

    private static void writePdc(PersistentDataContainer pdc, PdcEntry entry) {
        NamespacedKey key;
        try {
            key = new NamespacedKey(entry.namespace(), entry.key());
        } catch (Exception e) {
            IRShop.get().getLogger().warning("Invalid PDC key '" + entry.namespace() + ":" + entry.key() + "': " + e.getMessage());
            return;
        }
        try {
            switch (entry.type().toUpperCase()) {
                case "STRING"  -> pdc.set(key, PersistentDataType.STRING,  entry.rawValue());
                case "INTEGER", "INT" -> pdc.set(key, PersistentDataType.INTEGER, Integer.parseInt(entry.rawValue()));
                case "LONG"    -> pdc.set(key, PersistentDataType.LONG,    Long.parseLong(entry.rawValue()));
                case "DOUBLE"  -> pdc.set(key, PersistentDataType.DOUBLE,  Double.parseDouble(entry.rawValue()));
                case "FLOAT"   -> pdc.set(key, PersistentDataType.FLOAT,   Float.parseFloat(entry.rawValue()));
                case "BYTE"    -> pdc.set(key, PersistentDataType.BYTE,    Byte.parseByte(entry.rawValue()));
                default -> IRShop.get().getLogger().warning("Unknown PDC type '" + entry.type() + "' for key " + key);
            }
        } catch (NumberFormatException e) {
            IRShop.get().getLogger().warning("Cannot parse PDC value '" + entry.rawValue() + "' as " + entry.type());
        }
    }

    // ── misc ────────────────────────────────────────────────────────────────

    private static String prettify(Material mat) {
        String[] words = mat.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return sb.toString().trim();
    }
}
