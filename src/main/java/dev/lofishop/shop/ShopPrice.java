package dev.lofishop.shop;

/**
 * Represents a single price entry for a product.
 * Supports multiple currency types via the economy provider ID.
 */
public class ShopPrice {

    private final String economyId;
    private final double amount;

    public ShopPrice(String economyId, double amount) {
        this.economyId = economyId;
        this.amount = amount;
    }

    /** The economy provider ID (e.g. "vault", "essentials"). */
    public String getEconomyId() { return economyId; }

    /** The price amount. */
    public double getAmount() { return amount; }
}
