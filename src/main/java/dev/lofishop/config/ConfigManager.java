package dev.lofishop.config;

import dev.lofishop.LofiShop;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final LofiShop plugin;

    public ConfigManager(LofiShop plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public FileConfiguration get() {
        return plugin.getConfig();
    }

    public String getString(String path, String def) {
        return plugin.getConfig().getString(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        return plugin.getConfig().getBoolean(path, def);
    }

    public int getInt(String path, int def) {
        return plugin.getConfig().getInt(path, def);
    }

    public double getDouble(String path, double def) {
        return plugin.getConfig().getDouble(path, def);
    }
}
