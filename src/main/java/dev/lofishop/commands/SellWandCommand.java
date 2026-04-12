package dev.lofishop.commands;

import dev.lofishop.LofiShop;
import dev.lofishop.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * /sellwand [player] — shorthand to give a sell wand.
 */
public class SellWandCommand implements CommandExecutor {

    private final LofiShop plugin;

    public SellWandCommand(LofiShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lofishop.admin")) {
            if (sender instanceof Player) plugin.getMessageConfig().send((Player) sender, "no-permission");
            else sender.sendMessage("No permission.");
            return true;
        }

        Player target = null;
        if (args.length >= 1) {
            target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(MessageUtil.parse("<red>Player '" + args[0] + "' not found."));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        }

        if (target == null) {
            sender.sendMessage("Usage: /sellwand [player]");
            return true;
        }

        target.getInventory().addItem(plugin.getSellWandManager().createWand());
        plugin.getMessageConfig().send(target, "sellwand-given", Map.of("player", target.getName()));
        return true;
    }
}
