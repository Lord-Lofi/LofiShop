package dev.lofishop.commands;

import dev.lofishop.LofiShop;
import dev.lofishop.display.BlockShopMode;
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
 *   open <id> [player]         — open a shop; [player] allowed from console or admin
 *   list                       — list available shops
 *   reload                     — reload all configs
 *   quicksell                  — open quick-sell menu
 *   give sellwand [player]     — give a sell wand
 *   givecreator [player]       — give the shop creator wand
 *   createblock <shopId> <productKey> [FULL|SMALL|QUICK]
 *   removeblock                — remove block shop (look at block)
 *   help                       — show help
 *
 * Permission split for open:
 *   lofishop.open.command — required to open via /shop open (players)
 *   lofishop.open         — required to open via block/NPC interaction
 *   Console and lofishop.admin senders bypass the command permission and
 *   may specify a target player: /shop open <id> <playerName>
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
                if (!player.hasPermission("lofishop.use")) {
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
            case "open":           return handleOpen(sender, args);
            case "list":           return handleList(sender);
            case "reload":         return handleReload(sender);
            case "quicksell":      return handleQuickSell(sender);
            case "give":           return handleGive(sender, args);
            case "edit":           return handleEdit(sender, args);
            case "createblock":    return handleCreateBlock(sender, args);
            case "removeblock":    return handleRemoveBlock(sender);
            case "givecreator":    return handleGiveCreator(sender, args);
            case "serverbalance":  return handleServerBalance(sender);
            case "help":           return handleHelp(sender);
            default:               return handleOpen(sender, args);
        }
    }

    // ── /shop open <id> [player] ──────────────────────────────────────────────

    private boolean handleOpen(CommandSender sender, String[] args) {
        String shopId = args.length >= 2
                ? args[1].toLowerCase()
                : (args.length == 1 && !args[0].equalsIgnoreCase("open") ? args[0].toLowerCase() : null);

        if (shopId == null || shopId.equals("open")) {
            if (sender instanceof Player) showHelp((Player) sender);
            else showConsoleHelp(sender);
            return true;
        }

        boolean isConsole = !(sender instanceof Player);
        boolean isAdmin   = sender.hasPermission("lofishop.admin");

        // Determine target player
        Player target = null;

        if (args.length >= 3 && (isConsole || isAdmin)) {
            // Console or admin specified a target player name
            target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(MessageUtil.parse(
                        "<red>[LofiShop] Player '<white>" + args[2] + "<red>' is not online."));
                return true;
            }
        } else if (!isConsole) {
            // Regular player opening for themselves
            Player player = (Player) sender;

            if (!player.hasPermission("lofishop.open.command")) {
                plugin.getMessageConfig().send(player, "no-permission");
                return true;
            }
            if (!player.hasPermission("lofishop.open.*")
                    && !player.hasPermission("lofishop.open." + shopId)) {
                plugin.getMessageConfig().send(player, "no-permission");
                return true;
            }
            target = player;
        } else {
            sender.sendMessage("[LofiShop] Usage: /shop open <id> <player>");
            return true;
        }

        // Open the shop for the resolved target
        // When opened by console/admin on behalf of a player, skip per-shop permission check
        if (isConsole || (isAdmin && target != sender)) {
            dev.lofishop.shop.Shop shop = plugin.getShopManager().getShop(shopId);
            if (shop == null) {
                sender.sendMessage(MessageUtil.parse(
                        "<red>[LofiShop] Shop '<white>" + shopId + "<red>' not found."));
                return true;
            }
            new dev.lofishop.gui.ShopMenu(plugin, shop).open(target);
        } else {
            plugin.getMenuManager().openShop(target, shopId);
        }
        return true;
    }

    // ── /shop list ────────────────────────────────────────────────────────────

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(MessageUtil.parse(
                "<gold>Available shops: <white>" +
                String.join(", ", plugin.getShopManager().getShopIds())));
        return true;
    }

    // ── /shop reload ──────────────────────────────────────────────────────────

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

    // ── /shop quicksell ───────────────────────────────────────────────────────

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

    // ── /shop give sellwand [player] ──────────────────────────────────────────

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lofishop.give.sellwand")) {
            if (sender instanceof Player) plugin.getMessageConfig().send((Player) sender, "no-permission");
            else sender.sendMessage("No permission.");
            return true;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("sellwand")) {
            sender.sendMessage(MessageUtil.parse("<yellow>Usage: /lofishop give sellwand [player]"));
            return true;
        }

        Player target = resolveTarget(sender, args, 2);
        if (target == null) return true;

        target.getInventory().addItem(plugin.getSellWandManager().createWand());
        plugin.getMessageConfig().send(target, "sellwand-given", Map.of("player", target.getName()));
        if (sender != target) {
            sender.sendMessage(MessageUtil.parse(
                    "<gold>[LofiShop] <gray>Gave sell wand to <white>" + target.getName() + "."));
        }
        return true;
    }

    // ── /shop givecreator [player] ────────────────────────────────────────────

    private boolean handleGiveCreator(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lofishop.give.creator")) {
            if (sender instanceof Player) plugin.getMessageConfig().send((Player) sender, "no-permission");
            else sender.sendMessage("No permission.");
            return true;
        }

        Player target = resolveTarget(sender, args, 1);
        if (target == null) return true;

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

    // ── /shop edit <shopId> ───────────────────────────────────────────────────

    private boolean handleEdit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        if (!sender.hasPermission("lofishop.admin")) {
            plugin.getMessageConfig().send((Player) sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.parse("<yellow>Usage: /shop edit <shopId>"));
            return true;
        }

        Player player = (Player) sender;
        String shopId = args[1].toLowerCase();
        dev.lofishop.shop.Shop shop = plugin.getShopManager().getShop(shopId);
        if (shop == null) {
            player.sendMessage(MessageUtil.parse(
                    "<red>[LofiShop] Shop '<white>" + shopId + "<red>' not found."));
            return true;
        }

        plugin.getShopCreatorManager().openEditor(player, shop);
        return true;
    }

    // ── /shop createblock <shopId> <productKey> [FULL|SMALL|QUICK] ───────────

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
                    "<yellow>Usage: /shop createblock <shopId> <productKey> [FULL|SMALL|QUICK]"));
            return true;
        }

        Player player  = (Player) sender;
        String shopId  = args[1].toLowerCase();
        String prodId  = args[2].toLowerCase();
        BlockShopMode mode = args.length >= 4
                ? BlockShopMode.fromString(args[3])
                : BlockShopMode.FULL;

        dev.lofishop.shop.Shop shop = plugin.getShopManager().getShop(shopId);
        if (shop == null) {
            player.sendMessage(MessageUtil.parse(
                    "<red>[LofiShop] Shop '<white>" + shopId + "<red>' not found. Use /shop list to see available shops."));
            return true;
        }
        if (shop.getProduct(prodId) == null) {
            player.sendMessage(MessageUtil.parse(
                    "<red>[LofiShop] Product '<white>" + prodId + "<red>' not found in shop '<white>" + shopId + "<red>'."));
            return true;
        }

        org.bukkit.block.Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(MessageUtil.parse("<red>Look at a block within 5 blocks first."));
            return true;
        }

        boolean created = plugin.getBlockShopManager().create(
                target.getLocation(), shopId, prodId, mode);
        if (created) {
            player.sendMessage(MessageUtil.parse(
                    "<gold>[LofiShop] <green>Block shop created — <white>" +
                    shopId + "." + prodId +
                    " <dark_gray>[" + mode.name() + "]"));
        } else {
            player.sendMessage(MessageUtil.parse(
                    "<red>Could not create block shop at that location."));
        }
        return true;
    }

    // ── /shop serverbalance ───────────────────────────────────────────────────

    private boolean handleServerBalance(CommandSender sender) {
        if (!sender.hasPermission("lofishop.admin")) {
            if (sender instanceof Player) plugin.getMessageConfig().send((Player) sender, "no-permission");
            else sender.sendMessage("No permission.");
            return true;
        }

        dev.lofishop.economy.ServerAccount acc = plugin.getServerAccount();
        String name = acc.getDisplayName();

        sender.sendMessage(MessageUtil.parse(
                "<gold><bold>LofiShop — " + name + " Account"));
        sender.sendMessage(MessageUtil.parse(
                "<yellow>Balance:      <white>" + acc.formattedBalance()));
        sender.sendMessage(MessageUtil.parse(
                "<green>Total in:     <white>" + acc.formattedReceived()
                + " <dark_gray>(players buying from admin shops)"));
        sender.sendMessage(MessageUtil.parse(
                "<red>Total out:    <white>" + acc.formattedPaid()
                + " <dark_gray>(players selling to admin shops)"));

        if (!plugin.getConfig().getString("server-account.vault-sync-name", "").isBlank()) {
            sender.sendMessage(MessageUtil.parse(
                    "<dark_gray>Vault sync:   <gray>"
                    + plugin.getConfig().getString("server-account.vault-sync-name")));
        }
        return true;
    }

    // ── /shop removeblock ─────────────────────────────────────────────────────

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

    // ── Help ──────────────────────────────────────────────────────────────────

    private boolean handleHelp(CommandSender sender) {
        if (sender instanceof Player) showHelp((Player) sender);
        else showConsoleHelp(sender);
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(MessageUtil.parse("<gold><bold>LofiShop Commands"));
        if (player.hasPermission("lofishop.open.command")) {
            player.sendMessage(MessageUtil.parse("<yellow>/shop open <id> <gray>— Open a shop"));
        }
        player.sendMessage(MessageUtil.parse("<yellow>/shop list <gray>— List shops"));
        player.sendMessage(MessageUtil.parse("<yellow>/shop quicksell <gray>— Quick sell menu"));
        if (player.hasPermission("lofishop.admin")) {
            player.sendMessage(MessageUtil.parse("<yellow>/shop reload <gray>— Reload configs"));
            player.sendMessage(MessageUtil.parse("<yellow>/shop give sellwand [player] <gray>— Give sell wand"));
            player.sendMessage(MessageUtil.parse("<yellow>/shop givecreator [player] <gray>— Give shop creator wand"));
            player.sendMessage(MessageUtil.parse("<yellow>/shop edit <shopId> <gray>— Edit an existing shop"));
            player.sendMessage(MessageUtil.parse(
                    "<yellow>/shop createblock <shopId> <product> [FULL|SMALL|QUICK] <gray>— Create block shop"));
            player.sendMessage(MessageUtil.parse("<yellow>/shop removeblock <gray>— Remove block shop"));
            player.sendMessage(MessageUtil.parse(
                    "<yellow>/shop open <id> <player> <gray>— Open shop for another player"));
            player.sendMessage(MessageUtil.parse(
                    "<yellow>/shop serverbalance <gray>— View server treasury stats"));
        }
    }

    private void showConsoleHelp(CommandSender sender) {
        sender.sendMessage("[LofiShop] Console commands:");
        sender.sendMessage("  open <id> <player>  — open a shop for a player");
        sender.sendMessage("  list                — list all shops");
        sender.sendMessage("  reload              — reload configs");
        sender.sendMessage("  give sellwand <player>");
        sender.sendMessage("  givecreator <player>");
        sender.sendMessage("  createblock <shopId> <productKey> [FULL|SMALL|QUICK]  (player only)");
        sender.sendMessage("  serverbalance  — view server treasury stats");
    }

    // ── Tab completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        boolean isConsole = !(sender instanceof Player);
        boolean isAdmin   = sender.hasPermission("lofishop.admin");

        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            subs.add("list");
            subs.add("quicksell");
            subs.add("help");
            if (sender.hasPermission("lofishop.open.command") || isConsole || isAdmin)
                subs.add("open");
            if (sender.hasPermission("lofishop.reload"))
                subs.add("reload");
            if (isAdmin) {
                subs.add("edit");
                subs.add("give");
                subs.add("givecreator");
                subs.add("createblock");
                subs.add("removeblock");
                subs.add("serverbalance");
            }
            // Also complete shop IDs directly
            subs.addAll(plugin.getShopManager().getShopIds());
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            }

        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("open") || sub.equals("createblock") || sub.equals("edit")) {
                for (String id : plugin.getShopManager().getShopIds()) {
                    if (id.startsWith(args[1].toLowerCase())) completions.add(id);
                }
            } else if (sub.equals("give")) {
                completions.add("sellwand");
            } else if (sub.equals("givecreator")) {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase()))
                        completions.add(p.getName());
                }
            }

        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("createblock")) {
                dev.lofishop.shop.Shop shop = plugin.getShopManager().getShop(args[1].toLowerCase());
                if (shop != null) {
                    for (String key : shop.getProducts().keySet()) {
                        if (key.startsWith(args[2].toLowerCase())) completions.add(key);
                    }
                }
            } else if (sub.equals("give")) {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[2].toLowerCase()))
                        completions.add(p.getName());
                }
            } else if (sub.equals("open") && (isConsole || isAdmin)) {
                // Target player name for console/admin open
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[2].toLowerCase()))
                        completions.add(p.getName());
                }
            }

        } else if (args.length == 4 && args[0].equalsIgnoreCase("createblock")) {
            // Mode argument
            for (BlockShopMode mode : BlockShopMode.values()) {
                if (mode.name().startsWith(args[3].toUpperCase()))
                    completions.add(mode.name());
            }
        }

        return completions;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Resolves a target player from args[argIndex], falling back to the sender
     * if they are a player. Sends an error and returns null if unresolvable.
     */
    private Player resolveTarget(CommandSender sender, String[] args, int argIndex) {
        if (args.length > argIndex) {
            Player target = plugin.getServer().getPlayer(args[argIndex]);
            if (target == null) {
                sender.sendMessage(MessageUtil.parse(
                        "<red>[LofiShop] Player '<white>" + args[argIndex] + "<red>' is not online."));
                return null;
            }
            return target;
        }
        if (sender instanceof Player) {
            return (Player) sender;
        }
        sender.sendMessage("[LofiShop] Specify a player name.");
        return null;
    }
}
