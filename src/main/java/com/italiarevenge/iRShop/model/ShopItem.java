package com.italiarevenge.iRShop.model;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable model for a shop item.
 * Build via {@link Builder}; simple items can use {@link Builder#Builder(Material, double, double)}.
 *
 * YAML format reference (all fields except material/buy/sell are optional):
 * <pre>
 * - material: DIAMOND_SWORD
 *   buy: 500.0
 *   sell: 250.0
 *   name: "<gold>Legendary Sword"
 *   lore:
 *     - "<gray>A powerful weapon"
 *   enchantments:
 *     SHARPNESS: 5
 *     FIRE_ASPECT: 2
 *   item-flags:
 *     - HIDE_ATTRIBUTES
 *   attribute-modifiers:
 *     - attribute: GENERIC_ATTACK_DAMAGE
 *       amount: 5.0
 *       operation: ADD_NUMBER
 *       slot: HAND
 *   custom-model-data: 1001
 *   leather-color:
 *     r: 255
 *     g: 100
 *     b: 50
 *   pdc:
 *     - namespace: myserver
 *       key: item_tier
 *       type: STRING
 *       value: "legendary"
 *
 * # OR — full serialized item (preserves ALL NBT):
 * - serialized: "rO0ABXNy..."    # Base64 from /shopadmin serialize
 *   buy: 5000.0
 *   sell: -1
 * </pre>
 */
public class ShopItem {

    private final Material material;
    private final double buyPrice;
    private final double sellPrice;

    // Standard display
    private final String customName;
    private final List<String> customLore;

    // Extended metadata
    private final Map<Enchantment, Integer> enchantments;
    private final Set<ItemFlag> itemFlags;
    private final List<AttributeEntry> attributeModifiers;
    private final int customModelData;     // -1 = not set
    private final Color leatherColor;      // null = not set
    private final List<PdcEntry> pdcEntries;

    // Full NBT preservation via Paper serialisation
    private final byte[] serializedBytes;  // null = build from fields above

    // Variant group: if non-empty this item is a selector that opens a sub-GUI
    private final List<ShopItem> variants;

    private ShopItem(Builder b) {
        this.material           = b.material;
        this.buyPrice           = b.buyPrice;
        this.sellPrice          = b.sellPrice;
        this.customName         = b.customName;
        this.customLore         = Collections.unmodifiableList(b.customLore);
        this.enchantments       = Collections.unmodifiableMap(b.enchantments);
        this.itemFlags          = Collections.unmodifiableSet(b.itemFlags);
        this.attributeModifiers = Collections.unmodifiableList(b.attributeModifiers);
        this.customModelData    = b.customModelData;
        this.leatherColor       = b.leatherColor;
        this.pdcEntries         = Collections.unmodifiableList(b.pdcEntries);
        this.serializedBytes    = b.serializedBytes;
        this.variants           = Collections.unmodifiableList(b.variants);
    }

    // ── accessors ───────────────────────────────────────────────────────────

    public Material getMaterial()               { return material; }
    public double getBuyPrice()                 { return buyPrice; }
    public double getSellPrice()                { return sellPrice; }
    public boolean isBuyable()                  { return buyPrice >= 0; }
    public boolean isSellable()                 { return sellPrice >= 0; }
    public String getCustomName()               { return customName; }
    public List<String> getCustomLore()         { return customLore; }
    public Map<Enchantment, Integer> getEnchantments()       { return enchantments; }
    public Set<ItemFlag> getItemFlags()                      { return itemFlags; }
    public List<AttributeEntry> getAttributeModifiers()      { return attributeModifiers; }
    public int getCustomModelData()             { return customModelData; }
    public Color getLeatherColor()              { return leatherColor; }
    public List<PdcEntry> getPdcEntries()       { return pdcEntries; }
    public byte[] getSerializedBytes()          { return serializedBytes; }
    public boolean isSerialized()               { return serializedBytes != null; }
    public List<ShopItem> getVariants()         { return variants; }
    public boolean hasVariants()                { return !variants.isEmpty(); }

    // ── builder ─────────────────────────────────────────────────────────────

    public static class Builder {

        private final Material material;
        private final double buyPrice;
        private final double sellPrice;

        private String customName = null;
        private List<String> customLore = List.of();
        private Map<Enchantment, Integer> enchantments = Map.of();
        private Set<ItemFlag> itemFlags = Set.of();
        private List<AttributeEntry> attributeModifiers = List.of();
        private int customModelData = -1;
        private Color leatherColor = null;
        private List<PdcEntry> pdcEntries = List.of();
        private byte[] serializedBytes = null;
        private List<ShopItem> variants = List.of();

        public Builder(Material material, double buyPrice, double sellPrice) {
            this.material  = material;
            this.buyPrice  = buyPrice;
            this.sellPrice = sellPrice;
        }

        public Builder customName(String v)                    { customName = v;           return this; }
        public Builder customLore(List<String> v)              { customLore = v;           return this; }
        public Builder enchantments(Map<Enchantment, Integer> v){ enchantments = v;        return this; }
        public Builder itemFlags(Set<ItemFlag> v)              { itemFlags = v;            return this; }
        public Builder attributeModifiers(List<AttributeEntry> v){ attributeModifiers = v; return this; }
        public Builder customModelData(int v)                  { customModelData = v;      return this; }
        public Builder leatherColor(Color v)                   { leatherColor = v;         return this; }
        public Builder pdcEntries(List<PdcEntry> v)            { pdcEntries = v;           return this; }
        public Builder serializedBytes(byte[] v)               { serializedBytes = v;      return this; }
        public Builder variants(List<ShopItem> v)              { variants = v;             return this; }

        public ShopItem build() { return new ShopItem(this); }
    }
}
