package dev.lofishop.shop;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * A single purchasable/sellable product inside a shop.
 */
public class ShopProduct {

    private final String id;
    private final ItemStack displayItem;
    private final int amount;

    private final List<ShopPrice> buyPrices;
    private final List<ShopPrice> sellPrices;

    private final LimitSettings limits;

    private final List<String> buyActions;
    private final List<String> sellActions;
    private final List<String> buyConditions;
    private final List<String> sellConditions;

    /**
     * Optional quantity tiers for buy/sell.
     * If non-empty, clicking the product opens a QuantityPickerMenu instead of
     * transacting directly. Each value is a discrete amount the player can choose.
     * Example: [1, 8, 16, 64]
     */
    private final List<Integer> buyAmounts;
    private final List<Integer> sellAmounts;

    public ShopProduct(String id, ItemStack displayItem, int amount,
                       List<ShopPrice> buyPrices, List<ShopPrice> sellPrices,
                       LimitSettings limits,
                       List<String> buyActions, List<String> sellActions,
                       List<String> buyConditions, List<String> sellConditions,
                       List<Integer> buyAmounts, List<Integer> sellAmounts) {
        this.id = id;
        this.displayItem = displayItem;
        this.amount = amount;
        this.buyPrices = buyPrices;
        this.sellPrices = sellPrices;
        this.limits = limits;
        this.buyActions = buyActions;
        this.sellActions = sellActions;
        this.buyConditions = buyConditions;
        this.sellConditions = sellConditions;
        this.buyAmounts = buyAmounts != null ? buyAmounts : java.util.Collections.emptyList();
        this.sellAmounts = sellAmounts != null ? sellAmounts : java.util.Collections.emptyList();
    }

    /** Unique ID of this product within its shop. */
    public String getId() { return id; }

    /** Item shown in the shop GUI. */
    public ItemStack getDisplayItem() { return displayItem.clone(); }

    /** How many items per transaction. */
    public int getAmount() { return amount; }

    /** Buy price list (may contain multiple currencies). */
    public List<ShopPrice> getBuyPrices() { return buyPrices; }

    /** Sell price list. */
    public List<ShopPrice> getSellPrices() { return sellPrices; }

    public LimitSettings getLimits() { return limits; }

    public List<String> getBuyActions() { return buyActions; }
    public List<String> getSellActions() { return sellActions; }
    public List<String> getBuyConditions() { return buyConditions; }
    public List<String> getSellConditions() { return sellConditions; }

    public boolean isBuyable() { return !buyPrices.isEmpty(); }
    public boolean isSellable() { return !sellPrices.isEmpty(); }

    /** True if this product shows a quantity-picker sub-menu on buy. */
    public boolean hasMultipleBuyAmounts() { return !buyAmounts.isEmpty(); }
    /** True if this product shows a quantity-picker sub-menu on sell. */
    public boolean hasMultipleSellAmounts() { return !sellAmounts.isEmpty(); }

    public List<Integer> getBuyAmounts() { return buyAmounts; }
    public List<Integer> getSellAmounts() { return sellAmounts; }

    /** Returns the first buy price or null. */
    public ShopPrice getPrimaryBuyPrice() {
        return buyPrices.isEmpty() ? null : buyPrices.get(0);
    }

    /** Returns the first sell price or null. */
    public ShopPrice getPrimarySellPrice() {
        return sellPrices.isEmpty() ? null : sellPrices.get(0);
    }
}
