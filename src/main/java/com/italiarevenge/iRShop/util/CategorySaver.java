package com.italiarevenge.iRShop.util;

import com.italiarevenge.iRShop.IRShop;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

public final class CategorySaver {

    private CategorySaver() {}

    public static boolean updateItemPrice(IRShop plugin, String categoryId, int itemIndex, boolean isBuy, double price) {
        File file = categoryFile(plugin, categoryId);
        if (!file.exists()) return false;
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            List<Map<?, ?>> items = cfg.getMapList("items");
            if (itemIndex < 0 || itemIndex >= items.size()) return false;
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) items.get(itemIndex);
            item.put(isBuy ? "buy" : "sell", price);
            cfg.set("items", items);
            cfg.save(file);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("[CategorySaver] updateItemPrice failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean removeItem(IRShop plugin, String categoryId, int itemIndex) {
        File file = categoryFile(plugin, categoryId);
        if (!file.exists()) return false;
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            List<Map<?, ?>> items = cfg.getMapList("items");
            if (itemIndex < 0 || itemIndex >= items.size()) return false;
            items.remove(itemIndex);
            cfg.set("items", items);
            cfg.save(file);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("[CategorySaver] removeItem failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean appendItem(IRShop plugin, String categoryId, String yamlEntry) {
        File file = categoryFile(plugin, categoryId);
        if (!file.exists()) return false;
        try {
            Files.write(file.toPath(), yamlEntry.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("[CategorySaver] appendItem failed: " + e.getMessage());
            return false;
        }
    }

    private static File categoryFile(IRShop plugin, String categoryId) {
        return new File(plugin.getDataFolder(), "categories/" + categoryId + ".yml");
    }
}
