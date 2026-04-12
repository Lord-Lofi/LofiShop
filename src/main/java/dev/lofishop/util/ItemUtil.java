package dev.lofishop.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ItemUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private ItemUtil() {}

    /**
     * Builds an ItemStack with a MiniMessage name and lore.
     */
    public static ItemStack buildItem(Material material, String name, List<String> lore) {
        return buildItem(material, name, lore, -1);
    }

    public static ItemStack buildItem(Material material, String name, List<String> lore, int customModelData) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (name != null && !name.isBlank()) {
            meta.displayName(MM.deserialize(name));
        }

        if (lore != null && !lore.isEmpty()) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(MM.deserialize(line));
            }
            meta.lore(loreComponents);
        }

        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Appends extra lore lines to an existing item (clone).
     */
    public static ItemStack appendLore(ItemStack original, List<Component> extraLore) {
        ItemStack copy = original.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) return copy;

        List<Component> existing = meta.lore();
        List<Component> combined = new ArrayList<>();
        if (existing != null) combined.addAll(existing);
        combined.addAll(extraLore);
        meta.lore(combined);
        copy.setItemMeta(meta);
        return copy;
    }

    /**
     * Simple item matching by material type.
     * Can be extended to match by custom model data, NBT, or display name.
     */
    public static boolean matches(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;

        // Match custom model data if set
        ItemMeta metaA = a.getItemMeta();
        ItemMeta metaB = b.getItemMeta();
        if (metaA != null && metaB != null) {
            if (metaA.hasCustomModelData() && metaB.hasCustomModelData()) {
                return metaA.getCustomModelData() == metaB.getCustomModelData();
            }
            // If only one has CMD, they don't match
            if (metaA.hasCustomModelData() != metaB.hasCustomModelData()) return false;
        }

        return true;
    }

    /** Creates a named divider/spacer item (invisible name). */
    public static ItemStack divider(Material material) {
        return buildItem(material, "<gray>─────────────────", List.of());
    }
}
