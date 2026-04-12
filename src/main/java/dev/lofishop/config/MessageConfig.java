package dev.lofishop.config;

import dev.lofishop.LofiShop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;

public class MessageConfig {

    private final LofiShop plugin;
    private FileConfiguration messages;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public MessageConfig(LofiShop plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    /** Returns the raw MiniMessage string for the given key. */
    public String getRaw(String key) {
        return messages.getString(key, "<red>Missing message: " + key);
    }

    /** Parses a message key with named placeholders. */
    public Component get(String key, Map<String, String> placeholders) {
        String raw = getRaw(key);
        String prefix = getRaw("prefix");
        raw = raw.replace("{prefix}", prefix);

        TagResolver.Builder resolver = TagResolver.builder();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolver.resolver(Placeholder.parsed(entry.getKey(), entry.getValue()));
        }

        return mm.deserialize(raw, resolver.build());
    }

    /** Parses a message key with no extra placeholders. */
    public Component get(String key) {
        return get(key, Map.of());
    }

    /** Sends a message to a player. */
    public void send(Player player, String key, Map<String, String> placeholders) {
        player.sendMessage(get(key, placeholders));
    }

    public void send(Player player, String key) {
        send(player, key, Map.of());
    }

    /** Parses any MiniMessage string with placeholders. */
    public Component parse(String raw, Map<String, String> placeholders) {
        TagResolver.Builder resolver = TagResolver.builder();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolver.resolver(Placeholder.parsed(entry.getKey(), entry.getValue()));
        }
        return mm.deserialize(raw, resolver.build());
    }

    public Component parse(String raw) {
        return mm.deserialize(raw);
    }
}
