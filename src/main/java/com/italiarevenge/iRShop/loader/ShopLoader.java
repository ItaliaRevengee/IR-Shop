package com.italiarevenge.iRShop.loader;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.model.AttributeEntry;
import com.italiarevenge.iRShop.model.PdcEntry;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.model.ShopItem;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

/**
 * Loads shops, categories, and items from YAML files in the plugin data folder.
 *
 * Extended item format (all fields except material/buy/sell are optional):
 * <pre>
 * items:
 *   - material: DIAMOND_SWORD
 *     buy: 500.0
 *     sell: 250.0
 *     name: "<gold>Legendary Sword"
 *     lore:
 *       - "<gray>Forged in legend"
 *     enchantments:
 *       SHARPNESS: 5
 *       FIRE_ASPECT: 2
 *     item-flags:
 *       - HIDE_ATTRIBUTES
 *     attribute-modifiers:
 *       - attribute: GENERIC_ATTACK_DAMAGE
 *         amount: 5.0
 *         operation: ADD_NUMBER
 *         slot: HAND
 *     custom-model-data: 1001
 *     leather-color:
 *       r: 255
 *       g: 100
 *       b: 50
 *     pdc:
 *       - namespace: myserver
 *         key: item_tier
 *         type: STRING
 *         value: "legendary"
 *
 *   # Full NBT item (use /shopadmin serialize while holding it):
 *   - serialized: "rO0ABXUA..."
 *     buy: 5000.0
 *     sell: -1
 * </pre>
 */
public class ShopLoader {

    private final IRShop plugin;
    private final Map<String, Shop> shops = new LinkedHashMap<>();

    public ShopLoader(IRShop plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        shops.clear();
        saveDefaults();
        Map<String, ShopCategory> categoryMap = loadCategories();
        loadShops(categoryMap);
    }

    // ── defaults ────────────────────────────────────────────────────────────

    private void saveDefaults() {
        saveIfMissing("shops/default.yml");
        List<String> enabledCategories = getEnabledCategories();
        for (String cat : enabledCategories) saveIfMissing("categories/" + cat);
    }

    /**
     * Reads the list of enabled categories from config.yml
     * @return list of category filenames (without "categories/" prefix)
     */
    private List<String> getEnabledCategories() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.getLogger().warning("config.yml not found, using empty category list");
            return List.of();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        List<String> categories = config.getStringList("categories.enabled");
        if (categories.isEmpty()) {
            plugin.getLogger().warning("No categories found in config.yml 'categories.enabled'");
        }
        return categories;
    }

    private void saveIfMissing(String path) {
        File f = new File(plugin.getDataFolder(), path);
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            try {
                plugin.saveResource(path, false);
            } catch (IllegalArgumentException e) {
                // Resource doesn't exist in JAR, skip silently
                plugin.getLogger().warning("Resource '" + path + "' not found in JAR, skipping");
            }
        }
    }

    // ── category loading ─────────────────────────────────────────────────────

    private Map<String, ShopCategory> loadCategories() {
        Map<String, ShopCategory> map = new LinkedHashMap<>();
        File dir = new File(plugin.getDataFolder(), "categories");
        if (!dir.exists()) dir.mkdirs();

        // Load only enabled categories from config
        List<String> enabledCategories = getEnabledCategories();
        
        for (String catFile : enabledCategories) {
            File f = new File(dir, catFile.endsWith(".yml") ? catFile : catFile + ".yml");
            if (!f.exists()) {
                plugin.getLogger().warning("Category file not found: " + f.getName());
                continue;
            }
            
            String id = f.getName().replace(".yml", "");
            try {
                ShopCategory cat = parseCategory(id, YamlConfiguration.loadConfiguration(f));
                if (cat != null) {
                    map.put(id, cat);
                    plugin.getLogger().info("Loaded category: " + id + " (" + cat.getItems().size() + " items)");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load category " + f.getName() + ": " + e.getMessage());
            }
        }
        return map;
    }

    private ShopCategory parseCategory(String id, YamlConfiguration cfg) {
        String displayName = cfg.getString("display-name", id);
        String description = cfg.getString("description", "");
        Material icon      = parseMaterial(cfg.getString("icon"), Material.CHEST);
        int slot           = cfg.getInt("slot", 10);

        List<ShopItem> items = new ArrayList<>();
        for (Map<?, ?> raw : cfg.getMapList("items")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> entry = (Map<String, Object>) raw;
            ShopItem item = parseItem(entry);
            if (item != null) items.add(item);
        }
        return new ShopCategory(id, displayName, description, icon, slot, items);
    }

    // ── shop loading ──────────────────────────────────────────────────────────

    private void loadShops(Map<String, ShopCategory> categoryMap) {
        File dir = new File(plugin.getDataFolder(), "shops");
        if (!dir.exists()) dir.mkdirs();

        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;

        for (File f : files) {
            String id = f.getName().replace(".yml", "");
            try {
                Shop shop = parseShop(id, YamlConfiguration.loadConfiguration(f), categoryMap);
                if (shop != null) {
                    shops.put(shop.getId(), shop);
                    plugin.getLogger().info("Loaded shop: " + shop.getId()
                            + " (" + shop.getCategories().size() + " categories)");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load shop " + f.getName() + ": " + e.getMessage());
            }
        }
    }

    private Shop parseShop(String id, YamlConfiguration cfg, Map<String, ShopCategory> categoryMap) {
        String displayName = cfg.getString("display-name", cfg.getString("name", id));
        String description = cfg.getString("description", "");
        Material icon      = parseMaterial(cfg.getString("icon"), Material.CHEST);
        String currency    = cfg.getString("currency", "vault");
        String layout      = cfg.getString("layout", "classic");

        List<ShopCategory> categories = new ArrayList<>();
        for (String catId : cfg.getStringList("categories")) {
            ShopCategory cat = categoryMap.get(catId);
            if (cat != null) {
                categories.add(cat);
            } else {
                plugin.getLogger().warning("Shop '" + id + "' references unknown category: " + catId);
            }
        }
        return new Shop(id, displayName, description, icon, currency, layout, categories);
    }

    // ── item parsing ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private ShopItem parseItem(Map<String, Object> e) {
        double buy  = toDouble(e.getOrDefault("buy",  -1));
        double sell = toDouble(e.getOrDefault("sell", -1));

        // ── Serialized (full NBT) item ──
        String serializedB64 = (String) e.get("serialized");
        if (serializedB64 != null) {
            try {
                byte[] bytes = Base64.getDecoder().decode(serializedB64);
                // Validate by attempting decode; keep bytes for actual build
                ItemStack.deserializeBytes(bytes);
                Material mat = Material.PAPER; // placeholder — real item comes from bytes
                return new ShopItem.Builder(mat, buy, sell)
                        .serializedBytes(bytes)
                        .build();
            } catch (Exception ex) {
                plugin.getLogger().warning("Invalid 'serialized' field: " + ex.getMessage());
                return null;
            }
        }

        // ── Standard item ──
        Material mat = parseMaterial((String) e.get("material"), null);
        if (mat == null) return null;

        ShopItem.Builder builder = new ShopItem.Builder(mat, buy, sell)
                .customName((String) e.get("name"))
                .customLore((List<String>) e.getOrDefault("lore", List.of()))
                .enchantments(parseEnchantments(e))
                .itemFlags(parseItemFlags(e))
                .attributeModifiers(parseAttributeModifiers(e))
                .customModelData(toInt(e.getOrDefault("custom-model-data", -1)))
                .leatherColor(parseLeatherColor(e))
                .pdcEntries(parsePdc(e))
                .variants(parseVariants(e, buy, sell));

        return builder.build();
    }

    // ── field parsers ─────────────────────────────────────────────────────────

    private Map<Enchantment, Integer> parseEnchantments(Map<String, Object> e) {
        Object raw = e.get("enchantments");
        if (!(raw instanceof Map<?, ?> map)) return Map.of();

        Map<Enchantment, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String name = entry.getKey().toString().toLowerCase();
            Enchantment ench = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT).get(NamespacedKey.minecraft(name));
            if (ench == null) {
                plugin.getLogger().warning("Unknown enchantment: " + name);
                continue;
            }
            result.put(ench, toInt(entry.getValue()));
        }
        return result;
    }

    private Set<ItemFlag> parseItemFlags(Map<String, Object> e) {
        Object raw = e.get("item-flags");
        if (!(raw instanceof List<?> list)) return Set.of();

        Set<ItemFlag> flags = new LinkedHashSet<>();
        for (Object o : list) {
            try {
                flags.add(ItemFlag.valueOf(o.toString().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Unknown item flag: " + o);
            }
        }
        return flags;
    }

    @SuppressWarnings("unchecked")
    private List<AttributeEntry> parseAttributeModifiers(Map<String, Object> e) {
        Object raw = e.get("attribute-modifiers");
        if (!(raw instanceof List<?> list)) return List.of();

        List<AttributeEntry> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) continue;
            Map<String, Object> map = (Map<String, Object>) m;

            String attrName = String.valueOf(map.getOrDefault("attribute", "")).toUpperCase();
            Attribute attribute = parseAttribute(attrName);
            if (attribute == null) {
                plugin.getLogger().warning("Unknown attribute: " + attrName);
                continue;
            }

            double amount = toDouble(map.getOrDefault("amount", 0.0));

            AttributeModifier.Operation op;
            try {
                op = AttributeModifier.Operation.valueOf(
                        String.valueOf(map.getOrDefault("operation", "ADD_NUMBER")).toUpperCase());
            } catch (IllegalArgumentException ex) {
                op = AttributeModifier.Operation.ADD_NUMBER;
            }

            EquipmentSlotGroup slot = parseSlotGroup(String.valueOf(map.getOrDefault("slot", "ANY")));
            result.add(new AttributeEntry(attribute, amount, op, slot));
        }
        return result;
    }

    private Color parseLeatherColor(Map<String, Object> e) {
        Object raw = e.get("leather-color");
        if (!(raw instanceof Map<?, ?> rawMap)) return null;
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) rawMap;
        try {
            int r = toInt(map.getOrDefault("r", 0));
            int g = toInt(map.getOrDefault("g", 0));
            int b = toInt(map.getOrDefault("b", 0));
            return Color.fromRGB(
                    Math.max(0, Math.min(255, r)),
                    Math.max(0, Math.min(255, g)),
                    Math.max(0, Math.min(255, b)));
        } catch (Exception ex) {
            plugin.getLogger().warning("Invalid leather-color: " + ex.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<PdcEntry> parsePdc(Map<String, Object> e) {
        Object raw = e.get("pdc");
        if (!(raw instanceof List<?> list)) return List.of();

        List<PdcEntry> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) continue;
            Map<String, Object> map = (Map<String, Object>) m;
            String ns    = String.valueOf(map.getOrDefault("namespace", "irshop"));
            String key   = String.valueOf(map.getOrDefault("key", ""));
            String type  = String.valueOf(map.getOrDefault("type", "STRING")).toUpperCase();
            String value = String.valueOf(map.getOrDefault("value", ""));
            if (key.isEmpty()) continue;
            result.add(new PdcEntry(ns, key, type, value));
        }
        return result;
    }

    private List<ShopItem> parseVariants(Map<String, Object> e, double parentBuy, double parentSell) {
        Object raw = e.get("variants");
        if (!(raw instanceof List<?> list) || list.isEmpty()) return List.of();

        List<ShopItem> result = new ArrayList<>();
        for (Object entry : list) {
            if (entry instanceof String matName) {
                // Simple string → material only, inherit parent buy/sell
                Material varMat = parseMaterial(matName, null);
                if (varMat != null) result.add(new ShopItem.Builder(varMat, parentBuy, parentSell).build());
            } else if (entry instanceof Map<?, ?> varMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> varEntry = new HashMap<>((Map<String, Object>) varMap);
                // Inherit parent buy/sell if not overridden
                varEntry.putIfAbsent("buy",  parentBuy);
                varEntry.putIfAbsent("sell", parentSell);
                ShopItem varItem = parseItem(varEntry);
                if (varItem != null) result.add(varItem);
            }
        }
        return result;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Attribute parseAttribute(String name) {
        // Paper 1.21.3+ uses registry keys like "generic.attack_damage"
        // instead of the old enum-style "GENERIC_ATTACK_DAMAGE".
        // Convert by replacing only the first underscore with a dot.
        String lower = name.toLowerCase();
        int sep = lower.indexOf('_');
        String key = sep >= 0 ? lower.substring(0, sep) + '.' + lower.substring(sep + 1) : lower;
        Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(key));
        if (attr == null) attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(lower));
        return attr;
    }

    private EquipmentSlotGroup parseSlotGroup(String name) {
        return switch (name.toUpperCase()) {
            case "HAND"      -> EquipmentSlotGroup.HAND;
            case "OFF_HAND", "OFFHAND" -> EquipmentSlot.OFF_HAND.getGroup();
            case "HEAD"      -> EquipmentSlotGroup.HEAD;
            case "CHEST"     -> EquipmentSlotGroup.CHEST;
            case "LEGS"      -> EquipmentSlotGroup.LEGS;
            case "FEET"      -> EquipmentSlotGroup.FEET;
            case "ARMOR"     -> EquipmentSlotGroup.ARMOR;
            default          -> EquipmentSlotGroup.ANY;
        };
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try { return Material.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return fallback; }
    }

    private double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return -1; }
    }

    private int toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return -1; }
    }

    // ── public accessors ─────────────────────────────────────────────────────

    public Map<String, Shop> getShops()  { return Collections.unmodifiableMap(shops); }
    public Shop getShop(String id)        { return shops.get(id); }
    public Shop getDefaultShop()          { return shops.isEmpty() ? null : shops.values().iterator().next(); }
}
