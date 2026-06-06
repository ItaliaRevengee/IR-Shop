package com.italiarevenge.iRShop.loader;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.model.ShopItem;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ShopLoader {

    private final IRShop plugin;
    private final Map<String, Shop> shops = new LinkedHashMap<>();

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "categories/food.yml",
            "categories/minerals.yml",
            "categories/blocks.yml",
            "categories/tools.yml",
            "categories/armor.yml",
            "categories/redstone.yml"
    );

    public ShopLoader(IRShop plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        shops.clear();
        saveDefaults();

        Map<String, ShopCategory> categoryMap = loadCategories();
        loadShops(categoryMap);
    }

    private void saveDefaults() {
        saveIfMissing("shops/default.yml");
        for (String cat : DEFAULT_CATEGORIES) saveIfMissing(cat);
    }

    private void saveIfMissing(String path) {
        File f = new File(plugin.getDataFolder(), path);
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            plugin.saveResource(path, false);
        }
    }

    private Map<String, ShopCategory> loadCategories() {
        Map<String, ShopCategory> map = new LinkedHashMap<>();
        File dir = new File(plugin.getDataFolder(), "categories");
        if (!dir.exists()) dir.mkdirs();

        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return map;

        for (File f : files) {
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

    private ShopCategory parseCategory(String id, YamlConfiguration cfg) {
        String displayName = cfg.getString("display-name", id);
        String description = cfg.getString("description", "");
        Material icon      = parseMaterial(cfg.getString("icon"), Material.CHEST);
        int slot           = cfg.getInt("slot", 10);

        List<ShopItem> items = new ArrayList<>();
        for (Map<?, ?> raw : cfg.getMapList("items")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> entry = (Map<String, Object>) raw;

            Material mat = parseMaterial((String) entry.get("material"), null);
            if (mat == null) continue;

            double buy  = toDouble(entry.getOrDefault("buy",  -1));
            double sell = toDouble(entry.getOrDefault("sell", -1));
            String name = (String) entry.getOrDefault("name", null);

            @SuppressWarnings("unchecked")
            List<String> lore = (List<String>) entry.getOrDefault("lore", Collections.emptyList());

            items.add(new ShopItem(mat, buy, sell, name, lore));
        }
        return new ShopCategory(id, displayName, description, icon, slot, items);
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

    private Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try { return Material.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return fallback; }
    }

    private double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(val.toString()); }
        catch (Exception e) { return -1; }
    }

    public Map<String, Shop> getShops() { return Collections.unmodifiableMap(shops); }
    public Shop getShop(String id)     { return shops.get(id); }
    public Shop getDefaultShop()       { return shops.isEmpty() ? null : shops.values().iterator().next(); }
}
