package com.italiarevenge.iRShop.command;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.MessageManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

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

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            long start = System.currentTimeMillis();
            plugin.reload();
            long ms = System.currentTimeMillis() - start;
            sender.sendMessage(msg.get("admin.reloaded",
                    Placeholder.parsed("ms", String.valueOf(ms))));
        } else {
            sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg.getRaw("help.header"));
        sender.sendMessage(msg.getRaw("help.admin-reload"));
        sender.sendMessage(msg.getRaw("help.footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("reload");
        return List.of();
    }
}
