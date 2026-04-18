package dev.lofishop.gui;

import dev.lofishop.LofiShop;
import dev.lofishop.shop.Shop;
import dev.lofishop.shop.ShopProduct;
import dev.lofishop.util.ItemUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact 3-row GUI for SMALL-mode block shops.
 *
 * Layout (27 slots):
 *   [F][F][F][F][F][F][F][F][F]   row 0 — filler
 *   [F][F][Buy][F][Item][F][Sell][F][F]   row 1
 *   [F][F][F][F][Close][F][F][F][F]   row 2
 *
 * Tag format stored in MenuManager: "__small__:<shopId>:<productId>"
 */
public class SmallShopGui {

    public static final String TAG_PREFIX = "__small__";

    public static final int SLOT_BUY   = 11;
    public static final int SLOT_ITEM  = 13;
    public static final int SLOT_SELL  = 15;
    public static final int SLOT_CLOSE = 22;

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LofiShop plugin;
    private final Player player;
    private final Shop shop;
    private final ShopProduct product;

    public SmallShopGui(LofiShop plugin, Player player, Shop shop, ShopProduct product) {
        this.plugin  = plugin;
        this.player  = player;
        this.shop    = shop;
        this.product = product;
    }

    public void open() {
        String title = "<dark_gray>[ <gold>" + shop.getName() + "</gold> ]";
        Inventory inv = Bukkit.createInventory(null, 27, MM.deserialize(title));

        // Filler
        ItemStack filler = shop.getFillerItem();
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        // Centre — product display item (clone with price lore appended)
        inv.setItem(SLOT_ITEM, buildDisplayItem());

        // Buy button
        if (product.isBuyable()) {
            String buyPrice = formatPrice(product, true);
            List<String> buyLore = new ArrayList<>();
            buyLore.add("<gray>Amount: <white>" + product.getAmount());
            buyLore.add("<gray>Price:  <gold>" + buyPrice);
            if (product.hasMultipleBuyAmounts()) {
                buyLore.add("");
                buyLore.add("<green>Click <gray>to choose quantity");
            } else {
                buyLore.add("");
                buyLore.add("<green>Left-click <gray>to buy");
            }
            inv.setItem(SLOT_BUY, ItemUtil.buildItem(Material.LIME_WOOL,
                    "<green><bold>Buy", buyLore));
        }

        // Sell button
        if (product.isSellable()) {
            String sellPrice = formatPrice(product, false);
            List<String> sellLore = new ArrayList<>();
            sellLore.add("<gray>Amount: <white>" + product.getAmount());
            sellLore.add("<gray>Price:  <gold>" + sellPrice);
            if (product.hasMultipleSellAmounts()) {
                sellLore.add("");
                sellLore.add("<red>Click <gray>to choose quantity");
            } else {
                sellLore.add("");
                sellLore.add("<red>Right-click <gray>to sell");
            }
            inv.setItem(SLOT_SELL, ItemUtil.buildItem(Material.RED_WOOL,
                    "<red><bold>Sell", sellLore));
        }

        // Close
        inv.setItem(SLOT_CLOSE, ItemUtil.buildItem(Material.BARRIER,
                "<gray>Close", List.of()));

        String tag = TAG_PREFIX + ":" + shop.getId() + ":" + product.getId();
        player.openInventory(inv);
        plugin.getMenuManager().setOpenShop(player, tag);
    }

    private ItemStack buildDisplayItem() {
        List<net.kyori.adventure.text.Component> extra = new ArrayList<>();
        MiniMessage mm = MiniMessage.miniMessage();
        extra.add(mm.deserialize(""));
        if (product.isBuyable()) {
            extra.add(mm.deserialize("<yellow>Buy:  <gold>" + formatPrice(product, true)));
        }
        if (product.isSellable()) {
            extra.add(mm.deserialize("<yellow>Sell: <gold>" + formatPrice(product, false)));
        }
        return ItemUtil.appendLore(product.getDisplayItem(), extra);
    }

    private String formatPrice(ShopProduct product, boolean buy) {
        var price = buy ? product.getPrimaryBuyPrice() : product.getPrimarySellPrice();
        if (price == null) return "N/A";
        String symbol = plugin.getConfig().getString("general.currency-symbol", "$");
        int decimals  = plugin.getConfig().getInt("general.price-decimals", 2);
        return symbol + String.format("%." + decimals + "f", price.getAmount() * product.getAmount());
    }

    // ── Tag parsing ───────────────────────────────────────────────────────────

    public record SmallSession(String shopId, String productId) {}

    public static SmallSession parseTag(String tag) {
        if (tag == null || !tag.startsWith(TAG_PREFIX + ":")) return null;
        String[] parts = tag.split(":", 3);
        if (parts.length < 3) return null;
        return new SmallSession(parts[1], parts[2]);
    }
}
