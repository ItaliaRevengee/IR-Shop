package com.italiarevenge.iRShop.api;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.api.economy.EconomyProvider;
import com.italiarevenge.iRShop.gui.ShopCategoryGui;
import com.italiarevenge.iRShop.gui.ShopMainGui;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.model.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Public API for third-party plugins to integrate with IR-Shop.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * IRShopAPI api = IRShopAPI.get();
 * api.openShop(player, "survival");
 * api.registerEconomyProvider(myProvider);
 * }</pre>
 *
 * <p>Obtain the instance with {@link #get()}. Returns {@code null} if IR-Shop
 * is not enabled.
 */
public class IRShopAPI {

    private static IRShopAPI INSTANCE;
    private final IRShop plugin;

    private IRShopAPI(@NotNull IRShop plugin) {
        this.plugin = plugin;
    }

    /** Called by {@link IRShop#onEnable()}. */
    public static void initialize(@NotNull IRShop plugin) {
        INSTANCE = new IRShopAPI(plugin);
    }

    /** Returns the API instance, or {@code null} if IR-Shop is not loaded. */
    @Nullable
    public static IRShopAPI get() { return INSTANCE; }

    // ── GUI ───────────────────────────────────────────────────────────────────

    /** Opens the shop selector (or main shop if only one exists) for a player. */
    public void openShop(@NotNull Player player) {
        var shops = plugin.getShopService().getAllShops();
        if (shops.isEmpty()) return;
        if (shops.size() == 1) openShop(player, shops.get(0).getName());
        else plugin.getServer().getScheduler().runTask(plugin,
                () -> new ShopMainGui(plugin, player).open());
    }

    /** Opens a named shop directly for a player. */
    public void openShop(@NotNull Player player, @NotNull String shopName) {
        Shop shop = plugin.getShopService().getShopByName(shopName);
        if (shop == null) return;
        plugin.getCategoryService().loadCategories(shop)
                .thenRun(() -> {
                    for (var cat : shop.getCategories()) plugin.getItemService().loadItems(cat);
                })
                .thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () ->
                        new ShopCategoryGui(plugin, player, shop).open()));
    }

    // ── Economy ───────────────────────────────────────────────────────────────

    /**
     * Registers a custom economy provider. The provider is immediately available
     * for use in shop configurations.
     */
    public void registerEconomyProvider(@NotNull EconomyProvider provider) {
        plugin.getEconomyManager().registerProvider(provider);
    }

    @NotNull
    public Collection<EconomyProvider> getEconomyProviders() {
        return plugin.getEconomyManager().getAllProviders();
    }

    // ── Shop data ─────────────────────────────────────────────────────────────

    @NotNull
    public List<Shop> getAllShops() { return plugin.getShopService().getAllShops(); }

    @Nullable
    public Shop getShop(@NotNull String name) { return plugin.getShopService().getShopByName(name); }

    // ── Dynamic item management ───────────────────────────────────────────────

    /**
     * Adds an item to the specified category at runtime.
     * Changes are persisted to the database immediately.
     *
     * @return a future resolving to the created {@link ShopItem}
     */
    @NotNull
    public CompletableFuture<ShopItem> addItem(
            @NotNull ShopCategory category,
            @NotNull ItemStack item,
            double buyPrice,
            double sellPrice
    ) {
        return plugin.getItemService().addItem(category, item, buyPrice, sellPrice,
                category.getItems().size());
    }

    /**
     * Removes an item from its category.
     */
    @NotNull
    public CompletableFuture<Void> removeItem(@NotNull ShopItem item) {
        ShopCategory cat = plugin.getCategoryService().getCategory(item.getCategoryId());
        if (cat == null) return CompletableFuture.completedFuture(null);
        return plugin.getItemService().deleteItem(cat, item.getId());
    }

    /**
     * Updates the buy and/or sell price of an existing item.
     * Pass {@code -1} for either to keep the current value.
     */
    @NotNull
    public CompletableFuture<Void> setPrice(
            @NotNull ShopItem item,
            double buyPrice,
            double sellPrice
    ) {
        if (buyPrice >= 0) item.setBuyPrice(buyPrice);
        if (sellPrice >= 0) item.setSellPrice(sellPrice);
        return plugin.getItemService().saveItem(item);
    }

    // ── Plugin access ─────────────────────────────────────────────────────────

    @NotNull
    public IRShop getPlugin() { return plugin; }
}
