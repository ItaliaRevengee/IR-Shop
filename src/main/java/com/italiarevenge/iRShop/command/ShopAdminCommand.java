package com.italiarevenge.iRShop.command;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.MessageManager;
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

import java.util.Base64;
import java.util.List;

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
            case "serialize" -> handleSerialize(sender);
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

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg.getRaw("help.header"));
        sender.sendMessage(msg.getRaw("help.admin-reload"));
        sender.sendMessage(MessageManager.parse(
                "  <yellow>/shopadmin serialize</yellow> <dark_gray>—</dark_gray> <gray>Copy Base64 of held item (for custom NBT shop items)"));
        sender.sendMessage(msg.getRaw("help.footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("reload", "serialize");
        return List.of();
    }
}
