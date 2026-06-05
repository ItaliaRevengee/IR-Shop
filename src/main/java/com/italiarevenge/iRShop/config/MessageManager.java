package com.italiarevenge.iRShop.config;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads and provides all player-facing messages from {@code messages.yml}.
 *
 * <p>Messages are in MiniMessage format. Use {@link #send(Player, String)} for
 * simple messages and {@link #send(Player, String, Map)} for messages with
 * placeholder substitution.
 */
public class MessageManager {

    private final IRShop plugin;
    private final Logger log;
    private FileConfiguration messages;
    private String prefix;

    public MessageManager(@NotNull IRShop plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        load();
    }

    private void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(file);
        prefix = messages.getString("prefix", "<dark_gray>[IR-Shop]</dark_gray> ");
    }

    public void reload() { load(); }

    // ── Send helpers ──────────────────────────────────────────────────────────

    /** Sends a prefixed message to the player. */
    public void send(@NotNull Player player, @NotNull String key) {
        String raw = get(key);
        if (raw == null) return;
        player.sendMessage(TextUtil.parse(prefix + raw));
    }

    /** Sends a prefixed message with placeholder substitution. */
    public void send(@NotNull Player player, @NotNull String key, @NotNull Map<String, String> placeholders) {
        String raw = get(key);
        if (raw == null) return;
        player.sendMessage(TextUtil.parse(prefix + raw, placeholders));
    }

    /**
     * Sends a raw component without the prefix.
     * Accepts any {@link org.bukkit.command.CommandSender} — Paper's CommandSender
     * implements {@link net.kyori.adventure.audience.Audience}.
     */
    public void sendRaw(@NotNull org.bukkit.command.CommandSender sender, @NotNull Component component) {
        sender.sendMessage(component);
    }

    // ── Component builders ────────────────────────────────────────────────────

    @NotNull
    public Component get(@NotNull String key, @NotNull Map<String, String> placeholders) {
        String raw = get(key);
        if (raw == null) return Component.empty();
        return TextUtil.parse(raw, placeholders);
    }

    @NotNull
    public Component getComponent(@NotNull String key) {
        String raw = get(key);
        if (raw == null) return Component.empty();
        return TextUtil.parse(raw);
    }

    /** Returns the raw MiniMessage string for a message key, or {@code null}. */
    @Nullable
    public String get(@NotNull String key) {
        String val = messages.getString(key);
        if (val == null) {
            if (plugin.getConfigManager().isDebug()) {
                log.warning("Missing message key: " + key);
            }
        }
        return val;
    }

    @NotNull
    public List<String> getList(@NotNull String key) {
        return messages.getStringList(key);
    }

    @NotNull
    public String getPrefix() { return prefix; }

    @NotNull
    public Component getPrefixComponent() { return TextUtil.parse(prefix); }
}
