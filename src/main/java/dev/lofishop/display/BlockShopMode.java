package dev.lofishop.display;

/**
 * Controls how a physical block shop behaves when right-clicked.
 */
public enum BlockShopMode {

    /**
     * Opens the full shop GUI for the linked shop.
     * Best for category shops (ore shop, wood shop) where the block
     * is an entrance to a multi-product store.
     */
    FULL,

    /**
     * Opens a compact 3-row GUI showing only the linked product —
     * buy button on the left, sell button on the right.
     * Best for dedicated vending-machine-style blocks.
     */
    SMALL,

    /**
     * Instantly purchases the product's default amount with no GUI.
     * Feedback is sent as a chat message.
     * Best for impulse-buy setups or heavily used single items.
     */
    QUICK;

    /** Case-insensitive parse with FULL as the fallback. */
    public static BlockShopMode fromString(String s) {
        if (s == null) return FULL;
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FULL;
        }
    }
}
