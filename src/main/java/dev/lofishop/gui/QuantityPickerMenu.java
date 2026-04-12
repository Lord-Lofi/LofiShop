package dev.lofishop.gui;

import dev.lofishop.LofiShop;
import dev.lofishop.api.economy.EconomyProvider;
import dev.lofishop.shop.Shop;
import dev.lofishop.shop.ShopPrice;
import dev.lofishop.shop.ShopProduct;
import dev.lofishop.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Sub-menu shown when a product has multiple quantity tiers defined.
 * Each tier occupies a slot showing the item with the per-tier price.
 *
 * Left-click a tier = buy that quantity.
 * Right-click a tier = sell that quantity.
 */
public class QuantityPickerMenu {

    /** Tag stored in MenuManager to identify this as a picker menu. */
    public static final String PICKER_TAG = "__picker__";

    private final LofiShop plugin;
    private final Player player;
    private final Shop shop;
    private final ShopProduct product;
    private final boolean isBuy;       // true = buy tiers, false = sell tiers
    private final List<Integer> tiers;

    public QuantityPickerMenu(LofiShop plugin, Player player, Shop shop,
                              ShopProduct product, boolean isBuy) {
        this.plugin  = plugin;
        this.player  = player;
        this.shop    = shop;
        this.product = product;
        this.isBuy   = isBuy;
        this.tiers   = isBuy ? product.getBuyAmounts() : product.getSellAmounts();
    }

    public void open() {
        // Pick smallest fitting inventory: 9, 18, 27
        int size = tiers.size() <= 9 ? 9 : tiers.size() <= 18 ? 18 : 27;
        String mode = isBuy ? "Buy" : "Sell";
        Component title = plugin.getMessageConfig().parse(
                "<dark_gray>[ <gold>" + mode + "</gold>: " + getProductName() + " ]");

        Inventory inv = Bukkit.createInventory(null, size, title);

        EconomyProvider econ = plugin.getEconomyManager().getDefault();
        List<ShopPrice> prices = isBuy ? product.getBuyPrices() : product.getSellPrices();
        ShopPrice primary = prices.isEmpty() ? null : prices.get(0);

        for (int i = 0; i < tiers.size() && i < size; i++) {
            int qty = tiers.get(i);
            double total = primary != null ? primary.getAmount() * qty : 0;

            List<String> lore = new ArrayList<>();
            lore.add("<gray>Amount: <white>x" + qty);
            if (primary != null) {
                lore.add("<gray>Total: <gold>" + econ.format(total));
            }
            lore.add("");
            lore.add(isBuy
                    ? "<green>Left-click <gray>to buy x" + qty
                    : "<yellow>Left-click <gray>to sell x" + qty);

            ItemStack tierItem = ItemUtil.buildItem(
                    product.getDisplayItem().getType(),
                    "<yellow>x" + qty + " <white>" + getProductName(),
                    lore
            );
            tierItem.setAmount(Math.min(qty, 64));
            inv.setItem(i, tierItem);
        }

        // Back button in last slot
        inv.setItem(size - 1, ItemUtil.buildItem(
                Material.ARROW,
                "<red>Back",
                List.of("<gray>Return to shop.")));

        plugin.getMenuManager().setOpenShop(player, PICKER_TAG + ":" + shop.getId()
                + ":" + product.getId() + ":" + (isBuy ? "buy" : "sell"));
        player.openInventory(inv);
    }

    private String getProductName() {
        var meta = product.getDisplayItem().getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(meta.displayName());
        }
        return product.getDisplayItem().getType().name();
    }

    /** Parses a picker menu session tag into its parts. */
    public static PickerSession parseTag(String tag) {
        // "__picker__:<shopId>:<productId>:<buy|sell>"
        if (tag == null || !tag.startsWith(PICKER_TAG)) return null;
        String[] parts = tag.split(":");
        if (parts.length < 4) return null;
        return new PickerSession(parts[1], parts[2], parts[3].equals("buy"));
    }

    public record PickerSession(String shopId, String productId, boolean isBuy) {}
}
