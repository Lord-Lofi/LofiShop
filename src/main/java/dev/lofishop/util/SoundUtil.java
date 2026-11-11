package dev.lofishop.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * Plays a sound named in a config file.
 *
 * Accepts both formats:
 * - Sound keys, like "block.chest.open" or "minecraft:entity.player.levelup".
 *   These are played directly, so custom resource-pack sounds work too.
 * - Old-style constant names, like "ENTITY_EXPERIENCE_ORB_PICKUP", for existing configs.
 *
 * Returns false if the name isn't a known sound (keys can't be checked, so they always return true).
 */
public final class SoundUtil {

    private SoundUtil() {}

    public static boolean play(Player player, String sound) {
        if (player == null || sound == null || sound.isBlank()) return false;
        String name = sound.strip();

        if (name.contains(".") || name.contains(":")) {
            player.playSound(player.getLocation(), name.toLowerCase(Locale.ROOT), 1f, 1f);
            return true;
        }

        // Old-style name: look up the public constant of that name on org.bukkit.Sound.
        // Sounds are no longer an enum, so Sound.valueOf(...) is not available.
        try {
            Field field = Sound.class.getField(name.toUpperCase(Locale.ROOT));
            if (field.get(null) instanceof Sound constant) {
                player.playSound(player.getLocation(), constant, 1f, 1f);
                return true;
            }
        } catch (ReflectiveOperationException ignored) {
            // Not a known sound name
        }
        return false;
    }
}
