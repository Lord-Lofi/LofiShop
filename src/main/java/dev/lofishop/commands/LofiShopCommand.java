package dev.lofishop.commands;

import dev.lofishop.LofiShop;
import dev.lofishop.gui.QuickSellMenu;
import dev.lofishop.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * /lofishop (aliases: /shop, /ls)
 *
 * Subcommands:
 *   open <id>              — open a shop by ID
 *   list                   — list available shops
 *   reload                 — reload all configs
 *   quicksell              — open quick-sell menu
 *   give sellwand [player] — give a sell wand
 *   help                   — show help
 */
public class LofiShopCommand implements CommandExecutor, TabCompleter {

    private final LofiShop plugin;

    public LofiShopCommand(LofiShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (!player.hasPermission("lofishop.open")) {
                    plugin.getMessageConfig().send(player, "no-permission");
                    return true;
                }
                showHelp(player);
            } else {
                showConsoleHelp(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "open":         return handleOpen(sender, args);
            case "list":         return handleList(sender);
            case "reload":       return handleReload(sender);
            case "quicksell":    return handleQuickSell(sender);
            case "give":         return handleGive(sender, args);
            case "createblock":   return handleCreateBlock(sender, args);
            case "removeblock":   return handleRemoveBlock(sender);
            case "givecreator":   return handleGiveCreator(sender, args);
            case "help":          return handleHelp(sender);
            default:             return handleOpen(sender, args);
        }
    }

    private boolean handleOpen(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("lofishop.open")) {
            plugin.getMessageConfig().send(player, "no-permission");
            return true;
        }

        String shopId = args.length >= 2
                ? args[1].toLowerCase()
                : (args.length == 1 ? args[0].toLowerCase() : null);

        if (shopId == null || shopId.equals("open")) {
            showHelp(player);
            return true;
        }

        if (!player.hasPermission("lofishop.open.*")
                && !player.hasPermission("lofishop.open." + shopId)) {
            plugin.getMessageConfig().send(player, "no-permission");
            return true;
        }

        plugin.getMenuManager().openShop(player, shopId);
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(MessageUtil.parse(
                "<gold>Available shops: <white>" +
                String.join(", ", plugin.getShopManager().getShopIds())));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("lofishop.reload")) {
            if (sender instanceof Player) plugin.getMessageConfig().send((Player) sender, "no-permission");
            else sender.sendMessage("No permission.");
            return true;
        }
        try {
            plugin.reload();
            if (sender instanceof Player) {
                plugin.getMessageConfig().send((Player) sender, "reload-success");
            } else {
                sender.sendMessage("[LofiShop] Reloaded successfully.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Reload failed: " + e.getMessage());
            if (sender instanceof Player) {
                plugin.getMessageConfig().send((Player) sender, "reload-failed");
            }
        }
        return true;
    }

    private boolean handleQuickSell(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("lofishop.quicksell")) {
            plugin.getMessageConfig().send(player, "no-permission");
            return true;
        }
        new QuickSellMenu(plugin, player).open();
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lofishop.admin")) {
            if (sender instanceof Player) plugin.getMessageConfig().send((Player) sender, "no-permission");
            else sender.sendMessage("No permission.");
            return true;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("sellwand")) {
            sender.sendMessage(MessageUtil.parse("<yellow>Usage: /lofishop give sellwand [player]"));
            return true;
        }

        Player target = null;
        if (args.length >= 3) {
            target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(MessageUtil.parse("<red>Player '" + args[2] + "' not found."));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        }

        if (target == null) {
            sender.sendMessage("Specify a player.");
            return true;
        }

        target.getInventory().addItem(plugin.getSellWandManager().createWand());
        plugin.getMessageConfig().send(target, "sellwand-given", Map.of("player", target.getName()));
        return true;
    }

    private boolean handleGiveCreator(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lofishop.admin")) {
            if (sender instanceof Player) plugin.getMessageConfig().send((Player) sender, "no-permission");
            else sender.sendMessage("No permission.");
            return true;
        }

        Player target = null;
        if (args.length >= 2) {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(MessageUtil.parse("<red>Player '" + args[1] + "' not found."));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        }

        if (target == null) {
            sender.sendMessage("Usage: /shop givecreator [player]");
            return true;
        }

        target.getInventory().addItem(plugin.getShopCreatorManager().createWand());
        target.sendMessage(MessageUtil.parse(
                "<gold>[LofiShop] <gray>You received a <gold>Shop Creator Wand<gray>. " +
                "Right-click to open the creation wizard."));
        if (sender != target) {
            sender.sendMessage(MessageUtil.parse(
                    "<gold>[LofiShop] <gray>Gave Shop Creator Wand to <white>" + target.getName() + "."));
        }
        return true;
    }

    private boolean handleCreateBlock(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        if (!sender.hasPermission("lofishop.admin")) {
            plugin.getMessageConfig().send((Player) sender, "no-permission");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(MessageUtil.parse(
                    "<yellow>Usage: /shop createblock <shopId> <productKey>"));
            return true;
        }
        Player player = (Player) sender;
        String shopId   = args[1].toLowerCase();
        String productId = args[2].toLowerCase();

        org.bukkit.block.Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(MessageUtil.parse("<red>Look at a block within 5 blocks first."));
            return true;
        }

        boolean created = plugin.getBlockShopManager().create(target.getLocation(), shopId, productId);
        if (created) {
            player.sendMessage(MessageUtil.parse(
                    "<gold>[LofiShop] <green>Block shop created for <white>" +
                    shopId + "<green>.<white>" + productId));
        } else {
            player.sendMessage(MessageUtil.parse(
                    "<red>Could not create block shop — check shop ID and product key."));
        }
        return true;
    }

    private boolean handleRemoveBlock(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        if (!sender.hasPermission("lofishop.admin")) {
            plugin.getMessageConfig().send((Player) sender, "no-permission");
            return true;
        }
        Player player = (Player) sender;
        org.bukkit.block.Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(MessageUtil.parse("<red>Look at a block shop within 5 blocks first."));
            return true;
        }

        boolean removed = plugin.getBlockShopManager().remove(target.getLocation());
        if (removed) {
            player.sendMessage(MessageUtil.parse("<gold>[LofiShop] <green>Block shop removed."));
        } else {
            player.sendMessage(MessageUtil.parse("<red>No block shop found at that location."));
        }
        return true;
    }

    private boolean handleHelp(CommandSender sender) {
        if (sender instanceof Player) showHelp((Player) sender);
        else showConsoleHelp(sender);
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(MessageUtil.parse("<gold><bold>LofiShop Commands"));
        player.sendMessage(MessageUtil.parse("<yellow>/shop open <id> <gray>— Open a shop"));
        player.sendMessage(MessageUtil.parse("<yellow>/shop list <gray>— List shops"));
        player.sendMessage(MessageUtil.parse("<yellow>/shop quicksell <gray>— Quick sell menu"));
        if (player.hasPermission("lofishop.admin")) {
            player.sendMessage(MessageUtil.parse("<yellow>/shop reload <gray>— Reload configs"));
            player.sendMessage(MessageUtil.parse("<yellow>/shop give sellwand [player] <gray>— Give sell wand"));
            player.sendMessage(MessageUtil.parse("<yellow>/shop givecreator [player] <gray>— Give shop creator wand"));
            player.sendMessage(MessageUtil.parse("<yellow>/shop createblock <shopId> <product> <gray>— Create block shop (look at block)"));
            player.sendMessage(MessageUtil.parse("<yellow>/shop removeblock <gray>— Remove block shop (look at block)"));
        }
    }

    private void showConsoleHelp(CommandSender sender) {
        sender.sendMessage("[LofiShop] Commands: open, list, reload, give, quicksell, help");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("open", "list", "quicksell", "help"));
            if (sender.hasPermission("lofishop.reload")) subs.add("reload");
            if (sender.hasPermission("lofishop.admin")) {
                subs.add("give");
                subs.add("givecreator");
                subs.add("createblock");
                subs.add("removeblock");
            }
            subs.addAll(plugin.getShopManager().getShopIds());
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("open") || args[0].equalsIgnoreCase("createblock")) {
                for (String id : plugin.getShopManager().getShopIds()) {
                    if (id.startsWith(args[1].toLowerCase())) completions.add(id);
                }
            } else if (args[0].equalsIgnoreCase("give")) {
                completions.add("sellwand");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("createblock")) {
            dev.lofishop.shop.Shop shop = plugin.getShopManager().getShop(args[1].toLowerCase());
            if (shop != null) {
                for (String key : shop.getProducts().keySet()) {
                    if (key.startsWith(args[2].toLowerCase())) completions.add(key);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[2].toLowerCase()))
                    completions.add(p.getName());
            }
        }

        return completions;
    }
}
