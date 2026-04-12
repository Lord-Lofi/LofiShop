package dev.lofishop.shop;

/**
 * Limit configuration attached to a product.
 */
public class LimitSettings {

    public enum ResetType { NEVER, DAILY, WEEKLY, CRON }

    private final int personalBuy;
    private final int personalSell;
    private final int globalBuy;
    private final int globalSell;
    private final ResetType resetType;
    private final String cronExpression; // used when resetType == CRON

    public LimitSettings(int personalBuy, int personalSell,
                         int globalBuy, int globalSell,
                         ResetType resetType, String cronExpression) {
        this.personalBuy = personalBuy;
        this.personalSell = personalSell;
        this.globalBuy = globalBuy;
        this.globalSell = globalSell;
        this.resetType = resetType;
        this.cronExpression = cronExpression;
    }

    public static LimitSettings unlimited() {
        return new LimitSettings(-1, -1, -1, -1, ResetType.NEVER, null);
    }

    public int getPersonalBuy() { return personalBuy; }
    public int getPersonalSell() { return personalSell; }
    public int getGlobalBuy() { return globalBuy; }
    public int getGlobalSell() { return globalSell; }
    public ResetType getResetType() { return resetType; }
    public String getCronExpression() { return cronExpression; }

    public boolean hasPersonalBuyLimit() { return personalBuy > 0; }
    public boolean hasPersonalSellLimit() { return personalSell > 0; }
    public boolean hasGlobalBuyLimit() { return globalBuy > 0; }
    public boolean hasGlobalSellLimit() { return globalSell > 0; }
}
