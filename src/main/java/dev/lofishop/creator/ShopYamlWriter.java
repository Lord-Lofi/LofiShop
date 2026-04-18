package dev.lofishop.creator;

import dev.lofishop.LofiShop;
import dev.lofishop.util.MessageUtil;
import dev.lofishop.util.NbtMatcher;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a completed {@link ShopCreatorSession} into a YAML file inside
 * plugins/LofiShop/shops/ and triggers a live reload so the shop is
 * immediately available without restarting the server.
 */
public final class ShopYamlWriter {

    private ShopYamlWriter() {}

    public static void write(LofiShop plugin, ShopCreatorSession session, Player player) {
        String shopId = session.getShopId();
        File file = new File(plugin.getDataFolder(), "shops/" + shopId + ".yml");

        // Warn if overwriting an existing shop
        boolean overwrite = file.exists();

        YamlConfiguration cfg = new YamlConfiguration();

        // ── Shop-level fields ─────────────────────────────────────────────────
        cfg.set("shop-name", "<gold>" + session.getShopName());
        cfg.set("title", "<dark_gray>[ <gold>" + session.getShopName() + "</gold> ]");
        cfg.set("rows", session.getRows());
        cfg.set("admin-shop", session.isAdminShop());
        cfg.set("open-permission", "");

        cfg.set("filler.material", session.getFillerMaterial());
        cfg.set("filler.name", " ");

        // ── Layout generation ─────────────────────────────────────────────────
        // Place products left-to-right, top-to-bottom in the inner area,
        // surrounded by a filler border.
        List<String> layout = buildLayout(session);
        cfg.set("layout", layout);

        // ── Products ──────────────────────────────────────────────────────────
        for (ProductDraft draft : session.getProducts()) {
            String base = "products." + draft.getKey();

            // Item — serialize the full item bytes so custom plugin items
            // (MMOItems, Oraxen, ItemsAdder, etc.) reload with all PDC intact.
            ItemStack item = draft.getItem();
            if (item != null) {
                String base64 = NbtMatcher.toBase64(item);
                if (base64 != null) {
                    cfg.set(base + ".item.item-data", base64);
                }
                // Also store material as a human-readable hint (not used for loading
                // when item-data is present, but helpful for manual YAML inspection).
                cfg.set(base + ".item.material", item.getType().name());

                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasCustomModelData()) {
                    cfg.set(base + ".item.custom-model-data", meta.getCustomModelData());
                }
            }

            cfg.set(base + ".amount", draft.getAmount());

            // Buy/sell tiers
            if (!draft.getBuyAmounts().isEmpty()) {
                cfg.set(base + ".buy-amounts", draft.getBuyAmounts());
            }
            if (!draft.getSellAmounts().isEmpty()) {
                cfg.set(base + ".sell-amounts", draft.getSellAmounts());
            }

            // Buy price
            if (draft.getBuyPrice() >= 0) {
                List<java.util.Map<String, Object>> buyPrices = new ArrayList<>();
                java.util.Map<String, Object> bp = new java.util.LinkedHashMap<>();
                bp.put("type", "vault");
                bp.put("amount", draft.getBuyPrice());
                buyPrices.add(bp);
                cfg.set(base + ".buy-price", buyPrices);
            }

            // Sell price
            if (draft.getSellPrice() >= 0) {
                List<java.util.Map<String, Object>> sellPrices = new ArrayList<>();
                java.util.Map<String, Object> sp = new java.util.LinkedHashMap<>();
                sp.put("type", "vault");
                sp.put("amount", draft.getSellPrice());
                sellPrices.add(sp);
                cfg.set(base + ".sell-price", sellPrices);
            }

            // Limits
            cfg.set(base + ".limits.personal-buy",  draft.getPersonalBuyLimit());
            cfg.set(base + ".limits.personal-sell", draft.getPersonalSellLimit());
            cfg.set(base + ".limits.global-buy",    -1);
            cfg.set(base + ".limits.global-sell",   -1);
            cfg.set(base + ".limits.reset",          draft.getResetType());

            // Default empty actions/conditions
            cfg.set(base + ".actions.buy",  new ArrayList<>());
            cfg.set(base + ".actions.sell", new ArrayList<>());
            cfg.set(base + ".conditions.buy",  new ArrayList<>());
            cfg.set(base + ".conditions.sell", new ArrayList<>());
        }

        // ── Save ──────────────────────────────────────────────────────────────
        try {
            file.getParentFile().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save shop " + shopId + ": " + e.getMessage());
            player.sendMessage(MessageUtil.parse(
                    "<red>[LofiShop] Failed to save shop. Check console."));
            return;
        }

        // ── Handle rename: delete old file + update block shop references ────
        String originalId = session.getOriginalShopId();
        if (originalId != null && !originalId.equals(shopId)) {
            File oldFile = new File(plugin.getDataFolder(), "shops/" + originalId + ".yml");
            if (oldFile.exists()) oldFile.delete();
            plugin.getBlockShopManager().renameShop(originalId, shopId);
        }

        // ── Reload so the shop is live immediately ────────────────────────────
        plugin.getShopManager().reload();

        String verb = (originalId != null && !originalId.equals(shopId))
                ? "<yellow>(renamed from " + originalId + ")"
                : overwrite ? "<yellow>(updated)" : "<green>(created)";
        player.sendMessage(MessageUtil.parse(
                "<gold>[LofiShop] <green>Shop <white>" + shopId + " " + verb
                + " <green>saved and loaded! Use <white>/shop open " + shopId
                + " <green>to test it."));
    }

    /**
     * Builds a row-based layout string list. Products are placed in a centred
     * grid inside the shop's row count, with F (filler) padding the border.
     *
     * For a 6-row shop with 8 products the inner area is rows 1-4 = 36 slots.
     */
    private static List<String> buildLayout(ShopCreatorSession session) {
        int rows = session.getRows();
        int totalSlots = rows * 9;

        // Build a flat char array for all slots, default = 'F'
        String[] slots = new String[totalSlots];
        for (int i = 0; i < totalSlots; i++) slots[i] = "F";

        // Determine inner slots: skip first and last row if rows >= 3,
        // skip first and last column always
        List<Integer> innerSlots = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            if (rows >= 3 && (row == 0 || row == rows - 1)) continue;
            for (int col = 1; col <= 7; col++) {
                innerSlots.add(row * 9 + col);
            }
        }

        // Fill inner slots with products
        List<ProductDraft> products = session.getProducts();
        for (int i = 0; i < products.size() && i < innerSlots.size(); i++) {
            slots[innerSlots.get(i)] = products.get(i).getKey();
        }

        // Convert flat array to row strings
        List<String> layout = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            StringBuilder sb = new StringBuilder();
            for (int col = 0; col < 9; col++) {
                if (col > 0) sb.append(' ');
                sb.append(slots[row * 9 + col]);
            }
            layout.add(sb.toString());
        }
        return layout;
    }
}
