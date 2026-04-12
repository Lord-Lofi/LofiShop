package dev.lofishop.limit;

/**
 * Stores the current transaction counts for a single product+player (or global).
 */
public class LimitData {

    private int buyCount;
    private int sellCount;
    private long lastResetTimestamp; // epoch millis

    public LimitData(int buyCount, int sellCount, long lastResetTimestamp) {
        this.buyCount = buyCount;
        this.sellCount = sellCount;
        this.lastResetTimestamp = lastResetTimestamp;
    }

    public static LimitData empty() {
        return new LimitData(0, 0, System.currentTimeMillis());
    }

    public int getBuyCount() { return buyCount; }
    public int getSellCount() { return sellCount; }
    public long getLastResetTimestamp() { return lastResetTimestamp; }

    public void incrementBuy(int amount) { buyCount += amount; }
    public void incrementSell(int amount) { sellCount += amount; }

    public void reset() {
        buyCount = 0;
        sellCount = 0;
        lastResetTimestamp = System.currentTimeMillis();
    }

    public void setBuyCount(int v) { this.buyCount = v; }
    public void setSellCount(int v) { this.sellCount = v; }
    public void setLastResetTimestamp(long v) { this.lastResetTimestamp = v; }
}
