package dev.lofishop.shop;

import dev.lofishop.LofiShop;
import dev.lofishop.util.ItemUtil;
import dev.lofishop.util.NbtMatcher;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Loads and manages all shops from the shops/ directory.
 */
public class ShopManager {

    private final LofiShop plugin;
    private final Map<String, Shop> shops = new LinkedHashMap<>();

    public ShopManager(LofiShop plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        shops.clear();
        File shopsDir = new File(plugin.getDataFolder(), "shops");

        if (!shopsDir.exists()) {
            shopsDir.mkdirs();
            plugin.saveResource("shops/example.yml", false);
        }

        File[] files = shopsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                Shop shop = loadShop(file);
                if (shop != null) {
                    shops.put(shop.getId(), shop);
                    plugin.getLogger().info("Loaded shop: " + shop.getId());
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load shop: " + file.getName(), e);
            }
        }

        plugin.getLogger().info("Loaded " + shops.size() + " shop(s).");
    }

    private Shop loadShop(File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String id = file.getName().replace(".yml", "");

        String name = cfg.getString("shop-name", id);
        String title = cfg.getString("title", "<dark_gray>" + name);
        int rows = cfg.getInt("rows", 6);
        String openPerm = cfg.getString("open-permission", "");

        List<String> layout = cfg.getStringList("layout");
        boolean adminShop = cfg.getBoolean("admin-shop", false);

        // Load filler item
        ItemStack filler = loadFillerItem(cfg, "filler");
        if (filler == null) {
            // Fall back to config.yml filler
            String mat = plugin.getConfig().getString("menus.filler-material", "GRAY_STAINED_GLASS_PANE");
            String fillerName = plugin.getConfig().getString("menus.filler-name", " ");
            filler = ItemUtil.buildItem(Material.valueOf(mat), fillerName, Collections.emptyList());
        }

        // Load products
        Map<String, ShopProduct> products = new LinkedHashMap<>();
        ConfigurationSection productsSection = cfg.getConfigurationSection("products");
        if (productsSection != null) {
            for (String key : productsSection.getKeys(false)) {
                ConfigurationSection ps = productsSection.getConfigurationSection(key);
                if (ps == null) continue;
                ShopProduct product = loadProduct(key, ps);
                if (product != null) products.put(key, product);
            }
        }

        return new Shop(id, name, title, rows, openPerm, layout, products, filler, adminShop);
    }

    private ShopProduct loadProduct(String key, ConfigurationSection ps) {
        // Item
        ConfigurationSection itemSec = ps.getConfigurationSection("item");
        ItemStack display;
        if (itemSec != null) {
            display = loadItemStack(itemSec);
        } else {
            display = new ItemStack(Material.BARRIER);
        }

        int amount = ps.getInt("amount", 1);

        // Buy prices
        List<ShopPrice> buyPrices = loadPrices(ps, "buy-price");
        List<ShopPrice> sellPrices = loadPrices(ps, "sell-price");

        // Limits
        LimitSettings limits = loadLimits(ps.getConfigurationSection("limits"));

        // Actions & conditions
        List<String> buyActions = ps.getStringList("actions.buy");
        List<String> sellActions = ps.getStringList("actions.sell");
        List<String> buyConditions = ps.getStringList("conditions.buy");
        List<String> sellConditions = ps.getStringList("conditions.sell");

        // Quantity tiers (optional)
        List<Integer> buyAmounts  = ps.getIntegerList("buy-amounts");
        List<Integer> sellAmounts = ps.getIntegerList("sell-amounts");

        return new ShopProduct(key, display, amount,
                buyPrices, sellPrices, limits,
                buyActions, sellActions, buyConditions, sellConditions,
                buyAmounts, sellAmounts);
    }

    private List<ShopPrice> loadPrices(ConfigurationSection ps, String node) {
        List<ShopPrice> prices = new ArrayList<>();
        if (!ps.isList(node)) return prices;

        for (Map<?, ?> entry : ps.getMapList(node)) {
            Object rawType = entry.get("type");
            String type = rawType != null ? String.valueOf(rawType) : "vault";
            double amt = 0;
            Object rawAmt = entry.get("amount");
            if (rawAmt instanceof Number) amt = ((Number) rawAmt).doubleValue();
            prices.add(new ShopPrice(type, amt));
        }
        return prices;
    }

    private LimitSettings loadLimits(ConfigurationSection ls) {
        if (ls == null) return LimitSettings.unlimited();

        int pb = ls.getInt("personal-buy", -1);
        int ps = ls.getInt("personal-sell", -1);
        int gb = ls.getInt("global-buy", -1);
        int gs = ls.getInt("global-sell", -1);

        String resetStr = ls.getString("reset", "NEVER").toUpperCase();
        LimitSettings.ResetType reset;
        try {
            reset = LimitSettings.ResetType.valueOf(resetStr);
        } catch (IllegalArgumentException e) {
            reset = LimitSettings.ResetType.NEVER;
        }

        String cron = ls.getString("cron", null);
        return new LimitSettings(pb, ps, gb, gs, reset, cron);
    }

    private ItemStack loadItemStack(ConfigurationSection sec) {
        // Prefer base64-serialized item data (captures full PDC for custom items)
        String base64 = sec.getString("item-data");
        if (base64 != null && !base64.isBlank()) {
            ItemStack deserialized = NbtMatcher.fromBase64(base64);
            if (deserialized != null) return deserialized;
            plugin.getLogger().warning("Failed to deserialize item-data in section '"
                    + sec.getName() + "', falling back to material.");
        }

        // Vanilla item fallback: material + name + lore + custom model data
        String matName = sec.getString("material", "STONE").toUpperCase();
        Material material;
        try {
            material = Material.valueOf(matName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown material: " + matName);
            material = Material.BARRIER;
        }

        String name = sec.getString("name", "");
        // Strip price-placeholder lines — {buy_price} and {sell_price} are display
        // tokens meant for the shop GUI only; the auto-price-lore system handles them.
        List<String> rawLore = sec.getStringList("lore");
        List<String> lore = new ArrayList<>();
        for (String line : rawLore) {
            if (!line.contains("{buy_price}") && !line.contains("{sell_price}")) {
                lore.add(line);
            }
        }
        int customModelData = sec.getInt("custom-model-data", -1);

        return ItemUtil.buildItem(material, name, lore, customModelData);
    }

    private ItemStack loadFillerItem(YamlConfiguration cfg, String path) {
        if (!cfg.isConfigurationSection(path)) return null;
        ConfigurationSection sec = cfg.getConfigurationSection(path);
        if (sec == null) return null;
        String matName = sec.getString("material", "GRAY_STAINED_GLASS_PANE").toUpperCase();
        String name = sec.getString("name", " ");
        Material mat;
        try {
            mat = Material.valueOf(matName);
        } catch (IllegalArgumentException e) {
            mat = Material.GRAY_STAINED_GLASS_PANE;
        }
        return ItemUtil.buildItem(mat, name, Collections.emptyList());
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public Shop getShop(String id) { return shops.get(id); }

    public Collection<Shop> getAllShops() { return shops.values(); }

    public Set<String> getShopIds() { return shops.keySet(); }

    public boolean exists(String id) { return shops.containsKey(id); }
}
