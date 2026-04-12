package dev.lofishop.util;

/**
 * A single PDC key → expected-value rule used for custom item matching.
 *
 * In shop YAML:
 * <pre>
 * item:
 *   material: DIAMOND_SWORD
 *   nbt-match:
 *     - key: "mmoitems:item_id"     # namespace:path
 *       value: "FIRE_SWORD"         # expected string value
 *     - key: "oraxen:id"
 *       value: "my_custom_item"
 * </pre>
 *
 * All rules must pass for an item to match.
 * Values are always compared as strings for config simplicity;
 * the matcher will try string first, then integer, then byte.
 */
public class NbtMatchRule {

    private final String namespacedKey; // e.g. "mmoitems:item_id"
    private final String expectedValue; // e.g. "FIRE_SWORD"

    public NbtMatchRule(String namespacedKey, String expectedValue) {
        this.namespacedKey = namespacedKey;
        this.expectedValue = expectedValue;
    }

    public String getNamespacedKey() { return namespacedKey; }
    public String getExpectedValue()  { return expectedValue; }

    /** Splits the key into [namespace, path]. Falls back to ["lofishop", key] if no colon. */
    public String[] splitKey() {
        int colon = namespacedKey.indexOf(':');
        if (colon < 0) return new String[]{"lofishop", namespacedKey};
        return new String[]{ namespacedKey.substring(0, colon),
                             namespacedKey.substring(colon + 1) };
    }

    @Override
    public String toString() {
        return namespacedKey + "=" + expectedValue;
    }
}
