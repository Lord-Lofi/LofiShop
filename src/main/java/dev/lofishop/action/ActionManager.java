package dev.lofishop.action;

import dev.lofishop.LofiShop;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Parses and executes action strings defined in shop configs.
 *
 * Supported formats:
 *   [message] <text>           — Send MiniMessage text to player
 *   [actionbar] <text>         — Send MiniMessage actionbar
 *   [title] <title>|<subtitle> — Send title/subtitle
 *   [sound] SOUND_NAME         — Play a sound at the player
 *   [command] <cmd>            — Run command as player
 *   [console] <cmd>            — Run command as console
 */
public class ActionManager {

    private final LofiShop plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ActionManager(LofiShop plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes a list of action strings for a player.
     * @param actions  the raw action strings from config
     * @param player   the player context
     * @param placeholders  simple {key} → value replacements
     */
    public void execute(List<String> actions, Player player, Map<String, String> placeholders) {
        if (actions == null || actions.isEmpty()) return;
        for (String action : actions) {
            try {
                executeOne(apply(action, placeholders), player);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to execute action '" + action + "': " + e.getMessage());
            }
        }
    }

    private void executeOne(String action, Player player) {
        if (action.startsWith("[message]")) {
            String text = action.substring("[message]".length()).trim();
            player.sendMessage(mm.deserialize(text));

        } else if (action.startsWith("[actionbar]")) {
            String text = action.substring("[actionbar]".length()).trim();
            player.sendActionBar(mm.deserialize(text));

        } else if (action.startsWith("[title]")) {
            String text = action.substring("[title]".length()).trim();
            String[] parts = text.split("\\|", 2);
            var title = parts.length > 1
                    ? Title.title(mm.deserialize(parts[0]), mm.deserialize(parts[1]),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2500), Duration.ofMillis(500)))
                    : Title.title(mm.deserialize(parts[0]), net.kyori.adventure.text.Component.empty());
            player.showTitle(title);

        } else if (action.startsWith("[sound]")) {
            String soundName = action.substring("[sound]".length()).trim().toUpperCase();
            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown sound: " + soundName);
            }

        } else if (action.startsWith("[command]")) {
            String cmd = action.substring("[command]".length()).trim();
            player.performCommand(cmd);

        } else if (action.startsWith("[console]")) {
            String cmd = action.substring("[console]".length()).trim();
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);

        } else {
            plugin.getLogger().warning("Unknown action type: " + action);
        }
    }

    private String apply(String template, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            template = template.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return template;
    }
}
