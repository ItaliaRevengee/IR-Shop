package com.italiarevenge.iRShop.layout;

import com.italiarevenge.iRShop.IRShop;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * Loads {@link Layout} definitions from {@code layouts/*.yml} in the plugin data folder.
 * Falls back to bundled defaults when a file is absent.
 */
public class LayoutManager {

    private static final String[] BUNDLED = {"classic", "modern", "dark", "rpg"};

    private final IRShop plugin;
    private final Logger log;
    private final Map<String, Layout> layouts = new LinkedHashMap<>();

    public LayoutManager(@NotNull IRShop plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void loadLayouts() {
        layouts.clear();

        // Save bundled defaults if not present
        for (String name : BUNDLED) {
            File f = new File(plugin.getDataFolder(), "layouts/" + name + ".yml");
            if (!f.exists()) plugin.saveResource("layouts/" + name + ".yml", false);
        }

        // Load all YAML files from the layouts directory
        File dir = new File(plugin.getDataFolder(), "layouts");
        if (!dir.exists() || !dir.isDirectory()) {
            log.warning("No layouts directory found. Using built-in defaults.");
            loadDefaults();
            return;
        }

        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null || files.length == 0) {
            loadDefaults();
            return;
        }

        for (File file : files) {
            try {
                Layout layout = parseLayout(file);
                if (layout != null) {
                    layouts.put(layout.getId(), layout);
                    log.info("Layout loaded: " + layout.getId());
                }
            } catch (Exception e) {
                log.warning("Failed to load layout " + file.getName() + ": " + e.getMessage());
            }
        }

        if (layouts.isEmpty()) loadDefaults();
        log.info("Loaded " + layouts.size() + " layout(s).");
    }

    @Nullable
    private Layout parseLayout(@NotNull File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String id = file.getName().replace(".yml", "").toLowerCase();
        String displayName = cfg.getString("display-name", id);
        int rows = cfg.getInt("rows", 6);

        Material bg = parseMaterial(cfg.getString("background.material"), Material.GRAY_STAINED_GLASS_PANE);
        Material border = parseMaterial(cfg.getString("border.material"), Material.BLACK_STAINED_GLASS_PANE);

        ConfigurationSection navSection = cfg.getConfigurationSection("nav-slots");
        Map<String, Integer> navSlots = new HashMap<>();
        if (navSection != null) {
            for (String key : navSection.getKeys(false)) {
                navSlots.put(key, navSection.getInt(key));
            }
        }

        List<Integer> itemSlots = new ArrayList<>();
        for (int slot : cfg.getIntegerList("item-slots")) {
            if (slot >= 0 && slot < rows * 9) itemSlots.add(slot);
        }

        if (itemSlots.isEmpty()) return null;
        return new Layout(id, displayName, rows, bg, border, navSlots, itemSlots);
    }

    @NotNull
    private Material parseMaterial(@Nullable String name, @NotNull Material def) {
        if (name == null) return def;
        try { return Material.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return def; }
    }

    /** Loads a minimal hardcoded classic layout as last resort. */
    private void loadDefaults() {
        Map<String, Integer> nav = Map.of(
                "back", 45, "prev-page", 48, "close", 49, "next-page", 50, "search", 47);
        List<Integer> items = List.of(
                10,11,12,13,14,15,16,
                19,20,21,22,23,24,25,
                28,29,30,31,32,33,34,
                37,38,39,40,41,42,43);
        layouts.put("classic", new Layout("classic", "Classic", 6,
                Material.GRAY_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE, nav, items));
        log.info("Loaded built-in classic layout fallback.");
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    @Nullable
    public Layout getLayout(@NotNull String id) { return layouts.get(id.toLowerCase()); }

    @NotNull
    public Layout getLayoutOrDefault(@NotNull String id) {
        Layout l = layouts.get(id.toLowerCase());
        if (l != null) return l;
        l = layouts.get(plugin.getConfigManager().getDefaultLayout());
        if (l != null) return l;
        return layouts.values().iterator().next(); // guaranteed to have at least one
    }

    @NotNull
    public Collection<Layout> getAllLayouts() { return Collections.unmodifiableCollection(layouts.values()); }

    public boolean hasLayout(@NotNull String id) { return layouts.containsKey(id.toLowerCase()); }
}
