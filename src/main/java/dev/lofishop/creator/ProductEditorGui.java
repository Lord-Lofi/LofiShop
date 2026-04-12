package dev.lofishop.creator;

import dev.lofishop.LofiShop;
import dev.lofishop.util.ItemUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Per-product editor GUI (27 slots).
 *
 * Slot layout:
 *   13 - Item slot (drag your item here)
 *
 *   Row 0:
 *     0  - Set Buy Price    (gold ingot)
 *     1  - Set Sell Price   (iron ingot)
 *     2  - Set Amount       (comparator)
 *     3  - Set Buy Tiers    (ladder — comma-sep list e.g. "1,8,16,64")
 *     4  - Set Sell Tiers   (hopper)
 *     5  - Set Buy Limit    (barrier)
 *     6  - Set Sell Limit   (barrier)
 *     7  - Reset Type cycle (clock — NEVER/DAILY/WEEKLY)
 *
 *   Row 2:
 *     18 - Back             (arrow)
 *     26 - Save Product     (lime wool — validates + returns to main)
 */
public class ProductEditorGui {

    public static final String TAG = "__creator_product__";

    public static final int SLOT_ITEM        = 13;
    public static final int SLOT_BUY_PRICE   = 0;
    public static final int SLOT_SELL_PRICE  = 1;
    public static final int SLOT_AMOUNT      = 2;
    public static final int SLOT_BUY_TIERS   = 3;
    public static final int SLOT_SELL_TIERS  = 4;
    public static final int SLOT_BUY_LIMIT   = 5;
    public static final int SLOT_SELL_LIMIT  = 6;
    public static final int SLOT_RESET_TYPE  = 7;
    public static final int SLOT_BACK        = 18;
    public static final int SLOT_SAVE        = 26;

    private static final String[] RESET_CYCLE = { "NEVER", "DAILY", "WEEKLY" };

    private final LofiShop plugin;
    private final Player player;
    private final ShopCreatorSession session;
    private final ProductDraft draft;

    public ProductEditorGui(LofiShop plugin, Player player,
                            ShopCreatorSession session, ProductDraft draft) {
        this.plugin   = plugin;
        this.player   = player;
        this.session  = session;
        this.draft    = draft;
    }

    public void open() {
        Inventory inv = Bukkit.createInventory(null, 27,
                MiniMessage.miniMessage().deserialize(
                        "<dark_gray>[ <gold>Edit Product: </gold><white>" + draft.getKey() + " ]"));

        populate(inv);

        plugin.getMenuManager().setOpenShop(player, TAG);
        session.setState(ShopCreatorSession.State.PRODUCT_EDITOR);
        session.setEditingProduct(draft);
        player.openInventory(inv);
    }

    public void populate(Inventory inv) {
        // Item slot
        if (draft.getItem() != null) {
            inv.setItem(SLOT_ITEM, draft.getItem());
        } else {
            inv.setItem(SLOT_ITEM, ItemUtil.buildItem(Material.LIME_STAINED_GLASS_PANE,
                    "<green>Drag your item here",
                    List.of("<gray>This is what will be bought/sold.")));
        }

        // Controls
        String buyStr = draft.getBuyPrice() >= 0 ? "$" + draft.getBuyPrice() : "<red>None (not buyable)";
        inv.setItem(SLOT_BUY_PRICE, ItemUtil.buildItem(Material.GOLD_INGOT,
                "<yellow>Buy Price",
                List.of("<gray>Current: <white>" + buyStr,
                        "", "<green>Click <gray>to set (type in chat)",
                        "<red>Type 'none' <gray>to disable buying")));

        String sellStr = draft.getSellPrice() >= 0 ? "$" + draft.getSellPrice() : "<red>None (not sellable)";
        inv.setItem(SLOT_SELL_PRICE, ItemUtil.buildItem(Material.IRON_INGOT,
                "<yellow>Sell Price",
                List.of("<gray>Current: <white>" + sellStr,
                        "", "<green>Click <gray>to set (type in chat)",
                        "<red>Type 'none' <gray>to disable selling")));

        inv.setItem(SLOT_AMOUNT, ItemUtil.buildItem(Material.COMPARATOR,
                "<yellow>Default Amount",
                List.of("<gray>Current: <white>" + draft.getAmount(),
                        "<gray>Items per click (no tiers)",
                        "", "<green>Click <gray>to change (type in chat)")));

        String buyTiers = draft.getBuyAmounts().isEmpty() ? "<gray>None" : draft.getBuyAmounts().toString();
        inv.setItem(SLOT_BUY_TIERS, ItemUtil.buildItem(Material.LADDER,
                "<yellow>Buy Quantity Tiers",
                List.of("<gray>Current: <white>" + buyTiers,
                        "<gray>Opens picker menu on click.",
                        "<gray>Example input: <white>1,8,16,64",
                        "", "<green>Click <gray>to set | <red>type 'none' <gray>to clear")));

        String sellTiers = draft.getSellAmounts().isEmpty() ? "<gray>None" : draft.getSellAmounts().toString();
        inv.setItem(SLOT_SELL_TIERS, ItemUtil.buildItem(Material.HOPPER,
                "<yellow>Sell Quantity Tiers",
                List.of("<gray>Current: <white>" + sellTiers,
                        "<gray>Example input: <white>1,8,16,64",
                        "", "<green>Click <gray>to set | <red>type 'none' <gray>to clear")));

        String blStr = draft.getPersonalBuyLimit() < 0 ? "Unlimited" : String.valueOf(draft.getPersonalBuyLimit());
        inv.setItem(SLOT_BUY_LIMIT, ItemUtil.buildItem(Material.BARRIER,
                "<yellow>Personal Buy Limit",
                List.of("<gray>Current: <white>" + blStr,
                        "", "<green>Click <gray>to set | <red>type -1 <gray>for unlimited")));

        String slStr = draft.getPersonalSellLimit() < 0 ? "Unlimited" : String.valueOf(draft.getPersonalSellLimit());
        inv.setItem(SLOT_SELL_LIMIT, ItemUtil.buildItem(Material.BARRIER,
                "<yellow>Personal Sell Limit",
                List.of("<gray>Current: <white>" + slStr,
                        "", "<green>Click <gray>to set | <red>type -1 <gray>for unlimited")));

        inv.setItem(SLOT_RESET_TYPE, ItemUtil.buildItem(Material.CLOCK,
                "<yellow>Limit Reset: <white>" + draft.getResetType(),
                List.of("<gray>How limits reset over time.",
                        "", "<green>Click <gray>to cycle: NEVER → DAILY → WEEKLY")));

        inv.setItem(SLOT_BACK, ItemUtil.buildItem(Material.ARROW,
                "<red>Back",
                List.of("<gray>Return to shop editor.")));

        boolean ready = draft.isReady();
        inv.setItem(SLOT_SAVE, ItemUtil.buildItem(
                ready ? Material.LIME_WOOL : Material.RED_WOOL,
                ready ? "<green><bold>Save Product" : "<red>Cannot Save Yet",
                ready
                        ? List.of("<gray>Adds product <white>" + draft.getKey() + " <gray>to the shop.")
                        : List.of("<red>Set the item and at least one price first.")));
    }

    public void cycleResetType() {
        String cur = draft.getResetType();
        for (int i = 0; i < RESET_CYCLE.length; i++) {
            if (RESET_CYCLE[i].equals(cur)) {
                draft.setResetType(RESET_CYCLE[(i + 1) % RESET_CYCLE.length]);
                return;
            }
        }
        draft.setResetType(RESET_CYCLE[0]);
    }

    public ProductDraft getDraft() { return draft; }
}
