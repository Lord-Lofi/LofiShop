package dev.lofishop.creator;

import dev.lofishop.LofiShop;
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
 * The main shop editor GUI (54 slots).
 *
 * Layout:
 *  Row 0 (slots 0–8):  shop-level controls
 *  Rows 1–4 (9–44):   product slots (up to 36 products shown here)
 *  Row 5 (45–53):     save / cancel / page navigation
 *
 * Slot map (row 0):
 *   0 - Set Name       (book)
 *   1 - Set ID         (name tag)
 *   2 - Choose Rows    (comparator — cycles 1-6)
 *   3 - Toggle Admin   (beacon / barrier)
 *   4 - Filler colour  (glass pane picker — cycles materials)
 *   8 - Close/Cancel   (red wool)
 *
 * Product area (slots 9–44):
 *   Each occupied slot = a draft product. Click to edit, shift-click to remove.
 *   First empty slot = "Add Product" button (lime wool).
 *
 * Row 5:
 *   45 - Prev page     (arrow)
 *   53 - Next page     (arrow)
 *   49 - Save & Create (nether star)
 */
public class ShopCreatorGui {

    public static final String TAG = "__creator_main__";

    // Row-0 control slots
    public static final int SLOT_NAME     = 0;
    public static final int SLOT_ID       = 1;
    public static final int SLOT_ROWS     = 2;
    public static final int SLOT_ADMIN    = 3;
    public static final int SLOT_FILLER   = 4;
    public static final int SLOT_CANCEL   = 8;

    // Bottom row
    public static final int SLOT_PREV     = 45;
    public static final int SLOT_SAVE     = 49;
    public static final int SLOT_NEXT     = 53;

    // Product area
    public static final int PRODUCT_START = 9;
    public static final int PRODUCT_END   = 44;   // inclusive

    private static final String[] FILLER_CYCLE = {
        "GRAY_STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE",
        "WHITE_STAINED_GLASS_PANE", "BLUE_STAINED_GLASS_PANE",
        "GREEN_STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE",
        "YELLOW_STAINED_GLASS_PANE", "PURPLE_STAINED_GLASS_PANE",
        "CYAN_STAINED_GLASS_PANE", "LIGHT_GRAY_STAINED_GLASS_PANE"
    };

    private final LofiShop plugin;
    private final Player player;
    private final ShopCreatorSession session;
    private int page = 0;

    public ShopCreatorGui(LofiShop plugin, Player player, ShopCreatorSession session) {
        this.plugin  = plugin;
        this.player  = player;
        this.session = session;
    }

    public void open() {
        Inventory inv = Bukkit.createInventory(null, 54,
                MiniMessage.miniMessage().deserialize("<dark_gray>[ <gold>Shop Creator</gold> ]"));

        populate(inv);

        plugin.getMenuManager().setOpenShop(player, TAG);
        session.setState(ShopCreatorSession.State.MAIN_EDITOR);
        player.openInventory(inv);
    }

    private void populate(Inventory inv) {
        // ── Row 0 controls ────────────────────────────────────────────────────
        inv.setItem(SLOT_NAME, ItemUtil.buildItem(Material.WRITABLE_BOOK,
                "<yellow>Shop Name",
                List.of("<gray>Current: <white>" + session.getShopName(),
                        "", "<green>Click <gray>to change (type in chat)")));

        inv.setItem(SLOT_ID, ItemUtil.buildItem(Material.NAME_TAG,
                "<yellow>Shop ID",
                List.of("<gray>Current: <white>" + session.getShopId(),
                        "<gray>(file name, no spaces)",
                        "", "<green>Click <gray>to change (type in chat)")));

        inv.setItem(SLOT_ROWS, ItemUtil.buildItem(Material.COMPARATOR,
                "<yellow>Shop Rows: <white>" + session.getRows(),
                List.of("<gray>Rows = inventory height (1-6)",
                        "", "<green>Click <gray>to cycle")));

        boolean admin = session.isAdminShop();
        inv.setItem(SLOT_ADMIN, ItemUtil.buildItem(
                admin ? Material.BEACON : Material.BARRIER,
                (admin ? "<green>" : "<red>") + "Admin Shop: " + (admin ? "ON" : "OFF"),
                List.of("<gray>Admin shops have infinite stock",
                        "<gray>and bypass all limits.",
                        "", "<green>Click <gray>to toggle")));

        Material fillerMat;
        try { fillerMat = Material.valueOf(session.getFillerMaterial()); }
        catch (Exception e) { fillerMat = Material.GRAY_STAINED_GLASS_PANE; }
        inv.setItem(SLOT_FILLER, ItemUtil.buildItem(fillerMat,
                "<yellow>Filler Item: <white>" + prettyMat(session.getFillerMaterial()),
                List.of("<green>Click <gray>to cycle colours")));

        inv.setItem(SLOT_CANCEL, ItemUtil.buildItem(Material.RED_WOOL,
                "<red><bold>Cancel",
                List.of("<gray>Discard and close.")));

        // ── Product area ──────────────────────────────────────────────────────
        int productsPerPage = PRODUCT_END - PRODUCT_START + 1; // 36
        List<ProductDraft> products = session.getProducts();
        int start = page * productsPerPage;

        for (int i = 0; i < productsPerPage; i++) {
            int slot = PRODUCT_START + i;
            int idx  = start + i;

            if (idx < products.size()) {
                ProductDraft draft = products.get(idx);
                inv.setItem(slot, buildProductSlot(draft));
            } else if (idx == products.size()) {
                // "Add Product" button
                inv.setItem(slot, ItemUtil.buildItem(Material.LIME_WOOL,
                        "<green><bold>+ Add Product",
                        List.of("<gray>Click to create a new product.")));
            }
            // else: leave slot empty
        }

        // ── Bottom row ────────────────────────────────────────────────────────
        if (page > 0) {
            inv.setItem(SLOT_PREV, ItemUtil.buildItem(Material.ARROW,
                    "<gray>Previous Page", List.of()));
        }

        int totalPages = Math.max(1, (int) Math.ceil((products.size() + 1.0) / productsPerPage));
        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, ItemUtil.buildItem(Material.ARROW,
                    "<gray>Next Page", List.of()));
        }

        inv.setItem(SLOT_SAVE, ItemUtil.buildItem(Material.NETHER_STAR,
                "<green><bold>Save & Create Shop",
                List.of("<gray>Products: <white>" + products.size(),
                        "<gray>ID: <white>" + session.getShopId(),
                        "", products.isEmpty()
                                ? "<red>Add at least one product first."
                                : "<green>Click to save to shops/" + session.getShopId() + ".yml")));
    }

    private ItemStack buildProductSlot(ProductDraft draft) {
        if (draft.getItem() == null) {
            return ItemUtil.buildItem(Material.PAPER,
                    "<yellow>Product: <white>" + draft.getKey(),
                    List.of("<red>No item set yet.", "", "<green>Click <gray>to edit",
                            "<red>Shift+click <gray>to remove"));
        }

        ItemStack base = draft.getItem().clone();
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Key: <white>" + draft.getKey());
        if (draft.getBuyPrice() >= 0)
            lore.add("<gray>Buy: <gold>$" + draft.getBuyPrice());
        else
            lore.add("<gray>Buy: <red>not buyable");
        if (draft.getSellPrice() >= 0)
            lore.add("<gray>Sell: <gold>$" + draft.getSellPrice());
        else
            lore.add("<gray>Sell: <red>not sellable");
        lore.add("<gray>Amount: <white>" + draft.getAmount());
        if (!draft.getBuyAmounts().isEmpty())
            lore.add("<gray>Buy tiers: <white>" + draft.getBuyAmounts());
        lore.add("");
        lore.add("<green>Click <gray>to edit");
        lore.add("<red>Shift+click <gray>to remove");

        return ItemUtil.appendLore(base, lore.stream()
                .map(s -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(s))
                .collect(java.util.stream.Collectors.toList()));
    }

    private String prettyMat(String mat) {
        return mat.replace("_STAINED_GLASS_PANE", "").replace("_", " ").toLowerCase();
    }

    // ── Helpers for click handling (called from ShopCreatorListener) ──────────

    public void cycleFiller() {
        String cur = session.getFillerMaterial();
        for (int i = 0; i < FILLER_CYCLE.length; i++) {
            if (FILLER_CYCLE[i].equals(cur)) {
                session.setFillerMaterial(FILLER_CYCLE[(i + 1) % FILLER_CYCLE.length]);
                return;
            }
        }
        session.setFillerMaterial(FILLER_CYCLE[0]);
    }

    public void cycleRows() {
        session.setRows(session.getRows() % 6 + 1);
    }

    public int getPage() { return page; }
    public void setPage(int p) { this.page = p; }

    public int productsPerPage() { return PRODUCT_END - PRODUCT_START + 1; }
}
