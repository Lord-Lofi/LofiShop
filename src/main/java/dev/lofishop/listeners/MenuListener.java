package dev.lofishop.listeners;

import dev.lofishop.LofiShop;
import dev.lofishop.action.ActionManager;
import dev.lofishop.action.ConditionChecker;
import dev.lofishop.api.economy.EconomyProvider;
import dev.lofishop.integration.LofiBoxHook;
import dev.lofishop.gui.ShopMenu;
import dev.lofishop.shop.Shop;
import dev.lofishop.shop.ShopProduct;
import dev.lofishop.shop.ShopPrice;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Handles all click events inside LofiShop GUIs.
 *
 * Left-click  = buy
 * Right-click = sell
 * Shift+click = buy/sell max stack
 */
public class MenuListener implements Listener {

    private final LofiShop plugin;
    private final ActionManager actionManager;
    private final ConditionChecker conditionChecker;

    public MenuListener(LofiShop plugin) {
        this.plugin = plugin;
        this.actionManager = new ActionManager(plugin);
        this.conditionChecker = new ConditionChecker(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!plugin.getMenuManager().hasOpenShop(player)) return;

        String shopId = plugin.getMenuManager().getOpenShopId(player);
        if (shopId == null || shopId.equals("__quicksell__")) return;

        // ── Creator GUI — handled entirely by ShopCreatorListener ──────────
        if (shopId.startsWith("__creator_")) return;

        // ── Quantity picker menu click ──────────────────────────────────────
        if (shopId.startsWith(dev.lofishop.gui.QuantityPickerMenu.PICKER_TAG)) {
            event.setCancelled(true);
            handlePickerClick(player, shopId, event.getRawSlot(), event.getInventory().getSize());
            return;
        }

        // ── Small shop GUI click ────────────────────────────────────────────
        if (shopId.startsWith(dev.lofishop.gui.SmallShopGui.TAG_PREFIX)) {
            event.setCancelled(true);
            handleSmallShopClick(player, shopId, event.getRawSlot(), event.isRightClick());
            return;
        }

        event.setCancelled(true);

        Shop shop = plugin.getMenuManager().getOpenShop(player);
        if (shop == null) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= shop.getSize()) return;

        String productKey = shop.getProductKeyAt(slot);
        if (productKey == null) return;

        ShopProduct product = shop.getProduct(productKey);
        if (product == null) return;

        boolean shift = event.isShiftClick();
        boolean rightClick = event.isRightClick();

        // If multi-amount tiers are defined, open picker instead of direct transaction
        if (!rightClick && product.hasMultipleBuyAmounts()) {
            new dev.lofishop.gui.QuantityPickerMenu(plugin, player, shop, product, true).open();
            return;
        }
        if (rightClick && product.hasMultipleSellAmounts()) {
            new dev.lofishop.gui.QuantityPickerMenu(plugin, player, shop, product, false).open();
            return;
        }

        if (rightClick) {
            handleSell(player, shop, product, shift ? 64 : product.getAmount());
        } else {
            handleBuy(player, shop, product, shift ? product.getAmount() * 4 : product.getAmount());
        }
    }

    private void handleSmallShopClick(Player player, String tag, int slot, boolean rightClick) {
        dev.lofishop.gui.SmallShopGui.SmallSession session =
                dev.lofishop.gui.SmallShopGui.parseTag(tag);
        if (session == null) { player.closeInventory(); return; }

        Shop shop = plugin.getShopManager().getShop(session.shopId());
        if (shop == null) { player.closeInventory(); return; }
        ShopProduct product = shop.getProduct(session.productId());
        if (product == null) { player.closeInventory(); return; }

        if (slot == dev.lofishop.gui.SmallShopGui.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        if (slot == dev.lofishop.gui.SmallShopGui.SLOT_BUY) {
            if (product.hasMultipleBuyAmounts()) {
                new dev.lofishop.gui.QuantityPickerMenu(plugin, player, shop, product, true).open();
            } else {
                player.closeInventory();
                handleBuy(player, shop, product, product.getAmount());
            }
            return;
        }

        if (slot == dev.lofishop.gui.SmallShopGui.SLOT_SELL) {
            if (product.hasMultipleSellAmounts()) {
                new dev.lofishop.gui.QuantityPickerMenu(plugin, player, shop, product, false).open();
            } else {
                player.closeInventory();
                handleSell(player, shop, product, product.getAmount());
            }
        }
    }

    private void handlePickerClick(Player player, String sessionTag, int slot, int invSize) {
        dev.lofishop.gui.QuantityPickerMenu.PickerSession session =
                dev.lofishop.gui.QuantityPickerMenu.parseTag(sessionTag);
        if (session == null) { player.closeInventory(); return; }

        // Last slot = back button
        if (slot == invSize - 1) {
            plugin.getMenuManager().openShop(player, session.shopId());
            return;
        }

        Shop shop = plugin.getShopManager().getShop(session.shopId());
        if (shop == null) { player.closeInventory(); return; }
        ShopProduct product = shop.getProduct(session.productId());
        if (product == null) { player.closeInventory(); return; }

        java.util.List<Integer> tiers = session.isBuy()
                ? product.getBuyAmounts() : product.getSellAmounts();

        if (slot < 0 || slot >= tiers.size()) return;
        int qty = tiers.get(slot);

        player.closeInventory();

        if (session.isBuy()) {
            handleBuy(player, shop, product, qty);
        } else {
            handleSell(player, shop, product, qty);
        }
    }

    private void handleBuy(Player player, Shop shop, ShopProduct product, int amount) {
        if (!plugin.getConfig().getBoolean("general.buy-enabled", true)) {
            plugin.getMessageConfig().send(player, "buy-disabled");
            return;
        }

        // Seasonal LofiBox crate check — admins bypass
        if (!player.hasPermission("lofishop.admin")) {
            LofiBoxHook lofiBoxHook = plugin.getLofiBoxHook();
            if (lofiBoxHook != null && !lofiBoxHook.isInSeason(product.getDisplayItem())) {
                plugin.getMessageConfig().send(player, "box-out-of-season");
                return;
            }
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

        // Check conditions
        if (!conditionChecker.check(product.getBuyConditions(), player)) {
            plugin.getMessageConfig().send(player, "condition-failed");
            return;
        }

        // Check limits (admin shops bypass all limits)
        if (!shop.isAdminShop() && !player.hasPermission("lofishop.bypass.limits")
                && !plugin.getLimitManager().canBuy(player, shop.getId(), product, amount)) {
            plugin.getMessageConfig().send(player, "buy-limit-reached",
                    Map.of("item", getItemName(product)));
            return;
        }

        // Process each price (player must afford all of them)
        for (ShopPrice price : product.getBuyPrices()) {
            EconomyProvider econ = plugin.getEconomyManager().getProvider(price.getEconomyId());
            double total = price.getAmount() * amount;
            if (!econ.has(player, total)) {
                plugin.getMessageConfig().send(player, "buy-failed-money", Map.of(
                        "price", econ.format(total),
                        "currency", econ.getCurrencyName()
                ));
                return;
            }
        }

        // Deduct all prices
        for (ShopPrice price : product.getBuyPrices()) {
            EconomyProvider econ = plugin.getEconomyManager().getProvider(price.getEconomyId());
            econ.withdraw(player, price.getAmount() * amount);
        }

        // Give item (skipped when give-item: false — buy actions handle delivery)
        if (product.isGiveItem()) {
            ItemStack reward = product.getDisplayItem();
            reward.setAmount(amount);
            player.getInventory().addItem(reward);
        }

        // Record limit
        plugin.getLimitManager().recordBuy(player, shop.getId(), product, amount);

        // Credit server account if this is an admin shop
        if (shop.isAdminShop()) {
            double total = product.getPrimaryBuyPrice().getAmount() * amount;
            plugin.getServerAccount().credit(total);
        }

        // Actions
        ShopPrice primary = product.getPrimaryBuyPrice();
        EconomyProvider primaryEcon = plugin.getEconomyManager()
                .getProvider(primary.getEconomyId());
        actionManager.execute(product.getBuyActions(), player, Map.of(
                "amount", String.valueOf(amount),
                "item", getItemName(product),
                "price", primaryEcon.format(primary.getAmount() * amount),
                "currency", primaryEcon.getCurrencyName(),
                "shop", shop.getName()
        ));

        // Success message
        plugin.getMessageConfig().send(player, "buy-success", Map.of(
                "amount", String.valueOf(amount),
                "item", getItemName(product),
                "price", primaryEcon.format(primary.getAmount() * amount),
                "currency", primaryEcon.getCurrencyName()
        ));

        if (plugin.getConfig().getBoolean("general.log-transactions", true)) {
            plugin.getLogger().info("[Buy] " + player.getName() + " bought x" + amount +
                    " " + getItemName(product) + " from " + shop.getId());
        }

        // Refresh shop so limit counts update
        final Shop refreshShop = shop;
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                new ShopMenu(plugin, refreshShop).open(player), 1L);
    }

    private void handleSell(Player player, Shop shop, ShopProduct product, int requestedAmount) {
        if (!plugin.getConfig().getBoolean("general.sell-enabled", true)) {
            plugin.getMessageConfig().send(player, "sell-disabled");
            return;
        }
        if (!player.hasPermission("lofishop.sell")
                && !player.hasPermission("lofishop.sell." + shop.getId())
                && !player.hasPermission("lofishop.sell." + shop.getId() + "." + product.getId())) {
            plugin.getMessageConfig().send(player, "no-permission");
            return;
        }
        if (!product.isSellable()) {
            plugin.getMessageConfig().send(player, "product-not-sellable");
            return;
        }

        // Check conditions
        if (!conditionChecker.check(product.getSellConditions(), player)) {
            plugin.getMessageConfig().send(player, "condition-failed");
            return;
        }

        // Count how many the player actually has
        int playerHas = countInInventory(player, product.getDisplayItem());
        if (playerHas <= 0) {
            plugin.getMessageConfig().send(player, "sell-failed-no-item",
                    Map.of("item", getItemName(product)));
            return;
        }

        int amount = Math.min(requestedAmount, playerHas);

        // Check limits
        if (!plugin.getLimitManager().canSell(player, shop.getId(), product, amount)) {
            amount = plugin.getLimitManager().remainingSells(player, shop.getId(), product);
            if (amount <= 0) {
                plugin.getMessageConfig().send(player, "sell-limit-reached",
                        Map.of("item", getItemName(product)));
                return;
            }
        }

        // Remove items from inventory
        removeFromInventory(player, product.getDisplayItem(), amount);

        // Pay player
        ShopPrice primary = product.getPrimarySellPrice();
        EconomyProvider econ = plugin.getEconomyManager().getProvider(primary.getEconomyId());
        double earned = primary.getAmount() * amount;
        econ.deposit(player, earned);

        // Record limit
        plugin.getLimitManager().recordSell(player, shop.getId(), product, amount);

        // Debit server account if this is an admin shop
        if (shop.isAdminShop()) {
            plugin.getServerAccount().debit(earned);
        }

        // Actions
        actionManager.execute(product.getSellActions(), player, Map.of(
                "amount", String.valueOf(amount),
                "item", getItemName(product),
                "price", econ.format(earned),
                "currency", econ.getCurrencyName(),
                "shop", shop.getName()
        ));

        // Success message
        plugin.getMessageConfig().send(player, "sell-success", Map.of(
                "amount", String.valueOf(amount),
                "item", getItemName(product),
                "price", econ.format(earned),
                "currency", econ.getCurrencyName()
        ));

        if (plugin.getConfig().getBoolean("general.log-transactions", true)) {
            plugin.getLogger().info("[Sell] " + player.getName() + " sold x" + amount +
                    " " + getItemName(product) + " in " + shop.getId());
        }

        // Refresh shop so limit counts update
        final Shop refreshShop = shop;
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                new ShopMenu(plugin, refreshShop).open(player), 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            plugin.getMenuManager().closeShop((Player) event.getPlayer());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int countInInventory(Player player, ItemStack target) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == target.getType()) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeFromInventory(Player player, ItemStack target, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != target.getType()) continue;
            if (item.getAmount() <= remaining) {
                remaining -= item.getAmount();
                player.getInventory().setItem(i, new ItemStack(Material.AIR));
            } else {
                item.setAmount(item.getAmount() - remaining);
                remaining = 0;
            }
        }
    }

    private String getItemName(ShopProduct product) {
        var meta = product.getDisplayItem().getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            // Strip MiniMessage tags for plain-text messages
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(meta.displayName());
        }
        return product.getDisplayItem().getType().name();
    }
}
