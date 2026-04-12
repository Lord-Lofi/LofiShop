package dev.lofishop.creator;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable draft for a single product being built in the GUI wizard.
 */
public class ProductDraft {

    /** Single-character key used in the shop layout grid. */
    private String key;

    /** The item this product represents (set by dragging into the editor slot). */
    private ItemStack item;

    private double buyPrice  = -1;   // -1 = not buyable
    private double sellPrice = -1;   // -1 = not sellable
    private int    amount    = 1;
    private int    personalBuyLimit  = -1;
    private int    personalSellLimit = -1;
    private String resetType = "NEVER";

    /** Optional quantity tiers (empty = use `amount` directly). */
    private List<Integer> buyAmounts  = new ArrayList<>();
    private List<Integer> sellAmounts = new ArrayList<>();

    public ProductDraft(String key) { this.key = key; }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public ItemStack getItem() { return item; }
    public void setItem(ItemStack item) { this.item = item != null ? item.clone() : null; }

    public double getBuyPrice() { return buyPrice; }
    public void setBuyPrice(double v) { this.buyPrice = v; }

    public double getSellPrice() { return sellPrice; }
    public void setSellPrice(double v) { this.sellPrice = v; }

    public int getAmount() { return amount; }
    public void setAmount(int v) { this.amount = Math.max(1, v); }

    public int getPersonalBuyLimit() { return personalBuyLimit; }
    public void setPersonalBuyLimit(int v) { this.personalBuyLimit = v; }

    public int getPersonalSellLimit() { return personalSellLimit; }
    public void setPersonalSellLimit(int v) { this.personalSellLimit = v; }

    public String getResetType() { return resetType; }
    public void setResetType(String v) { this.resetType = v; }

    public List<Integer> getBuyAmounts() { return buyAmounts; }
    public void setBuyAmounts(List<Integer> v) { this.buyAmounts = v; }

    public List<Integer> getSellAmounts() { return sellAmounts; }
    public void setSellAmounts(List<Integer> v) { this.sellAmounts = v; }

    public boolean isReady() { return item != null && (buyPrice >= 0 || sellPrice >= 0); }
}
