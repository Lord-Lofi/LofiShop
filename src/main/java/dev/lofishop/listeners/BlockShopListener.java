package dev.lofishop.listeners;

import dev.lofishop.LofiShop;
import dev.lofishop.action.ActionManager;
import dev.lofishop.action.ConditionChecker;
import dev.lofishop.api.economy.EconomyProvider;
import dev.lofishop.display.BlockShop;
import dev.lofishop.gui.SmallShopGui;
import dev.lofishop.shop.Shop;
import dev.lofishop.shop.ShopPrice;
import dev.lofishop.shop.ShopProduct;
import dev.lofishop.util.MessageUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Handles player interaction with physical block shops.
 *
 * Routes by mode:
 *   FULL  — opens the full shop GUI
 *   SMALL — opens the compact single-product GUI
 *   QUICK — executes an immediate buy with no GUI
 *
 * Block break by a non-admin is cancelled; admins remove the shop.
 */
public class BlockShopListener implements Listener {

    private final LofiShop plugin;
    private final ActionManager actionManager;
    private final ConditionChecker conditionChecker;

    public BlockShopListener(LofiShop plugin) {
        this.plugin = plugin;
        this.actionManager    = new ActionManager(plugin);
        this.conditionChecker = new ConditionChecker(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        // Sell wand interactions are handled by SellWandListener
        if (plugin.getSellWandManager().isSellWand(
                event.getPlayer().getInventory().getItemInMainHand())) return;

        Block block = event.getClickedBlock();
        BlockShop bs = plugin.getBlockShopManager().getAt(block.getLocation());
        if (bs == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!player.hasPermission("lofishop.open")) {
            plugin.getMessageConfig().send(player, "no-permission");
            return;
        }

        Shop shop = plugin.getShopManager().getShop(bs.getShopId());
        if (shop == null) {
            player.sendMessage(MessageUtil.parse(
                    "<red>[LofiShop] Block shop references a shop that no longer exists."));
            return;
        }

        switch (bs.getMode()) {
            case FULL:
                plugin.getMenuManager().openShop(player, bs.getShopId());
                break;

            case SMALL:
                ShopProduct smallProduct = shop.getProduct(bs.getProductId());
                if (smallProduct == null) {
                    player.sendMessage(MessageUtil.parse(
                            "<red>[LofiShop] Block shop references a product that no longer exists."));
                    return;
                }
                new SmallShopGui(plugin, player, shop, smallProduct).open();
                break;

            case QUICK:
                ShopProduct quickProduct = shop.getProduct(bs.getProductId());
                if (quickProduct == null) {
                    player.sendMessage(MessageUtil.parse(
                            "<red>[LofiShop] Block shop references a product that no longer exists."));
                    return;
                }
                handleQuickBuy(player, shop, quickProduct);
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!plugin.getBlockShopManager().isBlockShop(block.getLocation())) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("lofishop.admin")) {
            event.setCancelled(true);
            plugin.getMessageConfig().send(player, "no-permission");
            return;
        }

        plugin.getBlockShopManager().remove(block.getLocation());
        player.sendMessage(MessageUtil.parse("<gold>[LofiShop] <gray>Block shop removed."));
    }

    // ── Quick Buy ─────────────────────────────────────────────────────────────

    /**
     * Executes an immediate purchase of the product's default amount.
     * Mirrors MenuListener.handleBuy() but sends feedback as chat messages
     * instead of opening a GUI.
     */
    private void handleQuickBuy(Player player, Shop shop, ShopProduct product) {
        if (!plugin.getConfig().getBoolean("general.buy-enabled", true)) {
            plugin.getMessageConfig().send(player, "buy-disabled");
            return;
        }
        if (!player.hasPermission("lofishop.buy")
                && !player.hasPermission("lofishop.buy." + shop.getId())
                && !player.hasPermission("lofishop.buy." + shop.getId() + "." + product.getId())) {
            plugin.getMessageConfig().send(player, "no-permission");
            return;
        }
        if (!product.isBuyable()) {
            plugin.getMessageConfig().send(player, "product-not-buyable");
            return;
        }
        if (!conditionChecker.check(product.getBuyConditions(), player)) {
            plugin.getMessageConfig().send(player, "condition-failed");
            return;
        }

        int amount = product.getAmount();

        if (!shop.isAdminShop() && !player.hasPermission("lofishop.bypass.limits")
                && !plugin.getLimitManager().canBuy(player, shop.getId(), product, amount)) {
            plugin.getMessageConfig().send(player, "buy-limit-reached",
                    Map.of("item", itemName(product)));
            return;
        }

        // Check balance
        for (ShopPrice price : product.getBuyPrices()) {
            EconomyProvider econ = plugin.getEconomyManager().getProvider(price.getEconomyId());
            double total = price.getAmount() * amount;
            if (!econ.has(player, total)) {
                plugin.getMessageConfig().send(player, "buy-failed-money", Map.of(
                        "price",    econ.format(total),
                        "currency", econ.getCurrencyName()
                ));
                return;
            }
        }

        // Deduct
        for (ShopPrice price : product.getBuyPrices()) {
            EconomyProvider econ = plugin.getEconomyManager().getProvider(price.getEconomyId());
            econ.withdraw(player, price.getAmount() * amount);
        }

        // Give item
        ItemStack reward = product.getDisplayItem();
        reward.setAmount(amount);
        player.getInventory().addItem(reward);

        plugin.getLimitManager().recordBuy(player, shop.getId(), product, amount);

        // Credit server account if this is an admin shop
        if (shop.isAdminShop()) {
            double total = product.getPrimaryBuyPrice().getAmount() * amount;
            plugin.getServerAccount().credit(total);
        }

        ShopPrice primary = product.getPrimaryBuyPrice();
        EconomyProvider primaryEcon = plugin.getEconomyManager()
                .getProvider(primary.getEconomyId());

        new ActionManager(plugin).execute(product.getBuyActions(), player, Map.of(
                "amount",   String.valueOf(amount),
                "item",     itemName(product),
                "price",    primaryEcon.format(primary.getAmount() * amount),
                "currency", primaryEcon.getCurrencyName(),
                "shop",     shop.getName()
        ));

        plugin.getMessageConfig().send(player, "buy-success", Map.of(
                "amount",   String.valueOf(amount),
                "item",     itemName(product),
                "price",    primaryEcon.format(primary.getAmount() * amount),
                "currency", primaryEcon.getCurrencyName()
        ));

        if (plugin.getConfig().getBoolean("general.log-transactions", true)) {
            plugin.getLogger().info("[QuickBuy] " + player.getName() + " bought x" + amount
                    + " " + itemName(product) + " from block shop (" + shop.getId() + ")");
        }
    }

    private String itemName(ShopProduct product) {
        var meta = product.getDisplayItem().getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(meta.displayName());
        }
        return product.getDisplayItem().getType().name();
    }
}
