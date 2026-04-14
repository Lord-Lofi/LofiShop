package dev.lofishop.api.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VaultEconomyProvider implements EconomyProvider {

    private Economy economy;
    private final JavaPlugin plugin;

    public VaultEconomyProvider(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() { return "vault"; }

    @Override
    public String getCurrencyName() {
        return economy != null ? economy.currencyNamePlural() : "Money";
    }

    @Override
    public String getCurrencySymbol() {
        return plugin.getConfig().getString("general.currency-symbol", "$");
    }

    @Override
    public boolean isAvailable() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    @Override
    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    @Override
    public boolean has(Player player, double amount) {
        return economy.has(player, amount);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean deposit(Player player, double amount) {
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    @Override
    public String format(double amount) {
        return economy != null ? economy.format(amount) : getCurrencySymbol() + String.format("%,.2f", amount);
    }

    @Override
    public boolean deposit(String name, double amount) {
        if (economy == null) return false;
        return economy.depositPlayer(name, amount).transactionSuccess();
    }

    @Override
    public boolean withdraw(String name, double amount) {
        if (economy == null) return false;
        return economy.withdrawPlayer(name, amount).transactionSuccess();
    }
}
