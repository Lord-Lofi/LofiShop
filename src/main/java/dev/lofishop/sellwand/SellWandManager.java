package dev.lofishop.sellwand;

import dev.lofishop.LofiShop;
import dev.lofishop.api.economy.EconomyProvider;
import dev.lofishop.shop.Shop;
import dev.lofishop.shop.ShopProduct;
import dev.lofishop.util.ItemUtil;
import dev.lofishop.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Manages sell wand creation, identification, and use.
 * Wand data is stored in item PDC to survive server restarts.
 */
public class SellWandManager {

    private static final String PDC_WAND_KEY = "lofishop_sellwand";
    private static final String PDC_USES_KEY = "lofishop_sellwand_uses";

    private final LofiShop plugin;
    private final NamespacedKey wandKey;
    private final NamespacedKey usesKey;

    public SellWandManager(LofiShop plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, PDC_WAND_KEY);
        this.usesKey = new NamespacedKey(plugin, PDC_USES_KEY);
    }

    /** Creates and returns a new sell wand ItemStack. */
    public ItemStack createWand() {
        String matName = plugin.getConfig()
                .getString("sell-wand.material", "BLAZE_ROD").toUpperCase();
        Material material;
        try {
            material = Material.valueOf(matName);
        } catch (IllegalArgumentException e) {
            material = Material.BLAZE_ROD;
        }

        String name = plugin.getConfig().getString("sell-wand.name", "<gold><bold>Sell Wand");
        List<String> loreStrings = plugin.getConfig().getStringList("sell-wand.lore");
        int maxUses = plugin.getConfig().getInt("sell-wand.max-uses", -1);

        // Build lore
        List<Component> lore = new ArrayList<>();
        for (String line : loreStrings) {
            lore.add(MessageUtil.parse(line));
        }
        if (maxUses > 0) {
            lore.add(MessageUtil.parse("<gray>Uses: <white>" + maxUses + "<gray>/" + maxUses));
        }

        ItemStack wand = ItemUtil.buildItem(material, name, loreStrings);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            if (maxUses > 0) {
                meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, maxUses);
            }
            wand.setItemMeta(meta);
        }

        return wand;
    }

    /** Returns true if the given item is a LofiShop sell wand. */
    public boolean isSellWand(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    /** Returns remaining uses, or -1 if unlimited. */
    public int getRemainingUses(ItemStack wand) {
        ItemMeta meta = wand.getItemMeta();
        if (meta == null) return -1;
        Integer uses = meta.getPersistentDataContainer().get(usesKey, PersistentDataType.INTEGER);
        return uses != null ? uses : -1;
    }

    /** Decrements the use count. Returns the wand (possibly null if broken). */
    public ItemStack consumeUse(ItemStack wand) {
        boolean consumeOnUse = plugin.getConfig().getBoolean("sell-wand.consume-on-use", false);
        if (consumeOnUse) {
            wand.setAmount(wand.getAmount() - 1);
            return wand.getAmount() <= 0 ? null : wand;
        }

        int remaining = getRemainingUses(wand);
        if (remaining < 0) return wand; // unlimited

        remaining--;
        if (remaining <= 0) {
            plugin.getMessageConfig().send(
                    (Player) null, "sellwand-broken" // sent by caller
            );
            return null;
        }

        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, remaining);
            // Update lore to reflect new count
            wand.setItemMeta(meta);
        }
        return wand;
    }

    /**
     * Sells all sellable contents of the given chest inventory on behalf of the player.
     * Returns the total earned, or -1 if nothing was sold.
     */
    public double sellChestContents(Player player, Inventory chestInv) {
        EconomyProvider economy = plugin.getEconomyManager().getDefault();
        double totalEarned = 0;
        int totalItems = 0;

        for (int i = 0; i < chestInv.getSize(); i++) {
            ItemStack item = chestInv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            SellResult result = findBestSell(item);
            if (result == null) continue;

            // Check sell permission
            if (!player.hasPermission("lofishop.sell")) continue;

            // Check sell limit
            if (!plugin.getLimitManager().canSell(player,
                    result.shopId(), result.product(), item.getAmount())) continue;

            double earned = result.pricePerUnit() * item.getAmount();
            totalEarned += earned;
            totalItems += item.getAmount();

            plugin.getLimitManager().recordSell(player, result.shopId(), result.product(), item.getAmount());
            chestInv.setItem(i, null);
        }

        if (totalItems == 0) return -1;

        economy.deposit(player, totalEarned);

        if (plugin.getConfig().getBoolean("general.log-transactions", true)) {
            plugin.getLogger().info("[Transaction] " + player.getName() +
                    " sell-wand sold " + totalItems + " items for " + economy.format(totalEarned));
        }

        return totalEarned;
    }

    private SellResult findBestSell(ItemStack item) {
        double bestPrice = 0;
        String bestShopId = null;
        ShopProduct bestProduct = null;

        for (Shop shop : plugin.getShopManager().getAllShops()) {
            for (ShopProduct product : shop.getProducts().values()) {
                if (!product.isSellable()) continue;
                if (!ItemUtil.matches(item, product.getDisplayItem())) continue;
                if (product.getPrimarySellPrice() == null) continue;

                double price = product.getPrimarySellPrice().getAmount();
                if (price > bestPrice) {
                    bestPrice = price;
                    bestShopId = shop.getId();
                    bestProduct = product;
                }
            }
        }

        if (bestProduct == null) return null;
        return new SellResult(bestShopId, bestProduct, bestPrice);
    }

    private record SellResult(String shopId, ShopProduct product, double pricePerUnit) {}
}
