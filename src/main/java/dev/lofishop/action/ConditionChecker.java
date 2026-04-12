package dev.lofishop.action;

import dev.lofishop.LofiShop;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Evaluates condition strings from shop product configs.
 *
 * Supported formats:
 *   [permission] node.name          — Player must have this permission
 *   [money] 500                     — Player must have at least this balance
 *   [level] 10                      — Player must be at least this XP level
 *   [placeholder] %placeholder% == value  — PlaceholderAPI comparison
 */
public class ConditionChecker {

    private final LofiShop plugin;

    public ConditionChecker(LofiShop plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns true only if ALL conditions are met.
     */
    public boolean check(List<String> conditions, Player player) {
        if (conditions == null || conditions.isEmpty()) return true;
        for (String condition : conditions) {
            if (!checkOne(condition.trim(), player)) return false;
        }
        return true;
    }

    private boolean checkOne(String condition, Player player) {
        if (condition.startsWith("[permission]")) {
            String perm = condition.substring("[permission]".length()).trim();
            return player.hasPermission(perm);

        } else if (condition.startsWith("[money]")) {
            String amtStr = condition.substring("[money]".length()).trim();
            try {
                double needed = Double.parseDouble(amtStr);
                return plugin.getEconomyManager().getDefault().has(player, needed);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid [money] condition value: " + amtStr);
                return false;
            }

        } else if (condition.startsWith("[level]")) {
            String lvlStr = condition.substring("[level]".length()).trim();
            try {
                int needed = Integer.parseInt(lvlStr);
                return player.getLevel() >= needed;
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid [level] condition value: " + lvlStr);
                return false;
            }

        } else if (condition.startsWith("[placeholder]")) {
            return checkPlaceholder(condition.substring("[placeholder]".length()).trim(), player);

        } else {
            plugin.getLogger().warning("Unknown condition: " + condition);
            return true; // unknown conditions pass by default
        }
    }

    /**
     * Evaluates a PlaceholderAPI expression like:
     *   %player_health% >= 10
     *   %vault_balance% == 500
     */
    private boolean checkPlaceholder(String expr, Player player) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return true;
        }

        // Split on operator
        String[] ops = {"==", "!=", ">=", "<=", ">", "<"};
        for (String op : ops) {
            int idx = expr.indexOf(op);
            if (idx < 0) continue;

            String placeholder = expr.substring(0, idx).trim();
            String expected = expr.substring(idx + op.length()).trim();

            String resolved = me.clip.placeholderapi.PlaceholderAPI
                    .setPlaceholders(player, placeholder);

            return compare(resolved, op, expected);
        }

        plugin.getLogger().warning("Could not parse placeholder condition: " + expr);
        return true;
    }

    private boolean compare(String actual, String op, String expected) {
        try {
            double a = Double.parseDouble(actual);
            double b = Double.parseDouble(expected);
            return switch (op) {
                case "==" -> a == b;
                case "!=" -> a != b;
                case ">=" -> a >= b;
                case "<=" -> a <= b;
                case ">"  -> a > b;
                case "<"  -> a < b;
                default   -> false;
            };
        } catch (NumberFormatException e) {
            // String comparison
            return switch (op) {
                case "==" -> actual.equalsIgnoreCase(expected);
                case "!=" -> !actual.equalsIgnoreCase(expected);
                default   -> false;
            };
        }
    }
}
