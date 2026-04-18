package dev.lofishop.creator;

import java.util.ArrayList;
import java.util.List;

/**
 * All mutable state for one player's in-progress shop creation wizard.
 */
public class ShopCreatorSession {

    public enum State {
        MAIN_EDITOR,            // main shop editor GUI open
        AWAITING_SHOP_NAME,     // player typing shop name in chat
        AWAITING_SHOP_ID,       // player typing shop file ID in chat
        PRODUCT_EDITOR,         // product editor GUI open
        AWAITING_BUY_PRICE,     // player typing buy price in chat
        AWAITING_SELL_PRICE,    // player typing sell price in chat
        AWAITING_AMOUNT,        // player typing default amount in chat
        AWAITING_BUY_TIERS,     // player typing comma-separated buy amounts in chat
        AWAITING_SELL_TIERS,    // player typing comma-separated sell amounts in chat
        AWAITING_BUY_LIMIT,     // player typing personal buy limit in chat
        AWAITING_SELL_LIMIT     // player typing personal sell limit in chat
    }

    // ── Shop-level fields ─────────────────────────────────────────────────────
    private String shopName     = "New Shop";
    private String shopId       = "";          // derived from name, editable
    private int    rows         = 6;
    private boolean adminShop   = false;
    private String fillerMaterial = "GRAY_STAINED_GLASS_PANE";

    // ── Products ──────────────────────────────────────────────────────────────
    private final List<ProductDraft> products = new ArrayList<>();

    // ── Wizard state ──────────────────────────────────────────────────────────
    private State state = State.MAIN_EDITOR;
    private ProductDraft editingProduct = null;   // which product is currently in ProductEditorGui

    /** Non-null when editing an existing shop; tracks the original ID for rename/overwrite. */
    private String originalShopId = null;

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public String getShopName() { return shopName; }
    public void setShopName(String v) {
        this.shopName = v;
        // Auto-derive ID from name (lowercase, spaces → underscores)
        if (shopId.isBlank()) shopId = v.toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }

    public String getShopId() { return shopId; }
    public void setShopId(String v) { this.shopId = v.toLowerCase().replaceAll("[^a-z0-9_]", "_"); }

    public int getRows() { return rows; }
    public void setRows(int v) { this.rows = Math.max(1, Math.min(6, v)); }

    public boolean isAdminShop() { return adminShop; }
    public void toggleAdminShop() { this.adminShop = !this.adminShop; }

    public String getFillerMaterial() { return fillerMaterial; }
    public void setFillerMaterial(String v) { this.fillerMaterial = v; }

    public List<ProductDraft> getProducts() { return products; }

    public void addProduct(ProductDraft p) { products.add(p); }

    public void removeProduct(String key) {
        products.removeIf(p -> p.getKey().equals(key));
    }

    /** Generates the next available single-character layout key. */
    public String nextKey() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (char c : chars.toCharArray()) {
            String s = String.valueOf(c);
            if (products.stream().noneMatch(p -> p.getKey().equals(s))) return s;
        }
        return String.valueOf(products.size());
    }

    public State getState() { return state; }
    public void setState(State s) { this.state = s; }

    public ProductDraft getEditingProduct() { return editingProduct; }
    public void setEditingProduct(ProductDraft p) { this.editingProduct = p; }

    public String getOriginalShopId() { return originalShopId; }
    public void setOriginalShopId(String id) { this.originalShopId = id; }
    public boolean isEditMode() { return originalShopId != null; }
}
