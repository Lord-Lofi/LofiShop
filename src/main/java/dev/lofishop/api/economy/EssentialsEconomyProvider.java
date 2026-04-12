package dev.lofishop.api.economy;

import com.earth2me.essentials.Essentials;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;

public class EssentialsEconomyProvider implements EconomyProvider {

    private Essentials essentials;
    private final JavaPlugin plugin;

    public EssentialsEconomyProvider(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() { return "essentials"; }

    @Override
    public String getCurrencyName() { return "Coins"; }

    @Override
    public String getCurrencySymbol() {
        return plugin.getConfig().getString("general.currency-symbol", "$");
    }

    @Override
    public boolean isAvailable() {
        org.bukkit.plugin.Plugin ess = plugin.getServer().getPluginManager().getPlugin("Essentials");
        if (ess instanceof Essentials) {
            essentials = (Essentials) ess;
            return true;
        }
        return false;
    }

    @Override
    public double getBalance(Player player) {
        try {
            return essentials.getUser(player).getMoney().doubleValue();
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public boolean has(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        try {
            var user = essentials.getUser(player);
            user.setMoney(user.getMoney().subtract(BigDecimal.valueOf(amount)));
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("EssentialsX withdraw failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deposit(Player player, double amount) {
        try {
            var user = essentials.getUser(player);
            user.setMoney(user.getMoney().add(BigDecimal.valueOf(amount)));
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("EssentialsX deposit failed: " + e.getMessage());
            return false;
        }
    }
}
