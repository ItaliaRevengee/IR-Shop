package com.italiarevenge.iRShop;

import com.italiarevenge.iRShop.command.ShopAdminCommand;
import com.italiarevenge.iRShop.command.ShopCommand;
import com.italiarevenge.iRShop.config.ConfigManager;
import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.economy.EconomyManager;
import com.italiarevenge.iRShop.gui.GuiListener;
import com.italiarevenge.iRShop.gui.admin.AdminChatInputListener;
import com.italiarevenge.iRShop.loader.LayoutLoader;
import com.italiarevenge.iRShop.loader.ShopLoader;
import org.bukkit.plugin.java.JavaPlugin;

public final class IRShop extends JavaPlugin {

    private static IRShop instance;

    private ConfigManager  configManager;
    private MessageManager messageManager;
    private EconomyManager economyManager;
    private LayoutLoader   layoutLoader;
    private ShopLoader     shopLoader;

    @Override
    public void onEnable() {
        instance = this;

        configManager  = new ConfigManager(this);
        messageManager = new MessageManager(this);
        economyManager = new EconomyManager(this);
        layoutLoader   = new LayoutLoader(this);
        shopLoader     = new ShopLoader(this);

        if (!economyManager.setup()) {
            getLogger().warning("Vault not found or no economy plugin loaded — buy/sell will be disabled.");
        }

        layoutLoader.loadAll();
        shopLoader.loadAll();

        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new AdminChatInputListener(), this);

        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("shop").setTabCompleter(new ShopCommand(this));
        getCommand("shopadmin").setExecutor(new ShopAdminCommand(this));
        getCommand("shopadmin").setTabCompleter(new ShopAdminCommand(this));

        getLogger().info("IR-Shop enabled — " + shopLoader.getShops().size() + " shop(s) loaded.");
    }

    @Override
    public void onDisable() {
        getLogger().info("IR-Shop disabled.");
    }

    /** Hot-reload configs, layouts, and shops without restarting. */
    public void reload() {
        configManager.reload();
        messageManager.reload();
        layoutLoader.loadAll();
        shopLoader.loadAll();
    }

    public static IRShop get()             { return instance; }
    public ConfigManager  getConfigManager()  { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public LayoutLoader   getLayoutLoader()   { return layoutLoader; }
    public ShopLoader     getShopLoader()     { return shopLoader; }
}
