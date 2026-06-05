package com.italiarevenge.iRShop;

import com.italiarevenge.iRShop.api.IRShopAPI;
import com.italiarevenge.iRShop.api.economy.EconomyManager;
import com.italiarevenge.iRShop.command.ShopAdminCommand;
import com.italiarevenge.iRShop.command.ShopCommand;
import com.italiarevenge.iRShop.config.ConfigManager;
import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.database.DatabaseManager;
import com.italiarevenge.iRShop.gui.framework.GuiManager;
import com.italiarevenge.iRShop.layout.LayoutManager;
import com.italiarevenge.iRShop.listener.InventoryListener;
import com.italiarevenge.iRShop.listener.PlayerListener;
import com.italiarevenge.iRShop.placeholder.IRShopExpansion;
import com.italiarevenge.iRShop.service.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * IR-Shop — main plugin class.
 *
 * <p>Initialisation order (critical — services depend on each other):
 * <ol>
 *   <li>Configs (ConfigManager, MessageManager)</li>
 *   <li>Database (DatabaseManager)</li>
 *   <li>Economy (EconomyManager)</li>
 *   <li>Layouts (LayoutManager)</li>
 *   <li>Services (shop, category, item, discount, transaction, player, statistics)</li>
 *   <li>GUI manager</li>
 *   <li>Public API</li>
 *   <li>Commands &amp; Listeners</li>
 *   <li>PlaceholderAPI (soft-dep, optional)</li>
 * </ol>
 */
public final class IRShop extends JavaPlugin {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static IRShop instance;

    @NotNull
    public static IRShop getInstance() {
        if (instance == null) throw new IllegalStateException("IRShop not yet initialised");
        return instance;
    }

    // ── Managers ──────────────────────────────────────────────────────────────

    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private LayoutManager layoutManager;
    private GuiManager guiManager;

    // ── Services ──────────────────────────────────────────────────────────────

    private ShopService shopService;
    private CategoryService categoryService;
    private ItemService itemService;
    private TransactionService transactionService;
    private StatisticsService statisticsService;
    private PlayerService playerService;
    private DiscountService discountService;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        instance = this;
        long start = System.currentTimeMillis();

        // 1 — Configs
        saveDefaultConfig();
        configManager  = new ConfigManager(this);
        messageManager = new MessageManager(this);

        // 2 — Database
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // 3 — Economy
        economyManager = new EconomyManager(this);
        economyManager.initialize();

        // 4 — Layouts
        layoutManager = new LayoutManager(this);
        layoutManager.loadLayouts();

        // 5 — Services (order matters: shop → category → item → rest)
        discountService    = new DiscountService(this);
        shopService        = new ShopService(this);
        categoryService    = new CategoryService(this);
        itemService        = new ItemService(this);
        transactionService = new TransactionService(this);
        playerService      = new PlayerService(this);
        statisticsService  = new StatisticsService(this);

        // 6 — GUI
        guiManager = new GuiManager(this);

        // 7 — Public API
        IRShopAPI.initialize(this);

        // 8 — Commands
        registerCommands();

        // 9 — Listeners
        registerListeners();

        // 10 — PlaceholderAPI (soft-dep)
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new IRShopExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        long ms = System.currentTimeMillis() - start;
        getLogger().info("IR-Shop v" + getPluginMeta().getVersion() + " enabled in " + ms + "ms.");
    }

    @Override
    public void onDisable() {
        if (guiManager != null)    guiManager.closeAll();
        if (playerService != null) playerService.flushAll();
        if (databaseManager != null) databaseManager.shutdown();
        instance = null;
        getLogger().info("IR-Shop disabled.");
    }

    // ── Commands & Listeners ──────────────────────────────────────────────────

    private void registerCommands() {
        ShopCommand shopCmd = new ShopCommand(this);
        var shopCommand = getCommand("shop");
        if (shopCommand != null) {
            shopCommand.setExecutor(shopCmd);
            shopCommand.setTabCompleter(shopCmd);
        }

        ShopAdminCommand adminCmd = new ShopAdminCommand(this);
        var adminCommand = getCommand("shopadmin");
        if (adminCommand != null) {
            adminCommand.setExecutor(adminCmd);
            adminCommand.setTabCompleter(adminCmd);
        }
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new InventoryListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
    }

    // ── Reload ────────────────────────────────────────────────────────────────

    /**
     * Hot-reloads all configuration and data without restarting the server.
     * Called by {@code /shopadmin reload} and {@link IRShopAPI}.
     */
    public void reload() {
        reloadConfig();
        configManager.reload();
        messageManager.reload();
        economyManager.reload();
        layoutManager.loadLayouts();
        shopService.reload();
        categoryService.reload();
        itemService.reload();
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    @NotNull public ConfigManager getConfigManager()           { return configManager; }
    @NotNull public MessageManager getMessageManager()         { return messageManager; }
    @NotNull public DatabaseManager getDatabaseManager()       { return databaseManager; }
    @NotNull public EconomyManager getEconomyManager()         { return economyManager; }
    @NotNull public LayoutManager getLayoutManager()           { return layoutManager; }
    @NotNull public GuiManager getGuiManager()                 { return guiManager; }
    @NotNull public ShopService getShopService()               { return shopService; }
    @NotNull public CategoryService getCategoryService()       { return categoryService; }
    @NotNull public ItemService getItemService()               { return itemService; }
    @NotNull public TransactionService getTransactionService() { return transactionService; }
    @NotNull public StatisticsService getStatisticsService()   { return statisticsService; }
    @NotNull public PlayerService getPlayerService()           { return playerService; }
    @NotNull public DiscountService getDiscountService()       { return discountService; }
}
