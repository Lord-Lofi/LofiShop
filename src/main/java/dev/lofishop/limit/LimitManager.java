package dev.lofishop.limit;

import dev.lofishop.LofiShop;
import dev.lofishop.shop.LimitSettings;
import dev.lofishop.shop.ShopProduct;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Tracks and persists buy/sell limit counts per player and globally.
 * Storage format: plugins/LofiShop/limits/{shopId}/{productId}.yml
 *   Keys: "personal.<uuid>.buy", "personal.<uuid>.sell", "personal.<uuid>.reset"
 *         "global.buy", "global.sell", "global.reset"
 */
public class LimitManager {

    private final LofiShop plugin;

    // Cache: shopId+productId → (uuid or "global") → LimitData
    private final Map<String, Map<String, LimitData>> cache = new HashMap<>();

    public LimitManager(LofiShop plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        cache.clear();
        // Data will be re-loaded on demand
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns true if the player can buy the given amount of the product
     * without exceeding limits.
     */
    public boolean canBuy(Player player, String shopId, ShopProduct product, int amount) {
        LimitSettings limits = product.getLimits();
        if (!limits.hasPersonalBuyLimit() && !limits.hasGlobalBuyLimit()) return true;

        checkReset(shopId, product);

        if (limits.hasPersonalBuyLimit()) {
            LimitData data = getPersonal(player.getUniqueId(), shopId, product.getId());
            if (data.getBuyCount() + amount > limits.getPersonalBuy()) return false;
        }
        if (limits.hasGlobalBuyLimit()) {
            LimitData data = getGlobal(shopId, product.getId());
            if (data.getBuyCount() + amount > limits.getGlobalBuy()) return false;
        }
        return true;
    }

    /**
     * Returns true if the player can sell the given amount.
     */
    public boolean canSell(Player player, String shopId, ShopProduct product, int amount) {
        LimitSettings limits = product.getLimits();
        if (!limits.hasPersonalSellLimit() && !limits.hasGlobalSellLimit()) return true;

        checkReset(shopId, product);

        if (limits.hasPersonalSellLimit()) {
            LimitData data = getPersonal(player.getUniqueId(), shopId, product.getId());
            if (data.getSellCount() + amount > limits.getPersonalSell()) return false;
        }
        if (limits.hasGlobalSellLimit()) {
            LimitData data = getGlobal(shopId, product.getId());
            if (data.getSellCount() + amount > limits.getGlobalSell()) return false;
        }
        return true;
    }

    public void recordBuy(Player player, String shopId, ShopProduct product, int amount) {
        LimitSettings limits = product.getLimits();
        if (limits.hasPersonalBuyLimit()) {
            getPersonal(player.getUniqueId(), shopId, product.getId()).incrementBuy(amount);
        }
        if (limits.hasGlobalBuyLimit()) {
            getGlobal(shopId, product.getId()).incrementBuy(amount);
        }
        saveLazy(shopId, product.getId());
    }

    public void recordSell(Player player, String shopId, ShopProduct product, int amount) {
        LimitSettings limits = product.getLimits();
        if (limits.hasPersonalSellLimit()) {
            getPersonal(player.getUniqueId(), shopId, product.getId()).incrementSell(amount);
        }
        if (limits.hasGlobalSellLimit()) {
            getGlobal(shopId, product.getId()).incrementSell(amount);
        }
        saveLazy(shopId, product.getId());
    }

    /** Returns how many more the player can buy (-1 = unlimited). */
    public int remainingBuys(Player player, String shopId, ShopProduct product) {
        LimitSettings limits = product.getLimits();
        if (!limits.hasPersonalBuyLimit()) return -1;
        checkReset(shopId, product);
        LimitData data = getPersonal(player.getUniqueId(), shopId, product.getId());
        return Math.max(0, limits.getPersonalBuy() - data.getBuyCount());
    }

    /** Returns how many more the player can sell (-1 = unlimited). */
    public int remainingSells(Player player, String shopId, ShopProduct product) {
        LimitSettings limits = product.getLimits();
        if (!limits.hasPersonalSellLimit()) return -1;
        checkReset(shopId, product);
        LimitData data = getPersonal(player.getUniqueId(), shopId, product.getId());
        return Math.max(0, limits.getPersonalSell() - data.getSellCount());
    }

    // ── Reset logic ───────────────────────────────────────────────────────────

    private void checkReset(String shopId, ShopProduct product) {
        LimitSettings limits = product.getLimits();
        if (limits.getResetType() == LimitSettings.ResetType.NEVER) return;

        String cacheKey = shopId + ":" + product.getId();
        Map<String, LimitData> productCache = cache.get(cacheKey);
        if (productCache == null) return;

        for (Map.Entry<String, LimitData> entry : productCache.entrySet()) {
            if (shouldReset(entry.getValue(), limits)) {
                entry.getValue().reset();
            }
        }
    }

    private boolean shouldReset(LimitData data, LimitSettings limits) {
        long last = data.getLastResetTimestamp();
        Instant lastInstant = Instant.ofEpochMilli(last);
        ZonedDateTime lastTime = lastInstant.atZone(ZoneId.systemDefault());
        ZonedDateTime now = ZonedDateTime.now();

        return switch (limits.getResetType()) {
            case DAILY -> !lastTime.toLocalDate().equals(now.toLocalDate());
            case WEEKLY -> {
                String resetDay = plugin.getConfig()
                        .getString("limits.weekly-reset-day", "MONDAY").toUpperCase();
                DayOfWeek resetDow = DayOfWeek.valueOf(resetDay);
                LocalDate lastWeekStart = lastTime.toLocalDate()
                        .with(java.time.temporal.TemporalAdjusters.previousOrSame(resetDow));
                LocalDate nowWeekStart = now.toLocalDate()
                        .with(java.time.temporal.TemporalAdjusters.previousOrSame(resetDow));
                yield !lastWeekStart.equals(nowWeekStart);
            }
            default -> false;
        };
    }

    // ── Cache + persistence ───────────────────────────────────────────────────

    private LimitData getPersonal(UUID uuid, String shopId, String productId) {
        return getFromCache(shopId, productId, uuid.toString());
    }

    private LimitData getGlobal(String shopId, String productId) {
        return getFromCache(shopId, productId, "global");
    }

    private LimitData getFromCache(String shopId, String productId, String key) {
        String cacheKey = shopId + ":" + productId;
        return cache.computeIfAbsent(cacheKey, k -> loadFromDisk(shopId, productId))
                .computeIfAbsent(key, k -> LimitData.empty());
    }

    private Map<String, LimitData> loadFromDisk(String shopId, String productId) {
        Map<String, LimitData> map = new HashMap<>();
        File file = getLimitFile(shopId, productId);
        if (!file.exists()) return map;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Load personal entries
        if (cfg.isConfigurationSection("personal")) {
            var sec = cfg.getConfigurationSection("personal");
            if (sec != null) {
                for (String uuid : sec.getKeys(false)) {
                    int buy = sec.getInt(uuid + ".buy", 0);
                    int sell = sec.getInt(uuid + ".sell", 0);
                    long reset = sec.getLong(uuid + ".reset", System.currentTimeMillis());
                    map.put(uuid, new LimitData(buy, sell, reset));
                }
            }
        }

        // Load global entry
        if (cfg.isConfigurationSection("global")) {
            int buy = cfg.getInt("global.buy", 0);
            int sell = cfg.getInt("global.sell", 0);
            long reset = cfg.getLong("global.reset", System.currentTimeMillis());
            map.put("global", new LimitData(buy, sell, reset));
        }

        return map;
    }

    private void saveLazy(String shopId, String productId) {
        // Defer to avoid IO on every transaction — flush on plugin disable
        // For real-time safety you could schedule a delayed task here
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                saveToDisk(shopId, productId));
    }

    private void saveToDisk(String shopId, String productId) {
        String cacheKey = shopId + ":" + productId;
        Map<String, LimitData> data = cache.get(cacheKey);
        if (data == null) return;

        File file = getLimitFile(shopId, productId);
        file.getParentFile().mkdirs();
        YamlConfiguration cfg = new YamlConfiguration();

        for (Map.Entry<String, LimitData> entry : data.entrySet()) {
            String key = entry.getKey();
            LimitData ld = entry.getValue();
            if (key.equals("global")) {
                cfg.set("global.buy", ld.getBuyCount());
                cfg.set("global.sell", ld.getSellCount());
                cfg.set("global.reset", ld.getLastResetTimestamp());
            } else {
                cfg.set("personal." + key + ".buy", ld.getBuyCount());
                cfg.set("personal." + key + ".sell", ld.getSellCount());
                cfg.set("personal." + key + ".reset", ld.getLastResetTimestamp());
            }
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save limit data for " + cacheKey, e);
        }
    }

    public void saveAll() {
        for (String cacheKey : cache.keySet()) {
            String[] parts = cacheKey.split(":", 2);
            if (parts.length == 2) saveToDisk(parts[0], parts[1]);
        }
    }

    private File getLimitFile(String shopId, String productId) {
        return new File(plugin.getDataFolder(), "limits/" + shopId + "/" + productId + ".yml");
    }
}
