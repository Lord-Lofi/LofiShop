package dev.lofishop.integration;

import dev.lofibox.api.LofiBoxAPI;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class LofiBoxHook {

    @SuppressWarnings("deprecation")
    private static final NamespacedKey BOX_KEY = new NamespacedKey("lofibox", "box_id");

    private final LofiBoxAPI api;

    public LofiBoxHook() {
        this.api = LofiBoxAPI.get();
    }

    public boolean isAvailable() {
        return api != null;
    }

    /** Returns the LofiBox crate ID from an item's PDC, or null if not a crate. */
    public String getBoxId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(BOX_KEY, PersistentDataType.STRING);
    }

    /**
     * Returns true if the item is in season (or is not a LofiBox crate at all).
     * Returns false only when the item is a LofiBox crate that is currently out of season.
     */
    public boolean isInSeason(ItemStack item) {
        if (api == null) return true;
        String boxId = getBoxId(item);
        if (boxId == null) return true;
        return api.isBoxAvailableNow(boxId);
    }

    /**
     * Returns a human-readable season window string (e.g. "Oct 1 – Nov 15"),
     * or null if the item is not a seasonal crate.
     */
    public String getSeasonWindowDisplay(ItemStack item) {
        if (api == null) return null;
        String boxId = getBoxId(item);
        if (boxId == null) return null;
        LofiBoxAPI.SeasonWindow window = api.getBoxPrimarySeasonWindow(boxId);
        return window == null ? null : window.toDisplayString();
    }
}
