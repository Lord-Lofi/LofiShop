package dev.lofishop;

import dev.lofishop.api.economy.EconomyManager;
import dev.lofishop.commands.LofiShopCommand;
import dev.lofishop.commands.SellWandCommand;
import dev.lofishop.config.ConfigManager;
import dev.lofishop.config.MessageConfig;
import dev.lofishop.creator.ShopCreatorListener;
import dev.lofishop.creator.ShopCreatorManager;
import dev.lofishop.display.BlockShopManager;
import dev.lofishop.gui.MenuManager;
import dev.lofishop.limit.LimitManager;
import dev.lofishop.listeners.BlockShopListener;
import dev.lofishop.listeners.MenuListener;
import dev.lofishop.listeners.QuickSellListener;
import dev.lofishop.listeners.SellWandListener;
import dev.lofishop.sellwand.SellWandManager;
import dev.lofishop.shop.ShopManager;
import dev.lofishop.util.PlaceholderHook;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class LofiShop extends JavaPlugin {

    private static LofiShop instance;

    private ConfigManager configManager;
    private MessageConfig messageConfig;
    private EconomyManager economyManager;
    private ShopManager shopManager;
    private LimitManager limitManager;
    private MenuManager menuManager;
    private SellWandManager sellWandManager;
    private BlockShopManager blockShopManager;
    private ShopCreatorManager shopCreatorManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        configManager        = new ConfigManager(this);
        messageConfig        = new MessageConfig(this);

        economyManager = new EconomyManager(this);
        if (!economyManager.setup()) {
            getLogger().severe("No economy provider found! Disabling LofiShop.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        limitManager         = new LimitManager(this);
        shopManager          = new ShopManager(this);
        menuManager          = new MenuManager(this);
        sellWandManager      = new SellWandManager(this);
        blockShopManager     = new BlockShopManager(this);
        shopCreatorManager   = new ShopCreatorManager(this);

        // Commands
        PluginCommand shopCmd = getCommand("lofishop");
        if (shopCmd != null) {
            LofiShopCommand handler = new LofiShopCommand(this);
            shopCmd.setExecutor(handler);
            shopCmd.setTabCompleter(handler);
        }

        PluginCommand wandCmd = getCommand("sellwand");
        if (wandCmd != null) wandCmd.setExecutor(new SellWandCommand(this));

        // Listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new MenuListener(this), this);
        pm.registerEvents(new SellWandListener(this), this);
        pm.registerEvents(new QuickSellListener(this), this);
        pm.registerEvents(new BlockShopListener(this), this);
        pm.registerEvents(new ShopCreatorListener(this), this);

        // Re-spawn block shop display entities 1 tick after worlds load
        getServer().getScheduler().runTaskLater(this, () ->
                blockShopManager.respawnAll(), 20L);

        // PlaceholderAPI hook
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI hook registered.");
        }

        getLogger().info("LofiShop v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (limitManager != null)    limitManager.saveAll();
        if (blockShopManager != null) blockShopManager.removeAllDisplays();
        getLogger().info("LofiShop disabled.");
    }

    public void reload() {
        reloadConfig();
        configManager.reload();
        messageConfig.reload();
        shopManager.reload();
        limitManager.reload();
        getServer().getScheduler().runTaskLater(this, () ->
                blockShopManager.respawnAll(), 5L);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public static LofiShop getInstance()              { return instance; }
    public ConfigManager getConfigManager()            { return configManager; }
    public MessageConfig getMessageConfig()            { return messageConfig; }
    public EconomyManager getEconomyManager()          { return economyManager; }
    public ShopManager getShopManager()                { return shopManager; }
    public LimitManager getLimitManager()              { return limitManager; }
    public MenuManager getMenuManager()                { return menuManager; }
    public SellWandManager getSellWandManager()        { return sellWandManager; }
    public BlockShopManager getBlockShopManager()      { return blockShopManager; }
    public ShopCreatorManager getShopCreatorManager()  { return shopCreatorManager; }
}
