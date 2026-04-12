package dev.lofishop.listeners;

import dev.lofishop.LofiShop;
import dev.lofishop.display.BlockShop;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Handles player interaction with physical block shops.
 * Right-click → open the shop GUI.
 * Block break → remove the block shop and display entity.
 */
public class BlockShopListener implements Listener {

    private final LofiShop plugin;

    public BlockShopListener(LofiShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        // Ignore sell wand interactions (handled by SellWandListener)
        if (plugin.getSellWandManager().isSellWand(event.getPlayer().getInventory().getItemInMainHand())) return;

        Block block = event.getClickedBlock();
        BlockShop bs = plugin.getBlockShopManager().getAt(block.getLocation());
        if (bs == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.hasPermission("lofishop.open")) {
            plugin.getMessageConfig().send(player, "no-permission");
            return;
        }

        plugin.getMenuManager().openShop(player, bs.getShopId());
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

        // Admin breaking the block removes the shop display
        plugin.getBlockShopManager().remove(block.getLocation());
        player.sendMessage(dev.lofishop.util.MessageUtil.parse(
                "<gold>[LofiShop] <gray>Block shop removed."));
    }
}
