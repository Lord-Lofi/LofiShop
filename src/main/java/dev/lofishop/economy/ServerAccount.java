package dev.lofishop.economy;

import dev.lofishop.LofiShop;
import dev.lofishop.api.economy.EconomyProvider;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Tracks money flowing through admin shops as a server treasury.
 *
 * When a player buys from an admin shop, the purchase price is credited here.
 * When a player sells to an admin shop, the sale price is debited here.
 *
 * Stats tracked:
 *   total-received — cumulative money spent by players buying (all time)
 *   total-paid     — cumulative money paid to players selling (all time)
 *   balance        — total-received minus total-paid (net position)
 *
 * If server-account.vault-sync-name is set in config.yml, LofiShop will also
 * deposit/withdraw from that Vault player account so the balance shows up in
 * your economy plugin (e.g. /eco see Server in EssentialsX).
 */
public class ServerAccount {

    private final LofiShop plugin;
    private final File dataFile;

    private double balance       = 0.0;
    private double totalReceived = 0.0;
    private double totalPaid     = 0.0;

    public ServerAccount(LofiShop plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "server-account.yml");
        load();
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    /**
     * Called when a player spends money buying from an admin shop.
     * Credits the server account.
     */
    public void credit(double amount) {
        if (!isEnabled() || amount <= 0) return;
        balance       += amount;
        totalReceived += amount;
        saveLazy();
        syncVaultDeposit(amount);
    }

    /**
     * Called when a player earns money selling to an admin shop.
     * Debits the server account.
     */
    public void debit(double amount) {
        if (!isEnabled() || amount <= 0) return;
        balance    -= amount;
        totalPaid  += amount;
        saveLazy();
        syncVaultWithdraw(amount);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public double getBalance()       { return balance; }
    public double getTotalReceived() { return totalReceived; }
    public double getTotalPaid()     { return totalPaid; }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("server-account.enabled", true);
    }

    public String getDisplayName() {
        return plugin.getConfig().getString("server-account.name", "Server");
    }

    /** Formatted balance string using the default economy's currency symbol. */
    public String formattedBalance() {
        return plugin.getEconomyManager().getDefault().format(balance);
    }

    public String formattedReceived() {
        return plugin.getEconomyManager().getDefault().format(totalReceived);
    }

    public String formattedPaid() {
        return plugin.getEconomyManager().getDefault().format(totalPaid);
    }

    // ── Vault sync (optional) ─────────────────────────────────────────────────

    private void syncVaultDeposit(double amount) {
        String vaultName = vaultSyncName();
        if (vaultName == null) return;
        try {
            EconomyProvider econ = plugin.getEconomyManager().getDefault();
            econ.deposit(vaultName, amount);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "[ServerAccount] Vault sync deposit failed: " + e.getMessage());
        }
    }

    private void syncVaultWithdraw(double amount) {
        String vaultName = vaultSyncName();
        if (vaultName == null) return;
        try {
            EconomyProvider econ = plugin.getEconomyManager().getDefault();
            econ.withdraw(vaultName, amount);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "[ServerAccount] Vault sync withdraw failed: " + e.getMessage());
        }
    }

    private String vaultSyncName() {
        String name = plugin.getConfig().getString("server-account.vault-sync-name", "");
        return (name == null || name.isBlank()) ? null : name;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        balance       = cfg.getDouble("balance",        0.0);
        totalReceived = cfg.getDouble("total-received", 0.0);
        totalPaid     = cfg.getDouble("total-paid",     0.0);
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("balance",        balance);
        cfg.set("total-received", totalReceived);
        cfg.set("total-paid",     totalPaid);
        try {
            dataFile.getParentFile().mkdirs();
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING,
                    "[ServerAccount] Failed to save server-account.yml: " + e.getMessage());
        }
    }

    /** Async save — safe to call on every transaction. */
    private void saveLazy() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
    }
}
