package dev.lofishop.gui;

import dev.lofishop.LofiShop;
import dev.lofishop.shop.Shop;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks which shop each player currently has open.
 */
public class MenuManager {

    private final LofiShop plugin;

    /** Player UUID → shop ID currently open */
    private final Map<UUID, String> openShops = new HashMap<>();

    public MenuManager(LofiShop plugin) {
        this.plugin = plugin;
    }

    public void openShop(Player player, String shopId) {
        Shop shop = plugin.getShopManager().getShop(shopId);
        if (shop == null) {
            plugin.getMessageConfig().send(player, "shop-not-found",
                    Map.of("shop", shopId));
            return;
        }

        if (!shop.getOpenPermission().isBlank()
                && !player.hasPermission(shop.getOpenPermission())) {
            plugin.getMessageConfig().send(player, "no-permission");
            return;
        }

        new ShopMenu(plugin, shop).open(player);
    }

    public void setOpenShop(Player player, String shopId) {
        openShops.put(player.getUniqueId(), shopId);
    }

    public void closeShop(Player player) {
        openShops.remove(player.getUniqueId());
    }

    public String getOpenShopId(Player player) {
        return openShops.get(player.getUniqueId());
    }

    public boolean hasOpenShop(Player player) {
        return openShops.containsKey(player.getUniqueId());
    }

    public Shop getOpenShop(Player player) {
        String id = openShops.get(player.getUniqueId());
        return id != null ? plugin.getShopManager().getShop(id) : null;
    }
}
