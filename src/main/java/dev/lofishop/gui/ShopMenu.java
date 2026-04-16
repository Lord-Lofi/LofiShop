package dev.lofishop.gui;

import dev.lofishop.LofiShop;
import dev.lofishop.api.economy.EconomyProvider;
import dev.lofishop.shop.Shop;
import dev.lofishop.shop.ShopProduct;
import dev.lofishop.util.ItemUtil;
import dev.lofishop.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds and opens the chest GUI for a shop.
 */
public class ShopMenu {

    private final LofiShop plugin;
    private final Shop shop;

    public ShopMenu(LofiShop plugin, Shop shop) {
        this.plugin = plugin;
        this.shop = shop;
    }

    public void open(Player player) {
        Component title = plugin.getMessageConfig().parse(shop.getTitle());
        Inventory inv = Bukkit.createInventory(null, shop.getSize(), title);

        populateInventory(inv, player);

        plugin.getMenuManager().setOpenShop(player, shop.getId());
        player.openInventory(inv);
    }

    private void populateInventory(Inventory inv, Player player) {
        List<String> layout = shop.getLayout();
        EconomyProvider economy = plugin.getEconomyManager().getDefault();

        for (int slot = 0; slot < shop.getSize(); slot++) {
            int row = slot / 9;
            int col = slot % 9;

            if (row >= layout.size()) {
                setFiller(inv, slot);
                continue;
            }

            String[] cells = layout.get(row).split(" ");
            if (col >= cells.length) {
                setFiller(inv, slot);
                continue;
            }

            String key = cells[col].trim();

            switch (key) {
                case "F", "" -> setFiller(inv, slot);
                default -> {
                    ShopProduct product = shop.getProduct(key);
                    if (product == null) {
                        setFiller(inv, slot);
                    } else {
                        inv.setItem(slot, buildProductItem(product, player, economy));
                    }
                }
            }
        }
    }

    private ItemStack buildProductItem(ShopProduct product, Player player, EconomyProvider economy) {
        ItemStack base = product.getDisplayItem();

        boolean autoPrice = plugin.getConfig().getBoolean("menus.auto-price-lore", true);
        boolean autoLimit = plugin.getConfig().getBoolean("menus.auto-limit-lore", true);

        List<Component> extraLore = new ArrayList<>();

        if (autoPrice) {
            if (product.isBuyable()) {
                double price = product.getPrimaryBuyPrice().getAmount();
                EconomyProvider priceEcon = plugin.getEconomyManager()
                        .getProvider(product.getPrimaryBuyPrice().getEconomyId());
                extraLore.add(MessageUtil.parse(
                        "<yellow>Buy: <gold>" + priceEcon.format(price)));
            }
            if (product.isSellable()) {
                double price = product.getPrimarySellPrice().getAmount();
                EconomyProvider priceEcon = plugin.getEconomyManager()
                        .getProvider(product.getPrimarySellPrice().getEconomyId());
                extraLore.add(MessageUtil.parse(
                        "<yellow>Sell: <gold>" + priceEcon.format(price)));
            }
        }

        if (autoLimit && product.getLimits().hasPersonalBuyLimit()) {
            int remaining = plugin.getLimitManager()
                    .remainingBuys(player, shop.getId(), product);
            extraLore.add(MessageUtil.parse(
                    "<gray>Buy limit: <white>" + remaining + "<gray>/" +
                    product.getLimits().getPersonalBuy()));
        }

        if (!extraLore.isEmpty()) {
            return ItemUtil.appendLore(base, extraLore);
        }
        return base;
    }

    private void setFiller(Inventory inv, int slot) {
        ItemStack filler = shop.getFillerItem();
        if (filler != null) {
            inv.setItem(slot, filler);
        }
    }

    public Shop getShop() { return shop; }
}
