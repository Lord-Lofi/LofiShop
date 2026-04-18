package dev.lofishop.display;

import dev.lofishop.LofiShop;
import dev.lofishop.shop.Shop;
import dev.lofishop.shop.ShopProduct;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages physical block shops — blocks in the world that display a floating
 * item above them and open a shop UI when right-clicked.
 *
 * Three modes (set at creation time):
 *   FULL  — opens the full shop GUI (default)
 *   SMALL — opens a compact single-product GUI
 *   QUICK — instantly purchases with no GUI
 *
 * Data is persisted to plugins/LofiShop/blockshops.yml.
 *
 * Commands:
 *   /shop createblock <shopId> <productKey> [FULL|SMALL|QUICK]
 *   /shop removeblock
 */
public class BlockShopManager {

    private static final float DISPLAY_OFFSET_Y = 1.35f;
    private static final float DISPLAY_SCALE     = 0.6f;

    private final LofiShop plugin;
    private final Map<String, BlockShop> blockShops = new HashMap<>();
    private File dataFile;

    public BlockShopManager(LofiShop plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "blockshops.yml");
        load();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /**
     * Registers a block as a shop display with the given mode.
     * Spawns the floating ItemDisplay entity.
     */
    public boolean create(Location blockLoc, String shopId, String productId, BlockShopMode mode) {
        Shop shop = plugin.getShopManager().getShop(shopId);
        if (shop == null) return false;
        ShopProduct product = shop.getProduct(productId);
        if (product == null) return false;

        String key = BlockShop.locationKey(blockLoc);
        if (blockShops.containsKey(key)) {
            remove(blockLoc); // replace existing
        }

        UUID entityId = spawnDisplay(blockLoc, product.getDisplayItem());
        BlockShop bs = new BlockShop(shopId, productId, mode, blockLoc, entityId);
        blockShops.put(key, bs);
        save();
        return true;
    }

    /** Removes the block shop at the given location and kills the display entity. */
    public boolean remove(Location blockLoc) {
        String key = BlockShop.locationKey(blockLoc);
        BlockShop bs = blockShops.remove(key);
        if (bs == null) return false;
        killDisplay(bs.getDisplayEntityId());
        save();
        return true;
    }

    /** Returns the BlockShop for the given location, or null. */
    public BlockShop getAt(Location loc) {
        return blockShops.get(BlockShop.locationKey(loc));
    }

    public boolean isBlockShop(Location loc) {
        return blockShops.containsKey(BlockShop.locationKey(loc));
    }

    // ── Entity management ─────────────────────────────────────────────────────

    private UUID spawnDisplay(Location blockLoc, ItemStack item) {
        Location spawnLoc = blockLoc.clone().add(0.5, DISPLAY_OFFSET_Y, 0.5);
        spawnLoc.setYaw(0);
        spawnLoc.setPitch(0);

        World world = spawnLoc.getWorld();
        if (world == null) return null;

        ItemDisplay display = (ItemDisplay) world.spawnEntity(spawnLoc, EntityType.ITEM_DISPLAY);
        display.setItemStack(item);
        display.setPersistent(true);
        display.setInvulnerable(true);
        display.setSilent(true);

        Transformation transform = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 1, 0),
                new Vector3f(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE),
                new AxisAngle4f(0, 0, 1, 0)
        );
        display.setTransformation(transform);
        display.setDisplayWidth(1.0f);
        display.setDisplayHeight(1.0f);

        startRotation(display);
        return display.getUniqueId();
    }

    private void startRotation(ItemDisplay display) {
        final UUID id = display.getUniqueId();
        final float[] yaw = {0f};

        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Entity entity = Bukkit.getEntity(id);
            if (!(entity instanceof ItemDisplay)) return;
            ItemDisplay d = (ItemDisplay) entity;
            if (!d.isValid()) return;

            yaw[0] += 2f;
            if (yaw[0] >= 360f) yaw[0] = 0f;

            float radians = (float) Math.toRadians(yaw[0]);
            Transformation t = new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(radians, 0, 1, 0),
                    new Vector3f(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE),
                    new AxisAngle4f(0, 0, 1, 0)
            );
            d.setTransformation(t);
            d.setInterpolationDuration(1);
            d.setInterpolationDelay(0);
        }, 0L, 1L);
    }

    private void killDisplay(UUID entityId) {
        if (entityId == null) return;
        Entity entity = Bukkit.getEntity(entityId);
        if (entity != null) entity.remove();
    }

    /** Re-spawns display entities for all loaded block shops on startup. */
    public void respawnAll() {
        for (BlockShop bs : new ArrayList<>(blockShops.values())) {
            Location loc = bs.getBlockLocation();
            if (loc.getWorld() == null) continue;

            if (bs.getDisplayEntityId() != null) {
                killDisplay(bs.getDisplayEntityId());
            }

            Shop shop = plugin.getShopManager().getShop(bs.getShopId());
            if (shop == null) continue;
            ShopProduct product = shop.getProduct(bs.getProductId());
            if (product == null) continue;

            UUID newId = spawnDisplay(loc, product.getDisplayItem());
            bs.setDisplayEntityId(newId);
        }
        save();
    }

    /** Removes all display entities (called on plugin disable). */
    public void removeAllDisplays() {
        for (BlockShop bs : blockShops.values()) {
            killDisplay(bs.getDisplayEntityId());
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection sec = cfg.getConfigurationSection("blockshops");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            ConfigurationSection entry = sec.getConfigurationSection(key);
            if (entry == null) continue;

            String worldName = entry.getString("world");
            double x = entry.getDouble("x");
            double y = entry.getDouble("y");
            double z = entry.getDouble("z");
            String shopId   = entry.getString("shopId");
            String prodId   = entry.getString("productId");
            String uuidStr  = entry.getString("displayEntity");
            BlockShopMode mode = BlockShopMode.fromString(entry.getString("mode", "FULL"));

            World world = Bukkit.getWorld(worldName != null ? worldName : "");
            if (world == null) continue;

            Location loc = new Location(world, x, y, z);
            UUID displayId = null;
            if (uuidStr != null) {
                try { displayId = UUID.fromString(uuidStr); } catch (IllegalArgumentException ignored) {}
            }

            BlockShop bs = new BlockShop(shopId, prodId, mode, loc, displayId);
            blockShops.put(bs.locationKey(), bs);
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();

        for (Map.Entry<String, BlockShop> entry : blockShops.entrySet()) {
            BlockShop bs = entry.getValue();
            Location loc = bs.getBlockLocation();
            String base = "blockshops." + entry.getKey() + ".";

            cfg.set(base + "world",         loc.getWorld().getName());
            cfg.set(base + "x",             loc.getBlockX());
            cfg.set(base + "y",             loc.getBlockY());
            cfg.set(base + "z",             loc.getBlockZ());
            cfg.set(base + "shopId",        bs.getShopId());
            cfg.set(base + "productId",     bs.getProductId());
            cfg.set(base + "mode",          bs.getMode().name());
            if (bs.getDisplayEntityId() != null) {
                cfg.set(base + "displayEntity", bs.getDisplayEntityId().toString());
            }
        }

        try {
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save blockshops.yml", e);
        }
    }

    public Collection<BlockShop> getAll() { return blockShops.values(); }
}
