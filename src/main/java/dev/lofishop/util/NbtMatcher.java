package dev.lofishop.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.Base64;
import java.util.Set;

/**
 * Handles NBT/PDC-aware item matching and serialization for LofiShop.
 *
 * Items in shop configs can be stored two ways:
 *
 *   1. Simple (material [+ custom-model-data]) — vanilla items with no plugins.
 *      Stored as the normal material/name/lore fields in YAML.
 *
 *   2. NBT-serialized — any custom item (MMOItems, Oraxen, ItemsAdder,
 *      MythicMobs, MobHeads, etc.). The full item bytes are base64-encoded and
 *      stored under the `item-data` key in YAML.
 *
 *      When matching, we compare in priority order:
 *        a) PDC keys — covers most custom plugin items
 *        b) Skull texture — covers PLAYER_HEAD items (MobHeads, HeadDatabase,
 *           custom decorative heads) that store identity only in SkullMeta
 *        c) Custom Model Data — vanilla items with resource-pack skins
 *        d) Material only — plain vanilla items
 *
 *      Mutable fields (durability, stack size, enchantment levels from use)
 *      are intentionally excluded so a "used" custom item still matches.
 */
public final class NbtMatcher {

    private NbtMatcher() {}

    // ── Serialization ─────────────────────────────────────────────────────────

    /**
     * Serializes an ItemStack to a base64 string using Paper's byte serializer.
     * Captures material, all PDC data, skull texture, custom model data, and meta.
     */
    public static String toBase64(ItemStack item) {
        if (item == null) return null;
        byte[] bytes = item.serializeAsBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Deserializes a base64 string back to an ItemStack.
     * Returns null if the string is null, blank, or malformed.
     */
    public static ItemStack fromBase64(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    // ── Matching ──────────────────────────────────────────────────────────────

    /**
     * Matches a player's item against a shop template.
     *
     * Priority order:
     *   1. Materials must match.
     *   2. If the template has PDC data, ALL of those key-value pairs must be
     *      present and equal on the player's item (covers most plugin items).
     *   3. If the template is a PLAYER_HEAD with a skull texture (and no PDC),
     *      the player's item must have the same "textures" profile property.
     *      This covers MobHeads, HeadDatabase, and any decorative head plugin
     *      that stores identity only in SkullMeta rather than PDC.
     *   4. If the template has no PDC or skull texture but has Custom Model Data,
     *      the player's item must have the same CMD.
     *   5. If none of the above apply, material match alone is sufficient.
     */
    public static boolean matches(ItemStack playerItem, ItemStack template) {
        if (playerItem == null || template == null) return false;
        if (playerItem.getType() != template.getType()) return false;

        ItemMeta templateMeta = template.getItemMeta();
        ItemMeta playerMeta   = playerItem.getItemMeta();

        if (templateMeta == null) return true; // material-only match

        PersistentDataContainer templatePdc = templateMeta.getPersistentDataContainer();
        Set<NamespacedKey> templateKeys = templatePdc.getKeys();

        // Priority 1 — PDC identity check (plugin items: MMOItems, Oraxen, MobHeads-PDC, etc.)
        if (!templateKeys.isEmpty()) {
            if (playerMeta == null) return false;
            PersistentDataContainer playerPdc = playerMeta.getPersistentDataContainer();
            return pdcContains(playerPdc, templatePdc, templateKeys);
        }

        // Priority 2 — Skull texture check (PLAYER_HEAD items with no PDC)
        // Handles MobHeads, HeadDatabase, custom decorative heads stored only in SkullMeta.
        if (templateMeta instanceof SkullMeta) {
            SkullMeta templateSkull = (SkullMeta) templateMeta;
            if (!(playerMeta instanceof SkullMeta)) return false;
            SkullMeta playerSkull = (SkullMeta) playerMeta;
            return skullTextureMatches(templateSkull, playerSkull);
        }

        // Priority 3 — Custom Model Data (vanilla items with resource-pack skins)
        if (templateMeta.hasCustomModelData()) {
            if (playerMeta == null || !playerMeta.hasCustomModelData()) return false;
            return templateMeta.getCustomModelData() == playerMeta.getCustomModelData();
        }

        // Priority 4 — Material-only match
        return true;
    }

    /**
     * Compares two PLAYER_HEAD items by their "textures" profile property value.
     *
     * The textures value is a base64-encoded JSON string containing the skin URL.
     * Two heads from the same source (e.g. the same MobHeads mob type) will have
     * identical texture values. If the template has no texture set, any head of
     * the same material is accepted.
     */
    private static boolean skullTextureMatches(SkullMeta template, SkullMeta player) {
        String templateTexture = extractTexture(template);

        // No texture on the template — plain player head, material match is enough
        if (templateTexture == null) return true;

        String playerTexture = extractTexture(player);
        return templateTexture.equals(playerTexture);
    }

    /**
     * Extracts the base64 "textures" property value from a SkullMeta, or null
     * if the skull has no profile or no textures property.
     */
    private static String extractTexture(SkullMeta skull) {
        PlayerProfile profile = skull.getPlayerProfile();
        if (profile == null) return null;
        for (ProfileProperty prop : profile.getProperties()) {
            if ("textures".equals(prop.getName())) {
                return prop.getValue();
            }
        }
        return null;
    }

    /**
     * Checks that every key present in {@code source} also exists in
     * {@code target} with the same value. Reads STRING, INTEGER, LONG, BYTE.
     */
    private static boolean pdcContains(PersistentDataContainer target,
                                        PersistentDataContainer source,
                                        Set<NamespacedKey> keys) {
        for (NamespacedKey key : keys) {
            if (source.has(key, PersistentDataType.STRING)) {
                String expected = source.get(key, PersistentDataType.STRING);
                if (!target.has(key, PersistentDataType.STRING)) return false;
                String actual = target.get(key, PersistentDataType.STRING);
                if (!expected.equals(actual)) return false;

            } else if (source.has(key, PersistentDataType.INTEGER)) {
                Integer expected = source.get(key, PersistentDataType.INTEGER);
                if (!target.has(key, PersistentDataType.INTEGER)) return false;
                if (!expected.equals(target.get(key, PersistentDataType.INTEGER))) return false;

            } else if (source.has(key, PersistentDataType.LONG)) {
                Long expected = source.get(key, PersistentDataType.LONG);
                if (!target.has(key, PersistentDataType.LONG)) return false;
                if (!expected.equals(target.get(key, PersistentDataType.LONG))) return false;

            } else if (source.has(key, PersistentDataType.BYTE)) {
                Byte expected = source.get(key, PersistentDataType.BYTE);
                if (!target.has(key, PersistentDataType.BYTE)) return false;
                if (!expected.equals(target.get(key, PersistentDataType.BYTE))) return false;
            }
            // Other PDC types (compound, byte arrays, etc.) are skipped —
            // they're typically internal engine data, not item identity.
        }
        return true;
    }

    // ── Plugin detection ──────────────────────────────────────────────────────

    /**
     * Detects which plugin created a custom item by checking known PDC namespaces
     * and SkullMeta texture presence.
     * Returns a human-readable label or null for vanilla items.
     */
    public static String detectPlugin(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        for (NamespacedKey key : meta.getPersistentDataContainer().getKeys()) {
            String ns = key.getNamespace();
            if (ns.equals("mmoitems"))        return "MMOItems";
            if (ns.equals("oraxen"))          return "Oraxen";
            if (ns.equals("itemsadder"))      return "ItemsAdder";
            if (ns.equals("mythicmobs"))      return "MythicMobs";
            if (ns.equals("executableitems")) return "ExecutableItems";
            if (ns.equals("nexo"))            return "Nexo";
            if (ns.equals("crucible"))        return "Crucible";
            if (ns.equals("mobheads"))        return "MobHeads";
            if (ns.equals("headdb"))          return "HeadDatabase";
        }

        // PLAYER_HEAD with a texture but no PDC — decorative head (MobHeads, custom heads)
        if (meta instanceof SkullMeta) {
            SkullMeta skull = (SkullMeta) meta;
            if (extractTexture(skull) != null) return "Custom Head";
        }

        return null;
    }

    /**
     * Returns a short summary of the item's identity data for display in
     * the shop creator GUI (so admins can confirm what's being captured).
     */
    public static String pdcSummary(ItemStack item) {
        if (item == null) return "none";
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return "none";

        Set<NamespacedKey> keys = meta.getPersistentDataContainer().getKeys();

        // For skull items with no PDC, report the texture as the identity
        if (keys.isEmpty() && meta instanceof SkullMeta) {
            String texture = extractTexture((SkullMeta) meta);
            if (texture != null) {
                // Show first 20 chars of the texture value — enough to confirm it's unique
                String preview = texture.length() > 20 ? texture.substring(0, 20) + "…" : texture;
                return "skull-texture:" + preview;
            }
            return "none";
        }

        if (keys.isEmpty()) return "none";

        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (NamespacedKey key : keys) {
            if (shown > 0) sb.append(", ");
            sb.append(key.getNamespace()).append(':').append(key.getKey());
            if (++shown >= 3) { sb.append(" (+").append(keys.size() - 3).append(" more)"); break; }
        }
        return sb.toString();
    }
}
