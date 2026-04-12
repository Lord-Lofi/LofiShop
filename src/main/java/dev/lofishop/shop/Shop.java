package dev.lofishop.shop;

import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a fully loaded shop, parsed from a YAML config file.
 */
public class Shop {

    private final String id;
    private final String name;
    private final String title;
    private final int rows;
    private final String openPermission;

    /** Ordered list of layout rows. Each row is 9 characters (product key or special token). */
    private final List<String> layout;

    /** Product key → ShopProduct */
    private final Map<String, ShopProduct> products;

    /** Filler item for empty slots */
    private final ItemStack fillerItem;

    /**
     * Admin shops have infinite stock — buying never depletes a physical chest,
     * and all buy/sell limits are ignored for the shop itself.
     * Players still need economy balance to buy; sellers are always paid.
     */
    private final boolean adminShop;

    public Shop(String id, String name, String title, int rows,
                String openPermission, List<String> layout,
                Map<String, ShopProduct> products, ItemStack fillerItem,
                boolean adminShop) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.rows = Math.max(1, Math.min(6, rows));
        this.openPermission = openPermission;
        this.layout = Collections.unmodifiableList(layout);
        this.products = Collections.unmodifiableMap(new LinkedHashMap<>(products));
        this.fillerItem = fillerItem;
        this.adminShop = adminShop;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getTitle() { return title; }
    public int getRows() { return rows; }
    public int getSize() { return rows * 9; }
    public String getOpenPermission() { return openPermission; }
    public List<String> getLayout() { return layout; }
    public Map<String, ShopProduct> getProducts() { return products; }
    public ItemStack getFillerItem() { return fillerItem != null ? fillerItem.clone() : null; }
    /** True = infinite stock, limits bypassed for all products in this shop. */
    public boolean isAdminShop() { return adminShop; }

    public ShopProduct getProduct(String key) { return products.get(key); }

    /**
     * Returns the product key at a given inventory slot, or null if none.
     * Layout rows are 9-character strings (spaces allowed).
     */
    public String getProductKeyAt(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        if (row >= layout.size()) return null;

        String rowStr = layout.get(row);
        // Each cell is separated by spaces; split and index
        String[] cells = rowStr.split(" ");
        if (col >= cells.length) return null;

        String key = cells[col].trim();
        if (key.isEmpty() || key.equals("F") || key.equals("BACK")
                || key.equals("NEXT") || key.equals("PREV")) return null;

        return products.containsKey(key) ? key : null;
    }

    /** Returns true if the given slot is a filler slot. */
    public boolean isFillerSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        if (row >= layout.size()) return true;
        String[] cells = layout.get(row).split(" ");
        if (col >= cells.length) return true;
        String key = cells[col].trim();
        return key.isEmpty() || key.equals("F");
    }
}
