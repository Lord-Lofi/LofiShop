package dev.lofishop.creator;

import dev.lofishop.LofiShop;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages per-player shop creation wizard sessions and the creator wand item.
 */
public class ShopCreatorManager {

    private static final String WAND_PDC_KEY = "lofishop_creator_wand";

    private final LofiShop plugin;
    private final NamespacedKey wandKey;
    private final Map<UUID, ShopCreatorSession> sessions = new HashMap<>();

    public ShopCreatorManager(LofiShop plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, WAND_PDC_KEY);
    }

    // ── Wand ─────────────────────────────────────────────────────────────────

    public ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<gold><bold>Shop Creator Wand"));
            meta.lore(java.util.List.of(
                    net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                            .deserialize("<gray>Right-click to open the"),
                    net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                            .deserialize("<gray>shop creation wizard.")));
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            wand.setItemMeta(meta);
        }
        return wand;
    }

    public boolean isCreatorWand(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    // ── Session management ────────────────────────────────────────────────────

    public ShopCreatorSession startSession(Player player) {
        ShopCreatorSession session = new ShopCreatorSession();
        sessions.put(player.getUniqueId(), session);
        return session;
    }

    public ShopCreatorSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void endSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public void openWizard(Player player) {
        ShopCreatorSession session = hasSession(player)
                ? getSession(player) : startSession(player);
        new ShopCreatorGui(plugin, player, session).open();
    }
}
