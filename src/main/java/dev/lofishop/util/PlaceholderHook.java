package dev.lofishop.util;

import dev.lofishop.LofiShop;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides %lofishop_<placeholder>% through PlaceholderAPI.
 *
 * Available placeholders:
 *   %lofishop_balance%              — player's default economy balance
 *   %lofishop_balance_formatted%    — formatted balance
 *   %lofishop_buy_limit_<shop>_<product>%   — remaining personal buy limit
 *   %lofishop_sell_limit_<shop>_<product>%  — remaining personal sell limit
 */
public class PlaceholderHook extends PlaceholderExpansion {

    private final LofiShop plugin;

    public PlaceholderHook(LofiShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "lofishop"; }

    @Override
    public @NotNull String getAuthor() { return "LofiShop"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        var econ = plugin.getEconomyManager().getDefault();

        if (params.equals("balance")) {
            return String.valueOf(econ.getBalance(player));
        }

        if (params.equals("balance_formatted")) {
            return econ.format(econ.getBalance(player));
        }

        // %lofishop_buy_limit_<shopId>_<productId>%
        if (params.startsWith("buy_limit_")) {
            String rest = params.substring("buy_limit_".length());
            String[] parts = rest.split("_", 2);
            if (parts.length == 2) {
                var shop = plugin.getShopManager().getShop(parts[0]);
                if (shop != null) {
                    var product = shop.getProduct(parts[1]);
                    if (product != null) {
                        int rem = plugin.getLimitManager().remainingBuys(player, parts[0], product);
                        return rem < 0 ? "∞" : String.valueOf(rem);
                    }
                }
            }
        }

        // %lofishop_sell_limit_<shopId>_<productId>%
        if (params.startsWith("sell_limit_")) {
            String rest = params.substring("sell_limit_".length());
            String[] parts = rest.split("_", 2);
            if (parts.length == 2) {
                var shop = plugin.getShopManager().getShop(parts[0]);
                if (shop != null) {
                    var product = shop.getProduct(parts[1]);
                    if (product != null) {
                        int rem = plugin.getLimitManager().remainingSells(player, parts[0], product);
                        return rem < 0 ? "∞" : String.valueOf(rem);
                    }
                }
            }
        }

        // %lofishop_server_balance%
        if (params.equals("server_balance")) {
            return plugin.getServerAccount().formattedBalance();
        }

        // %lofishop_server_received%
        if (params.equals("server_received")) {
            return plugin.getServerAccount().formattedReceived();
        }

        // %lofishop_server_paid%
        if (params.equals("server_paid")) {
            return plugin.getServerAccount().formattedPaid();
        }

        return null;
    }
}
