package com.italiarevenge.iRShop.config;

import com.italiarevenge.iRShop.IRShop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MessageManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final IRShop plugin;
    private FileConfiguration messages;

    public MessageManager(IRShop plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
        mergeDefaults(file);
    }

    /** Adds any missing keys from the bundled messages.yml without touching existing values. */
    private void mergeDefaults(File file) {
        try (InputStream in = plugin.getResource("messages.yml")) {
            if (in == null) return;
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            messages.setDefaults(defaults);
            messages.options().copyDefaults(true);
            messages.save(file);
            // Reload from disk so the in-memory view matches the saved file
            messages = YamlConfiguration.loadConfiguration(file);
            messages.setDefaults(defaults);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to merge messages.yml defaults: " + e.getMessage());
        }
    }

    private String prefix() {
        return messages.getString("prefix", "<dark_gray>[IR-Shop]</dark_gray> ");
    }

    /** Returns a component with the plugin prefix prepended. */
    public Component get(String key, TagResolver... resolvers) {
        String raw = messages.getString(key, "<red>Missing message: " + key);
        return MM.deserialize(prefix() + raw, resolvers);
    }

    /** Returns a component without the plugin prefix. */
    public Component getRaw(String key, TagResolver... resolvers) {
        String raw = messages.getString(key, "<red>Missing: " + key);
        return MM.deserialize(raw, resolvers);
    }

    /** Returns a list of components from a string list key. */
    public List<Component> getList(String key, TagResolver... resolvers) {
        List<String> lines = messages.getStringList(key);
        if (lines.isEmpty()) return Collections.emptyList();
        return lines.stream()
                .map(l -> MM.deserialize(l, resolvers))
                .collect(Collectors.toList());
    }

    public static Component parse(String miniMessage, TagResolver... resolvers) {
        return MM.deserialize(miniMessage, resolvers);
    }

    public FileConfiguration raw() { return messages; }
}
