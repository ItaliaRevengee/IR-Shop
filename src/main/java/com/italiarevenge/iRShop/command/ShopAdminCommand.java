package com.italiarevenge.iRShop.command;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.gui.admin.AdminShopListGui;
import com.italiarevenge.iRShop.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class ShopAdminCommand implements CommandExecutor, TabCompleter {

    private final IRShop plugin;
    private final MessageManager msg;

    public ShopAdminCommand(IRShop plugin) {
        this.plugin = plugin;
        this.msg    = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("irshop.admin")) {
            sender.sendMessage(msg.get("general.no-permission"));
            return true;
        }

        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                long start = System.currentTimeMillis();
                plugin.reload();
                long ms = System.currentTimeMillis() - start;
                sender.sendMessage(msg.get("admin.reloaded",
                        Placeholder.parsed("ms", String.valueOf(ms))));
            }
            case "gui" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(msg.get("general.player-only"));
                    return true;
                }
                new AdminShopListGui(player).open();
            }
            case "serialize" -> handleSerialize(sender);
            case "additem"   -> handleAddItem(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    /**
     * Serializes the item in the player's main hand to Base64 and prints it
     * as a clickable chat message so it can be pasted into a YAML config under
     * the {@code serialized:} field.
     */
    private void handleSerialize(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg.get("general.player-only"));
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            player.sendMessage(msg.get("general.no-permission")); // reuse generic error
            player.sendMessage(MessageManager.parse("<red>Hold an item in your main hand first."));
            return;
        }

        byte[] bytes  = ItemBuilder.serializeNbt(held);
        String base64 = Base64.getEncoder().encodeToString(bytes);

        player.sendMessage(MessageManager.parse(
                "<gold>Serialized item (<gray>" + held.getType().name() + "<gold>):"));
        // Send as clickable text so the admin can click to copy
        Component serializedText = Component.text(base64, NamedTextColor.AQUA)
                .clickEvent(ClickEvent.copyToClipboard(base64));
        player.sendMessage(serializedText);
        player.sendMessage(MessageManager.parse(
                "<gray>Click the text above to copy. Paste it under <white>serialized:</white> in your category YAML."));
        player.sendMessage(MessageManager.parse(
                "<gray>Example:"));
        player.sendMessage(MessageManager.parse(
                "  <white>- serialized: \"" + base64.substring(0, Math.min(20, base64.length())) + "...\""));
        player.sendMessage(MessageManager.parse(
                "  <white>  buy: 100.0"));
        player.sendMessage(MessageManager.parse(
                "  <white>  sell: 50.0"));
    }

    /**
     * /shopadmin additem <category> <buy> <sell>
     * Appends the held item to the given category YAML file, then reloads.
     * Plain items (no custom name/lore/enchants/CMD) use the "material:" format;
     * everything else uses "serialized:" so all NBT is preserved.
     */
    private void handleAddItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg.get("general.player-only"));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(MessageManager.parse(
                    "<red>Uso: /shopadmin additem <categoria> <prezzo-acquisto> <prezzo-vendita>"));
            return;
        }

        String categoryId = args[1];
        double buyPrice, sellPrice;
        try {
            buyPrice  = Double.parseDouble(args[2]);
            sellPrice = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageManager.parse("<red>Prezzi non validi."));
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            sender.sendMessage(MessageManager.parse("<red>Tieni un oggetto in mano."));
            return;
        }

        File categoryFile = new File(plugin.getDataFolder(), "categories/" + categoryId + ".yml");
        if (!categoryFile.exists()) {
            sender.sendMessage(MessageManager.parse(
                    "<red>Categoria '<white>" + categoryId + "<red>' non trovata."));
            return;
        }

        String entry;
        if (isPlainItem(held)) {
            entry = "  - material: " + held.getType().name() + "\n"
                  + "    buy: "      + buyPrice              + "\n"
                  + "    sell: "     + sellPrice             + "\n";
        } else {
            String b64 = Base64.getEncoder().encodeToString(ItemBuilder.serializeNbt(held));
            entry = "  - serialized: \"" + b64 + "\"\n"
                  + "    buy: "          + buyPrice          + "\n"
                  + "    sell: "         + sellPrice         + "\n";
        }

        try {
            Files.write(categoryFile.toPath(), entry.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            sender.sendMessage(MessageManager.parse(
                    "<red>Errore scrittura file: " + e.getMessage()));
            return;
        }

        plugin.reload();
        sender.sendMessage(MessageManager.parse(
                "<green>Oggetto <white>" + held.getType().name()
                + " <green>aggiunto alla categoria <white>" + categoryId + "<green>."));
    }

    private boolean isPlainItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;
        return !meta.hasDisplayName()
            && !meta.hasLore()
            && !meta.hasEnchants()
            && !meta.hasCustomModelData()
            && !(meta instanceof LeatherArmorMeta);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg.getRaw("help.header"));
        sender.sendMessage(MessageManager.parse(
                "  <yellow>/shopadmin gui</yellow> <dark_gray>—</dark_gray> <gray>Apre la GUI di amministrazione degli shop"));
        sender.sendMessage(msg.getRaw("help.admin-reload"));
        sender.sendMessage(MessageManager.parse(
                "  <yellow>/shopadmin serialize</yellow> <dark_gray>—</dark_gray> <gray>Copy Base64 of held item (for custom NBT shop items)"));
        sender.sendMessage(MessageManager.parse(
                "  <yellow>/shopadmin additem <categoria> <buy> <sell></yellow> <dark_gray>—</dark_gray> <gray>Aggiunge l'oggetto in mano alla categoria"));
        sender.sendMessage(msg.getRaw("help.footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("gui", "reload", "serialize", "additem");
        if (args.length == 2 && args[0].equalsIgnoreCase("additem")) {
            File categoriesDir = new File(plugin.getDataFolder(), "categories");
            File[] files = categoriesDir.listFiles((d, n) -> n.endsWith(".yml"));
            if (files == null) return List.of();
            return Arrays.stream(files)
                    .map(f -> f.getName().replace(".yml", ""))
                    .filter(n -> n.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("additem"))
            return List.of("10.0", "50.0", "100.0", "500.0");
        if (args.length == 4 && args[0].equalsIgnoreCase("additem"))
            return List.of("-1", "5.0", "25.0", "50.0");
        return List.of();
    }
}
