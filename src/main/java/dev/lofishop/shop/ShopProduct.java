package dev.lofishop.shop;

import dev.lofishop.util.NbtMatcher;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * A single purchasable/sellable product inside a shop.
 */
public class ShopProduct {

    private final String id;

    /**
     * The display item shown in the GUI.
     * For custom plugin items this is deserialized from the base64 `item-data`
     * field, so it carries the full original PDC/NBT data.
     */
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
     * Optional quantity tiers — if non-empty a picker sub-menu opens on click.
     * Example: [1, 8, 16, 64]
     */
    private final List<Integer> buyAmounts;
    private final List<Integer> sellAmounts;

    /**
     * When false, the display item is NOT automatically added to the player's inventory on buy.
     * Use this for service/command products (e.g. crate boxes) where a buy action delivers the reward.
     */
    private final boolean giveItem;

    public ShopProduct(String id, ItemStack displayItem, int amount,
                       List<ShopPrice> buyPrices, List<ShopPrice> sellPrices,
                       LimitSettings limits,
                       List<String> buyActions, List<String> sellActions,
                       List<String> buyConditions, List<String> sellConditions,
                       List<Integer> buyAmounts, List<Integer> sellAmounts,
                       boolean giveItem) {
        this.id           = id;
        this.displayItem  = displayItem;
        this.amount       = amount;
        this.buyPrices    = buyPrices;
        this.sellPrices   = sellPrices;
        this.limits       = limits;
        this.buyActions   = buyActions;
        this.sellActions  = sellActions;
        this.buyConditions  = buyConditions;
        this.sellConditions = sellConditions;
        this.buyAmounts   = buyAmounts  != null ? buyAmounts  : Collections.emptyList();
        this.sellAmounts  = sellAmounts != null ? sellAmounts : Collections.emptyList();
        this.giveItem     = giveItem;
    }

    /** Unique ID of this product within its shop. */
    public String getId() { return id; }

    /**
     * Returns the display item (cloned).
     * For custom items this carries the full original PDC data and is used
     * as the matching template by {@link dev.lofishop.util.NbtMatcher}.
     */
    public ItemStack getDisplayItem() { return displayItem.clone(); }

    /** How many items per transaction. */
    public int getAmount() { return amount; }

    public List<ShopPrice> getBuyPrices()  { return buyPrices; }
    public List<ShopPrice> getSellPrices() { return sellPrices; }
    public LimitSettings   getLimits()     { return limits; }

    public List<String> getBuyActions()     { return buyActions; }
    public List<String> getSellActions()    { return sellActions; }
    public List<String> getBuyConditions()  { return buyConditions; }
    public List<String> getSellConditions() { return sellConditions; }

    public boolean isBuyable()  { return !buyPrices.isEmpty(); }
    public boolean isSellable() { return !sellPrices.isEmpty(); }

    public boolean hasMultipleBuyAmounts()  { return !buyAmounts.isEmpty(); }
    public boolean hasMultipleSellAmounts() { return !sellAmounts.isEmpty(); }
    public boolean isGiveItem() { return giveItem; }

    public List<Integer> getBuyAmounts()  { return buyAmounts; }
    public List<Integer> getSellAmounts() { return sellAmounts; }

    public ShopPrice getPrimaryBuyPrice() {
        return buyPrices.isEmpty() ? null : buyPrices.get(0);
    }

    public ShopPrice getPrimarySellPrice() {
        return sellPrices.isEmpty() ? null : sellPrices.get(0);
    }

    /**
     * Checks whether {@code playerItem} matches this product's template.
     * Delegates to {@link NbtMatcher#matches} which handles vanilla items
     * (material + CMD) and custom plugin items (PDC identity).
     */
    public boolean matches(ItemStack playerItem) {
        return NbtMatcher.matches(playerItem, displayItem);
    }
}
