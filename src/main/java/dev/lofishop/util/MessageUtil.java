package dev.lofishop.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Convenience methods for MiniMessage parsing outside of MessageConfig context.
 */
public final class MessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private MessageUtil() {}

    public static Component parse(String miniMessage) {
        return MM.deserialize(miniMessage);
    }

    public static String strip(String miniMessage) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(MM.deserialize(miniMessage));
    }
}
