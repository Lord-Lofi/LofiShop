package dev.lofishop.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
 *      MythicMobs, etc.). The full item bytes are base64-encoded and stored
 *      under the `item-data` key in YAML.
 *
 *      When matching, we compare the PDC data of the template against the
 *      player's item. Mutable fields (durability, stack size, enchantment
 *      levels from use) are intentionally excluded so a "used" custom item
 *      still matches its shop entry.
 */
public final class NbtMatcher {

    private NbtMatcher() {}

    // ── Serialization ─────────────────────────────────────────────────────────

    /**
     * Serializes an ItemStack to a base64 string using Paper's byte serializer.
     * Captures material, all PDC data, custom model data, and meta.
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
     * Matches a player's item against a shop template using PDC identity.
     *
     * Rules:
     *   1. Materials must match.
     *   2. If the template has PDC data, ALL of those key-value pairs must be
     *      present and equal on the player's item (identity check).
     *      This handles custom plugin items reliably.
     *   3. If the template has no PDC data but has a custom model data value,
     *      the player's item must have the same CMD.
     *   4. If the template has neither PDC nor CMD, only material is checked.
     *
     * This means admins never need to configure anything special — the shop
     * creator captures the item as-is and this method handles matching.
     */
    public static boolean matches(ItemStack playerItem, ItemStack template) {
        if (playerItem == null || template == null) return false;
        if (playerItem.getType() != template.getType()) return false;

        ItemMeta templateMeta = template.getItemMeta();
        ItemMeta playerMeta   = playerItem.getItemMeta();

        if (templateMeta == null) return true; // material-only match

        PersistentDataContainer templatePdc = templateMeta.getPersistentDataContainer();
        Set<NamespacedKey> templateKeys = templatePdc.getKeys();

        // If the template carries PDC data, compare it (plugin items)
        if (!templateKeys.isEmpty()) {
            if (playerMeta == null) return false;
            PersistentDataContainer playerPdc = playerMeta.getPersistentDataContainer();
            return pdcContains(playerPdc, templatePdc, templateKeys);
        }

        // No PDC — fall back to custom model data
        if (templateMeta.hasCustomModelData()) {
            if (playerMeta == null || !playerMeta.hasCustomModelData()) return false;
            return templateMeta.getCustomModelData() == playerMeta.getCustomModelData();
        }

        // Material-only match
        return true;
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
     * Detects which plugin created a custom item by checking known PDC namespaces.
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
        }
        return null;
    }

    /**
     * Returns a short summary of the item's PDC keys for display in
     * the shop creator GUI (so admins can confirm what's being captured).
     */
    public static String pdcSummary(ItemStack item) {
        if (item == null) return "none";
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return "none";
        Set<NamespacedKey> keys = meta.getPersistentDataContainer().getKeys();
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
