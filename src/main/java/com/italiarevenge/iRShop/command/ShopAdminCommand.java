package com.italiarevenge.iRShop.command;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.api.events.ShopReloadEvent;
import com.italiarevenge.iRShop.gui.editor.ShopEditorGui;
import com.italiarevenge.iRShop.model.Shop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles {@code /shopadmin [create|edit|delete|reload]} commands.
 * All subcommands require {@code irshop.admin}.
 */
public class ShopAdminCommand implements CommandExecutor, TabCompleter {

    private final IRShop plugin;

    public ShopAdminCommand(@NotNull IRShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("irshop.admin")) {
            if (sender instanceof Player p) plugin.getMessageManager().send(p, "admin.no-permission");
            else sender.sendMessage("No permission.");
            return true;
        }

        if (args.length == 0) {
            sendAdminHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 2) {
                    sendUsage(sender, "/shopadmin create <name>");
                    yield true;
                }
                createShop(sender, args[1]);
                yield true;
            }
            case "edit" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Player only.");
                    yield true;
                }
                if (args.length < 2) {
                    sendUsage(sender, "/shopadmin edit <name>");
                    yield true;
                }
                editShop(player, args[1]);
                yield true;
            }
            case "delete" -> {
                if (args.length < 2) {
                    sendUsage(sender, "/shopadmin delete <name>");
                    yield true;
                }
                deleteShop(sender, args[1]);
                yield true;
            }
            case "reload" -> {
                reload(sender);
                yield true;
            }
            default -> {
                sendAdminHelp(sender);
                yield true;
            }
        };
    }

    private void createShop(@NotNull CommandSender sender, @NotNull String name) {
        if (plugin.getShopService().shopExists(name)) {
            if (sender instanceof Player p) {
                plugin.getMessageManager().send(p, "shop.not-found",
                        Map.of("name", name)); // reuse "already exists" or custom key
            }
            return;
        }
        plugin.getShopService().createShop(name, "<white>" + name, null, null)
                .thenAccept(shop -> {
                    if (sender instanceof Player p) {
                        plugin.getServer().getScheduler().runTask(plugin, () ->
                                plugin.getMessageManager().send(p, "admin.shop-created",
                                        Map.of("name", name)));
                    } else {
                        sender.sendMessage("Shop '" + name + "' created.");
                    }
                });
    }

    private void editShop(@NotNull Player player, @NotNull String name) {
        Shop shop = plugin.getShopService().getShopByName(name);
        if (shop == null) {
            plugin.getMessageManager().send(player, "shop.not-found", Map.of("name", name));
            return;
        }
        plugin.getCategoryService().loadCategories(shop)
                .thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () ->
                        new ShopEditorGui(plugin, player, shop).open()));
    }

    private void deleteShop(@NotNull CommandSender sender, @NotNull String name) {
        Shop shop = plugin.getShopService().getShopByName(name);
        if (shop == null) {
            if (sender instanceof Player p)
                plugin.getMessageManager().send(p, "shop.not-found", Map.of("name", name));
            return;
        }
        plugin.getShopService().deleteShop(shop.getId()).thenRun(() -> {
            if (sender instanceof Player p) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        plugin.getMessageManager().send(p, "admin.shop-deleted", Map.of("name", name)));
            } else {
                sender.sendMessage("Shop '" + name + "' deleted.");
            }
        });
    }

    private void reload(@NotNull CommandSender sender) {
        long start = System.currentTimeMillis();
        plugin.reload();
        long ms = System.currentTimeMillis() - start;
        plugin.getServer().getPluginManager().callEvent(new ShopReloadEvent(ms));
        if (sender instanceof Player p) {
            plugin.getMessageManager().send(p, "admin.reloaded", Map.of("ms", String.valueOf(ms)));
        } else {
            sender.sendMessage("IR-Shop reloaded in " + ms + "ms.");
        }
    }

    private void sendAdminHelp(@NotNull CommandSender sender) {
        sender.sendMessage("§6IR-Shop Admin Commands:");
        sender.sendMessage("§e/shopadmin create <name> §7- Create a new shop");
        sender.sendMessage("§e/shopadmin edit <name> §7- Open the shop editor");
        sender.sendMessage("§e/shopadmin delete <name> §7- Delete a shop");
        sender.sendMessage("§e/shopadmin reload §7- Reload all data");
    }

    private void sendUsage(@NotNull CommandSender sender, @NotNull String usage) {
        if (sender instanceof Player p) {
            plugin.getMessageManager().send(p, "general.invalid-usage", Map.of("usage", usage));
        } else {
            sender.sendMessage("Usage: " + usage);
        }
    }

    @Override
    @Nullable
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) return List.of("create", "edit", "delete", "reload");
        if (args.length == 2 && !args[0].equalsIgnoreCase("create") && !args[0].equalsIgnoreCase("reload")) {
            return plugin.getShopService().getAllShops().stream()
                    .map(Shop::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
