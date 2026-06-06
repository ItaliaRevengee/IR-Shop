package com.italiarevenge.iRShop.command;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.gui.SearchGui;
import com.italiarevenge.iRShop.gui.ShopCategoryGui;
import com.italiarevenge.iRShop.gui.ShopMainGui;
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
 * Handles {@code /shop [open|list|search|help]} commands.
 */
public class ShopCommand implements CommandExecutor, TabCompleter {

    private final IRShop plugin;

    public ShopCommand(@NotNull IRShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().sendRaw(sender,
                    plugin.getMessageManager().getComponent("general.player-only"));
            return true;
        }

        if (!player.hasPermission("irshop.use")) {
            plugin.getMessageManager().send(player, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            openDefault(player);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "open" -> {
                if (args.length < 2) {
                    openDefault(player);
                    yield true;
                }
                openNamed(player, args[1]);
                yield true;
            }
            case "list" -> {
                listShops(player);
                yield true;
            }
            case "search" -> {
                if (args.length < 2) {
                    plugin.getMessageManager().send(player, "general.invalid-usage",
                            Map.of("usage", "/shop search <query>"));
                    yield true;
                }
                String query = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                openSearch(player, query);
                yield true;
            }
            case "help" -> {
                sendHelp(player);
                yield true;
            }
            default -> {
                // Treat first arg as shop name
                openNamed(player, args[0]);
                yield true;
            }
        };
    }

    private void openDefault(@NotNull Player player) {
        var shops = plugin.getShopService().getAllShops();
        if (shops.isEmpty()) {
            plugin.getMessageManager().send(player, "shop.list-empty");
            return;
        }
        if (shops.size() == 1) {
            openShop(player, shops.get(0));
        } else {
            new ShopMainGui(plugin, player).open();
        }
    }

    private void openNamed(@NotNull Player player, @NotNull String name) {
        Shop shop = plugin.getShopService().getShopByName(name);
        if (shop == null) {
            plugin.getMessageManager().send(player, "shop.not-found", Map.of("name", name));
            return;
        }
        openShop(player, shop);
    }

    private void openShop(@NotNull Player player, @NotNull Shop shop) {
        if (shop.getPermission() != null && !player.hasPermission(shop.getPermission())) {
            plugin.getMessageManager().send(player, "shop.no-permission");
            return;
        }
        plugin.getCategoryService().loadCategories(shop)
                .thenCompose(cats -> {
                    var futures = cats.stream()
                            .map(cat -> plugin.getItemService().loadItems(cat))
                            .toList();
                    return java.util.concurrent.CompletableFuture.allOf(
                            futures.toArray(new java.util.concurrent.CompletableFuture[0]));
                })
                .thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (shop.getCategories().isEmpty()) {
                        plugin.getMessageManager().send(player, "shop.no-categories");
                        return;
                    }
                    new ShopCategoryGui(plugin, player, shop).open();
                }));
    }

    private void openSearch(@NotNull Player player, @NotNull String query) {
        var shops = plugin.getShopService().getAllShops();
        if (shops.isEmpty()) { plugin.getMessageManager().send(player, "shop.list-empty"); return; }
        Shop shop = shops.get(0);
        new SearchGui(plugin, player, shop, query).open();
    }

    private void listShops(@NotNull Player player) {
        var shops = plugin.getShopService().getAllShops();
        if (shops.isEmpty()) { plugin.getMessageManager().send(player, "shop.list-empty"); return; }
        plugin.getMessageManager().sendRaw(player, plugin.getMessageManager().getComponent("shop.list-header"));
        for (Shop shop : shops) {
            if (shop.getPermission() == null || player.hasPermission(shop.getPermission())) {
                plugin.getMessageManager().send(player, "shop.list-entry",
                        Map.of("name", shop.getName(),
                               "description", shop.getDescription() != null ? shop.getDescription() : ""));
            }
        }
    }

    private void sendHelp(@NotNull Player player) {
        plugin.getMessageManager().sendRaw(player, plugin.getMessageManager().getComponent("help.header"));
        plugin.getMessageManager().sendRaw(player, plugin.getMessageManager().getComponent("help.shop"));
        plugin.getMessageManager().sendRaw(player, plugin.getMessageManager().getComponent("help.shop-open"));
        plugin.getMessageManager().sendRaw(player, plugin.getMessageManager().getComponent("help.shop-list"));
        plugin.getMessageManager().sendRaw(player, plugin.getMessageManager().getComponent("help.shop-search"));
        plugin.getMessageManager().sendRaw(player, plugin.getMessageManager().getComponent("help.footer"));
    }

    @Override
    @Nullable
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return List.of("open", "list", "search", "help");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            return plugin.getShopService().getAllShops().stream()
                    .map(Shop::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
