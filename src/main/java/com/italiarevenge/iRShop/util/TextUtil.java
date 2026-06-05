package com.italiarevenge.iRShop.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility for MiniMessage-based text formatting throughout IR-Shop.
 *
 * <p>All player-facing text uses Adventure {@link Component}s via MiniMessage.
 * Legacy {@code &}-codes are not supported — use MiniMessage tags in configs.
 */
public final class TextUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private TextUtil() {}

    // ── Parsing ───────────────────────────────────────────────────────────────

    /**
     * Parses a MiniMessage string into a {@link Component}.
     * Returns {@link Component#empty()} for {@code null} input.
     */
    @NotNull
    public static Component parse(@Nullable String miniMessage) {
        if (miniMessage == null || miniMessage.isEmpty()) return Component.empty();
        return MM.deserialize(miniMessage);
    }

    /**
     * Parses a MiniMessage string, substituting {@code {key}} placeholders
     * from {@code replacements}.
     *
     * <p>Example: {@code parse("<green>Hello <player>!", Map.of("player", "Steve"))}
     */
    @NotNull
    public static Component parse(@Nullable String miniMessage, @NotNull Map<String, String> replacements) {
        if (miniMessage == null || miniMessage.isEmpty()) return Component.empty();
        String processed = miniMessage;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            processed = processed.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return MM.deserialize(processed);
    }

    /**
     * Parses with Adventure {@link TagResolver}s — use for dynamic components.
     */
    @NotNull
    public static Component parse(@Nullable String miniMessage, @NotNull TagResolver... resolvers) {
        if (miniMessage == null || miniMessage.isEmpty()) return Component.empty();
        return MM.deserialize(miniMessage, resolvers);
    }

    /** Converts a list of MiniMessage strings to a list of {@link Component}s. */
    @NotNull
    public static List<Component> parseList(@Nullable List<String> lines) {
        if (lines == null || lines.isEmpty()) return List.of();
        List<Component> result = new ArrayList<>(lines.size());
        for (String line : lines) result.add(parse(line));
        return result;
    }

    /** Parses a list of MiniMessage strings with placeholder substitution. */
    @NotNull
    public static List<Component> parseList(@Nullable List<String> lines, @NotNull Map<String, String> replacements) {
        if (lines == null || lines.isEmpty()) return List.of();
        List<Component> result = new ArrayList<>(lines.size());
        for (String line : lines) result.add(parse(line, replacements));
        return result;
    }

    // ── Serialisation ─────────────────────────────────────────────────────────

    /** Strips all formatting and returns plain text. */
    @NotNull
    public static String plain(@NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** Serialises a Component back to MiniMessage string. */
    @NotNull
    public static String serialize(@NotNull Component component) {
        return MM.serialize(component);
    }

    // ── Convenience tag builders ──────────────────────────────────────────────

    /** Creates a string {@link Placeholder} resolver. */
    @NotNull
    public static TagResolver placeholder(@NotNull String key, @NotNull String value) {
        return Placeholder.parsed(key, value);
    }

    /** Creates a Component {@link Placeholder} resolver. */
    @NotNull
    public static TagResolver placeholder(@NotNull String key, @NotNull Component value) {
        return Placeholder.component(key, value);
    }

    // ── Number formatting ─────────────────────────────────────────────────────

    /** Formats a price to 2 decimal places, removing ".00" for round numbers. */
    @NotNull
    public static String formatPrice(double price) {
        if (price == Math.floor(price) && !Double.isInfinite(price)) {
            return String.valueOf((long) price);
        }
        return String.format("%.2f", price);
    }
}
