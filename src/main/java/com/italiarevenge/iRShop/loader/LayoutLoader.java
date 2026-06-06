package com.italiarevenge.iRShop.loader;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.gui.Layout;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class LayoutLoader {

    private final IRShop plugin;
    private final Map<String, Layout> layouts = new LinkedHashMap<>();

    public LayoutLoader(IRShop plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        layouts.clear();
        saveDefaults();

        File dir = new File(plugin.getDataFolder(), "layouts");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;

        for (File f : files) {
            String id = f.getName().replace(".yml", "");
            try {
                Layout layout = parse(id, YamlConfiguration.loadConfiguration(f));
                layouts.put(id, layout);
                plugin.getLogger().info("Loaded layout: " + id);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load layout " + f.getName() + ": " + e.getMessage());
            }
        }
    }

    private void saveDefaults() {
        for (String name : List.of("layouts/classic.yml")) {
            File f = new File(plugin.getDataFolder(), name);
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                plugin.saveResource(name, false);
            }
        }
    }

    private Layout parse(String id, YamlConfiguration cfg) {
        int rows = cfg.getInt("rows", 6);

        Material bgMat     = parseMaterial(cfg.getString("background.material"), Material.GRAY_STAINED_GLASS_PANE);
        String   bgName    = cfg.getString("background.name", " ");
        Material borderMat = parseMaterial(cfg.getString("border.material"), Material.BLACK_STAINED_GLASS_PANE);
        String   borderName = cfg.getString("border.name", " ");

        Map<String, Integer> navSlots = new HashMap<>();
        for (String key : List.of("back", "prev-page", "close", "next-page", "search", "info")) {
            int slot = cfg.getInt("nav-slots." + key, -1);
            if (slot >= 0) navSlots.put(key, slot);
        }

        List<Integer> itemSlots = new ArrayList<>();
        for (int s : cfg.getIntegerList("item-slots")) itemSlots.add(s);

        if (itemSlots.isEmpty()) {
            // Fallback: inner 4×7 grid of a 6-row chest
            for (int r = 1; r <= 4; r++) {
                for (int c = 1; c <= 7; c++) {
                    itemSlots.add(r * 9 + c);
                }
            }
        }

        return new Layout(id, rows, bgMat, bgName, borderMat, borderName, navSlots, itemSlots);
    }

    private Material parseMaterial(String s, Material fallback) {
        if (s == null) return fallback;
        try { return Material.valueOf(s.toUpperCase()); } catch (Exception e) { return fallback; }
    }

    public Layout get(String id) {
        Layout l = layouts.get(id);
        return l != null ? l : layouts.getOrDefault("classic", fallbackLayout());
    }

    private Layout fallbackLayout() {
        List<Integer> slots = new ArrayList<>();
        for (int r = 1; r <= 4; r++)
            for (int c = 1; c <= 7; c++)
                slots.add(r * 9 + c);
        Map<String, Integer> nav = Map.of("back", 45, "prev-page", 48, "close", 49, "next-page", 50);
        return new Layout("fallback", 6,
                Material.GRAY_STAINED_GLASS_PANE, " ",
                Material.BLACK_STAINED_GLASS_PANE, " ",
                nav, slots);
    }

    public Map<String, Layout> getAll() { return Collections.unmodifiableMap(layouts); }
}
