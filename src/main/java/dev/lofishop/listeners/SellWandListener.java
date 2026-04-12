package dev.lofishop.listeners;

import dev.lofishop.LofiShop;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Handles right-clicking a chest with a sell wand.
 */
public class SellWandListener implements Listener {

    private final LofiShop plugin;

    public SellWandListener(LofiShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only main hand, only right-click block
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!plugin.getSellWandManager().isSellWand(item)) return;

        // Must have permission
        if (!player.hasPermission("lofishop.sellwand.use")) {
            plugin.getMessageConfig().send(player, "no-permission");
            event.setCancelled(true);
            return;
        }

        // Must be a chest
        if (!(event.getClickedBlock().getState() instanceof Chest)) {
            plugin.getMessageConfig().send(player, "sellwand-no-chest");
            return;
        }
        Chest chest = (Chest) event.getClickedBlock().getState();

        event.setCancelled(true);

        Inventory inv;
        if (chest.getInventory().getHolder() instanceof DoubleChest) {
            inv = ((DoubleChest) chest.getInventory().getHolder()).getInventory();
        } else {
            inv = chest.getInventory();
        }

        double earned = plugin.getSellWandManager().sellChestContents(player, inv);

        if (earned < 0) {
            plugin.getMessageConfig().send(player, "sellwand-empty");
        } else {
            var econ = plugin.getEconomyManager().getDefault();
            plugin.getMessageConfig().send(player, "sellwand-use", Map.of(
                    "amount", countItems(inv),
                    "price", econ.format(earned),
                    "currency", econ.getCurrencyName()
            ));

            // Consume a use
            int remaining = plugin.getSellWandManager().getRemainingUses(item);
            if (remaining == 0) {
                // Wand broke
                player.getInventory().setItemInMainHand(null);
                plugin.getMessageConfig().send(player, "sellwand-broken");
            }
        }
    }

    private String countItems(Inventory inv) {
        int count = 0;
        for (ItemStack i : inv.getContents()) {
            if (i != null) count += i.getAmount();
        }
        return String.valueOf(count);
    }
}
