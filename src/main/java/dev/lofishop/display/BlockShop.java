package dev.lofishop.display;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Represents a physical block shop — a block in the world that opens a shop
 * UI when right-clicked, with a floating ItemDisplay entity above it.
 */
public class BlockShop {

    private final String shopId;
    private final String productId;
    private final BlockShopMode mode;
    private final Location blockLocation;

    /** UUID of the ItemDisplay entity floating above this block. */
    private UUID displayEntityId;

    public BlockShop(String shopId, String productId, BlockShopMode mode,
                     Location blockLocation, UUID displayEntityId) {
        this.shopId          = shopId;
        this.productId       = productId;
        this.mode            = mode != null ? mode : BlockShopMode.FULL;
        this.blockLocation   = blockLocation.clone();
        this.displayEntityId = displayEntityId;
    }

    public String getShopId()        { return shopId; }
    public String getProductId()     { return productId; }
    public BlockShopMode getMode()   { return mode; }
    public Location getBlockLocation() { return blockLocation.clone(); }
    public UUID getDisplayEntityId() { return displayEntityId; }
    public void setDisplayEntityId(UUID id) { this.displayEntityId = id; }

    /** Returns a unique key for map storage. */
    public String locationKey() {
        Location l = blockLocation;
        return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }

    public static String locationKey(Location l) {
        return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }
}
