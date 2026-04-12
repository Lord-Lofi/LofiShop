package dev.lofishop.gui;

import dev.lofishop.LofiShop;
import dev.lofishop.api.economy.EconomyProvider;
import dev.lofishop.shop.Shop;
import dev.lofishop.shop.ShopProduct;
import dev.lofishop.util.ItemUtil;
import dev.lofishop.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Quick-sell menu: displays all sellable items from the player's inventory,
 * lets them confirm the sale for a total payout.
 */
public class QuickSellMenu {

    private static final String META_KEY = "lofishop_quicksell";

    private final LofiShop plugin;
    private final Player player;
    private final Map<ItemStack, Double> sellableItems; // item → total payout
    private double totalPayout = 0;

    public QuickSellMenu(LofiShop plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.sellableItems = scanInventory();
    }

    /**
     * Scans the player's inventory for sellable items across all shops.
     */
    private Map<ItemStack, Double> scanInventory() {
        Map<ItemStack, Double> result = new LinkedHashMap<>();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            double price = getBestSellPrice(item);
            if (price <= 0) continue;

            double total = price * item.getAmount();
            result.merge(item.clone(), total, Double::sum);
            totalPayout += total;
        }

        return result;
    }

    private double getBestSellPrice(ItemStack item) {
        double best = 0;
        for (Shop shop : plugin.getShopManager().getAllShops()) {
            for (ShopProduct product : shop.getProducts().values()) {
                if (!product.isSellable()) continue;
                if (ItemUtil.matches(item, product.getDisplayItem())) {
                    if (product.getPrimarySellPrice() != null) {
                        best = Math.max(best, product.getPrimarySellPrice().getAmount());
                    }
                }
            }
        }
        return best;
    }

    public boolean hasSellableItems() {
        return !sellableItems.isEmpty();
    }

    public void open() {
        if (!hasSellableItems()) {
            plugin.getMessageConfig().send(player, "quicksell-empty");
            return;
        }

        int rows = plugin.getConfig().getInt("quick-sell.rows", 4);
        String titleRaw = plugin.getConfig().getString("quick-sell.title", "<dark_gray>Quick Sell");
        Component title = plugin.getMessageConfig().parse(titleRaw);

        Inventory inv = Bukkit.createInventory(null, rows * 9, title);

        // Fill with items (first rows * 9 - 9 slots are item display)
        int displaySlots = (rows - 1) * 9;
        int slot = 0;
        for (Map.Entry<ItemStack, Double> entry : sellableItems.entrySet()) {
            if (slot >= displaySlots) break;
            inv.setItem(slot++, entry.getKey());
        }

        // Bottom row: controls
        EconomyProvider economy = plugin.getEconomyManager().getDefault();
        int confirmSlot = plugin.getConfig().getInt("quick-sell.confirm-slot", 31);
        int cancelSlot = plugin.getConfig().getInt("quick-sell.cancel-slot", 29);

        // Total display item
        int totalSlot = rows * 9 - 5;
        ItemStack totalItem = ItemUtil.buildItem(
                Material.GOLD_INGOT,
                "<yellow>Total Payout: <gold>" + economy.format(totalPayout),
                List.of("<gray>Click Confirm to sell all items.")
        );
        inv.setItem(totalSlot, totalItem);

        // Confirm
        ItemStack confirm = ItemUtil.buildItem(
                Material.LIME_STAINED_GLASS_PANE,
                plugin.getConfig().getString("quick-sell.confirm-item-name", "<green><bold>Confirm Sale"),
                List.of("<gray>Sell all items for <gold>" + economy.format(totalPayout))
        );
        inv.setItem(confirmSlot, confirm);

        // Cancel
        ItemStack cancel = ItemUtil.buildItem(
                Material.RED_STAINED_GLASS_PANE,
                plugin.getConfig().getString("quick-sell.cancel-item-name", "<red><bold>Cancel"),
                List.of("<gray>Close without selling.")
        );
        inv.setItem(cancelSlot, cancel);

        plugin.getMenuManager().setOpenShop(player, "__quicksell__");
        player.openInventory(inv);
    }

    public void executeSell() {
        EconomyProvider economy = plugin.getEconomyManager().getDefault();
        int totalItems = 0;

        for (Map.Entry<ItemStack, Double> entry : sellableItems.entrySet()) {
            ItemStack item = entry.getKey();
            player.getInventory().removeItem(item);
            totalItems += item.getAmount();
        }

        economy.deposit(player, totalPayout);

        plugin.getMessageConfig().send(player, "sell-success", Map.of(
                "amount", String.valueOf(totalItems),
                "item", "items",
                "price", economy.format(totalPayout),
                "currency", economy.getCurrencyName()
        ));

        if (plugin.getConfig().getBoolean("general.log-transactions", true)) {
            plugin.getLogger().info("[Transaction] " + player.getName() +
                    " quick-sold " + totalItems + " items for " + economy.format(totalPayout));
        }
    }

    public double getTotalPayout() { return totalPayout; }
}
