package dev.lofishop.listeners;

import dev.lofishop.LofiShop;
import dev.lofishop.gui.QuickSellMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles clicks inside the quick-sell confirmation menu.
 */
public class QuickSellListener implements Listener {

    private final LofiShop plugin;

    // We store the pending QuickSellMenu per player so we can execute on confirm
    private final java.util.Map<java.util.UUID, QuickSellMenu> pending = new java.util.HashMap<>();

    public QuickSellListener(LofiShop plugin) {
        this.plugin = plugin;
    }

    public void setPending(Player player, QuickSellMenu menu) {
        pending.put(player.getUniqueId(), menu);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String shopId = plugin.getMenuManager().getOpenShopId(player);
        if (!"__quicksell__".equals(shopId)) return;

        event.setCancelled(true);

        int confirmSlot = plugin.getConfig().getInt("quick-sell.confirm-slot", 31);
        int cancelSlot  = plugin.getConfig().getInt("quick-sell.cancel-slot", 29);
        int slot = event.getRawSlot();

        if (slot == confirmSlot) {
            QuickSellMenu menu = pending.remove(player.getUniqueId());
            if (menu != null) menu.executeSell();
            player.closeInventory();
        } else if (slot == cancelSlot) {
            pending.remove(player.getUniqueId());
            player.closeInventory();
        }
    }
}
