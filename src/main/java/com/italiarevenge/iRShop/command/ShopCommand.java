package com.italiarevenge.iRShop.command;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.gui.CategoryListGui;
import com.italiarevenge.iRShop.model.Shop;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ShopCommand implements CommandExecutor, TabCompleter {

    private final IRShop plugin;
    private final MessageManager msg;

    public ShopCommand(IRShop plugin) {
        this.plugin = plugin;
        this.msg    = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg.get("general.player-only"));
            return true;
        }
        if (!player.hasPermission("irshop.use")) {
            player.sendMessage(msg.get("general.no-permission"));
            return true;
        }

        if (args.length == 0) {
            openDefault(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "open" -> {
                if (args.length < 2) { openDefault(player); return true; }
                Shop shop = plugin.getShopLoader().getShop(args[1]);
                if (shop == null) {
                    player.sendMessage(msg.get("shop.not-found",
                            Placeholder.parsed("name", args[1])));
                    return true;
                }
                openShop(player, shop);
            }
            case "list" -> {
                var shops = plugin.getShopLoader().getShops();
                if (shops.isEmpty()) { player.sendMessage(msg.get("shop.list-empty")); return true; }
                player.sendMessage(msg.getRaw("shop.list-header"));
                for (Shop s : shops.values()) {
                    player.sendMessage(msg.getRaw("shop.list-entry",
                            Placeholder.parsed("name",        s.getId()),
                            Placeholder.parsed("description", s.getDescription())));
                }
            }
            case "help" -> sendHelp(player);
            default     -> player.sendMessage(msg.get("general.unknown-subcommand"));
        }
        return true;
    }

    private void openDefault(Player player) {
        Shop shop = plugin.getShopLoader().getDefaultShop();
        if (shop == null) { player.sendMessage(msg.get("shop.list-empty")); return; }
        openShop(player, shop);
    }

    private void openShop(Player player, Shop shop) {
        player.sendMessage(msg.get("shop.opened",
                Placeholder.parsed("shop-name", shop.getId())));
        new CategoryListGui(player, shop).open();
    }

    private void sendHelp(Player player) {
        player.sendMessage(msg.getRaw("help.header"));
        player.sendMessage(msg.getRaw("help.shop"));
        player.sendMessage(msg.getRaw("help.shop-open"));
        player.sendMessage(msg.getRaw("help.shop-list"));
        player.sendMessage(msg.getRaw("help.footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("open", "list", "help");
        if (args.length == 2 && args[0].equalsIgnoreCase("open"))
            return new ArrayList<>(plugin.getShopLoader().getShops().keySet());
        return List.of();
    }
}
