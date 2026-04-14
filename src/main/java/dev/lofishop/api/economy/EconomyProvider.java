package dev.lofishop.api.economy;

import org.bukkit.entity.Player;

/**
 * Common interface for all economy backends.
 */
public interface EconomyProvider {

    /** Unique identifier used in shop configs, e.g. "vault" or "essentials". */
    String getId();

    /** Human-readable currency name. */
    String getCurrencyName();

    /** Symbol shown in menus. */
    String getCurrencySymbol();

    /** Returns true if the provider is available and ready. */
    boolean isAvailable();

    /** Returns the player's current balance. */
    double getBalance(Player player);

    /** Returns true if the player can afford the given amount. */
    boolean has(Player player, double amount);

    /** Withdraws amount from the player. Returns true on success. */
    boolean withdraw(Player player, double amount);

    /** Deposits amount to the player. Returns true on success. */
    boolean deposit(Player player, double amount);

    /** Formatted balance string (e.g. "$1,250.00"). */
    default String format(double amount) {
        return getCurrencySymbol() + String.format("%,.2f", amount);
    }

    /**
     * Deposits amount to a named account (offline/virtual player).
     * Used for the optional server-account Vault sync.
     * Returns false if the provider does not support named accounts.
     */
    default boolean deposit(String name, double amount) { return false; }

    /**
     * Withdraws amount from a named account (offline/virtual player).
     * Returns false if the provider does not support named accounts.
     */
    default boolean withdraw(String name, double amount) { return false; }
}
