package dev.lofishop.creator;

import dev.lofishop.LofiShop;
import dev.lofishop.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles all interactions with the shop creator wizard:
 * - Wand right-click → open wizard
 * - GUI clicks in main editor and product editor
 * - Drag-and-drop item into product slot
 * - Pop-up input windows (Paper dialogs) for text fields
 */
public class ShopCreatorListener implements Listener {

    private final LofiShop plugin;

    public ShopCreatorListener(LofiShop plugin) {
        this.plugin = plugin;
    }

    // ── Wand right-click ──────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWandUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!plugin.getShopCreatorManager().isCreatorWand(item)) return;
        if (!player.hasPermission("lofishop.admin")) {
            plugin.getMessageConfig().send(player, "no-permission");
            return;
        }

        event.setCancelled(true);
        plugin.getShopCreatorManager().openWizard(player);
    }

    // ── Main editor GUI clicks ────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!plugin.getShopCreatorManager().hasSession(player)) return;

        String tag = plugin.getMenuManager().getOpenShopId(player);
        if (tag == null) return;

        ShopCreatorSession session = plugin.getShopCreatorManager().getSession(player);

        if (ShopCreatorGui.TAG.equals(tag)) {
            event.setCancelled(true);
            handleMainEditorClick(player, session, event.getRawSlot(), event.isShiftClick());
        } else if (ProductEditorGui.TAG.equals(tag)) {
            event.setCancelled(true);
            handleProductEditorClick(player, session, event);
        }
    }

    private void handleMainEditorClick(Player player, ShopCreatorSession session,
                                        int slot, boolean shift) {
        switch (slot) {
            case ShopCreatorGui.SLOT_NAME -> {
                promptInput(player, session, ShopCreatorSession.State.AWAITING_SHOP_NAME,
                        "<gold>Shop display name",
                        "<gray>The name shown at the top of the shop. <dark_gray>e.g. My Shop",
                        session.getShopName());
            }
            case ShopCreatorGui.SLOT_ID -> {
                promptInput(player, session, ShopCreatorSession.State.AWAITING_SHOP_ID,
                        "<gold>Shop file ID",
                        "<gray>Used for the shop's file name. No spaces. <dark_gray>e.g. my_shop",
                        session.getShopId());
            }
            case ShopCreatorGui.SLOT_ROWS -> {
                session.setRows(session.getRows() % 6 + 1);
                refreshMain(player, session);
                player.sendMessage(MessageUtil.parse("<gold>[LofiShop] <gray>Rows set to <white>" + session.getRows()));
            }
            case ShopCreatorGui.SLOT_ADMIN -> {
                session.toggleAdminShop();
                refreshMain(player, session);
                player.sendMessage(MessageUtil.parse("<gold>[LofiShop] <gray>Admin shop: <white>" + session.isAdminShop()));
            }
            case ShopCreatorGui.SLOT_FILLER -> {
                cycleFiller(session);
                refreshMain(player, session);
            }
            case ShopCreatorGui.SLOT_CANCEL -> {
                plugin.getShopCreatorManager().endSession(player);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                    player.sendMessage(MessageUtil.parse("<gold>[LofiShop] <gray>Shop creation cancelled."));
                });
            }
            case ShopCreatorGui.SLOT_SAVE -> {
                if (session.getProducts().isEmpty()) {
                    player.sendMessage(MessageUtil.parse("<red>Add at least one product before saving."));
                    return;
                }
                if (session.getShopId().isBlank()) {
                    player.sendMessage(MessageUtil.parse("<red>Set a shop ID before saving."));
                    return;
                }
                ShopCreatorSession savedSession = session;
                plugin.getShopCreatorManager().endSession(player);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                    ShopYamlWriter.write(plugin, savedSession, player);
                });
            }
            case ShopCreatorGui.SLOT_PREV -> {
                // Page navigation handled via re-open with page-1
                player.sendMessage(MessageUtil.parse("<gray>Previous page."));
            }
            case ShopCreatorGui.SLOT_NEXT -> {
                player.sendMessage(MessageUtil.parse("<gray>Next page."));
            }
            default -> {
                // Product area
                if (slot >= ShopCreatorGui.PRODUCT_START && slot <= ShopCreatorGui.PRODUCT_END) {
                    handleProductAreaClick(player, session, slot, shift);
                }
            }
        }
    }

    private void handleProductAreaClick(Player player, ShopCreatorSession session, int slot, boolean shift) {
        int idx = slot - ShopCreatorGui.PRODUCT_START;
        List<ProductDraft> products = session.getProducts();

        if (idx < products.size()) {
            ProductDraft draft = products.get(idx);
            if (shift) {
                products.remove(idx);
                player.sendMessage(MessageUtil.parse(
                        "<gold>[LofiShop] <gray>Removed product <white>" + draft.getKey()));
                refreshMain(player, session);
            } else {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        new ProductEditorGui(plugin, player, session, draft).open());
            }
        } else if (idx == products.size()) {
            // "Add Product" button
            ProductDraft newDraft = new ProductDraft(session.nextKey());
            // Don't add to list yet — add only when saved from product editor
            session.setEditingProduct(newDraft);
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    new ProductEditorGui(plugin, player, session, newDraft).open());
        }
    }

    // ── Product editor clicks ─────────────────────────────────────────────────

    private void handleProductEditorClick(Player player, ShopCreatorSession session,
                                           InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ProductDraft draft = session.getEditingProduct();
        if (draft == null) return;

        // Click in player's bottom inventory → capture that item as the product item.
        // The click is already cancelled, so the item stays in the player's inventory.
        if (slot >= 27) {
            org.bukkit.inventory.Inventory clickedInv = event.getClickedInventory();
            if (clickedInv != null) {
                ItemStack item = clickedInv.getItem(event.getSlot());
                if (item != null && !item.getType().isAir()) {
                    ItemStack toSet = item.clone();
                    toSet.setAmount(1);
                    draft.setItem(toSet);
                    player.sendMessage(MessageUtil.parse(
                            "<gold>[LofiShop] <gray>Item set: <white>" + item.getType().name()));
                    refreshProduct(player, session, draft);
                }
            }
            return;
        }

        switch (slot) {
            case ProductEditorGui.SLOT_BUY_PRICE ->
                promptInput(player, session, ShopCreatorSession.State.AWAITING_BUY_PRICE,
                        "<gold>Buy price",
                        "<gray>What players pay to buy this item. Leave empty to disable buying.",
                        priceText(draft.getBuyPrice()));
            case ProductEditorGui.SLOT_SELL_PRICE ->
                promptInput(player, session, ShopCreatorSession.State.AWAITING_SELL_PRICE,
                        "<gold>Sell price",
                        "<gray>What players get for selling this item. Leave empty to disable selling.",
                        priceText(draft.getSellPrice()));
            case ProductEditorGui.SLOT_AMOUNT ->
                promptInput(player, session, ShopCreatorSession.State.AWAITING_AMOUNT,
                        "<gold>Default amount",
                        "<gray>How many items one purchase or sale is for. <dark_gray>e.g. 1, 8, 64",
                        String.valueOf(draft.getAmount()));
            case ProductEditorGui.SLOT_BUY_TIERS ->
                promptInput(player, session, ShopCreatorSession.State.AWAITING_BUY_TIERS,
                        "<gold>Buy amounts",
                        "<gray>Amount options for buying, separated by commas. Leave empty to clear. <dark_gray>e.g. 1,8,16,64",
                        tiersText(draft.getBuyAmounts()));
            case ProductEditorGui.SLOT_SELL_TIERS ->
                promptInput(player, session, ShopCreatorSession.State.AWAITING_SELL_TIERS,
                        "<gold>Sell amounts",
                        "<gray>Amount options for selling, separated by commas. Leave empty to clear. <dark_gray>e.g. 1,8,16,64",
                        tiersText(draft.getSellAmounts()));
            case ProductEditorGui.SLOT_BUY_LIMIT ->
                promptInput(player, session, ShopCreatorSession.State.AWAITING_BUY_LIMIT,
                        "<gold>Personal buy limit",
                        "<gray>How many each player can buy per reset. -1 = unlimited.",
                        String.valueOf(draft.getPersonalBuyLimit()));
            case ProductEditorGui.SLOT_SELL_LIMIT ->
                promptInput(player, session, ShopCreatorSession.State.AWAITING_SELL_LIMIT,
                        "<gold>Personal sell limit",
                        "<gray>How many each player can sell per reset. -1 = unlimited.",
                        String.valueOf(draft.getPersonalSellLimit()));
            case ProductEditorGui.SLOT_RESET_TYPE -> {
                cycleResetType(draft);
                refreshProduct(player, session, draft);
            }
            case ProductEditorGui.SLOT_BACK ->
                returnToMain(player, session);
            case ProductEditorGui.SLOT_SAVE -> {
                if (!draft.isReady()) {
                    player.sendMessage(MessageUtil.parse("<red>Set the item and at least one price first."));
                    return;
                }
                if (!session.getProducts().contains(draft)) {
                    session.addProduct(draft);
                }
                player.sendMessage(MessageUtil.parse(
                        "<gold>[LofiShop] <green>Product <white>" + draft.getKey() + " <green>saved."));
                returnToMain(player, session);
            }
        }
    }

    // Drag-and-drop in the product editor
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!plugin.getShopCreatorManager().hasSession(player)) return;

        String tag = plugin.getMenuManager().getOpenShopId(player);
        if (!ProductEditorGui.TAG.equals(tag)) return;

        event.setCancelled(true);
    }

    // ── Text input (pop-up windows) ───────────────────────────────────────────

    /**
     * Applies a value entered in an input window.
     * Returns an error message (MiniMessage) if the value is invalid, or null once it's applied.
     */
    private String applyInput(Player player, ShopCreatorSession session,
                              ShopCreatorSession.State state, String input) {
        ProductDraft draft = session.getEditingProduct();
        boolean none = input.isEmpty() || input.equalsIgnoreCase("none");

        switch (state) {
            case AWAITING_SHOP_NAME -> {
                if (input.isEmpty()) return "<red>The shop name can't be empty.";
                session.setShopName(input);
                player.sendMessage(MessageUtil.parse("<gold>[LofiShop] <gray>Shop name set to <white>" + input));
            }
            case AWAITING_SHOP_ID -> {
                if (input.isEmpty()) return "<red>The shop ID can't be empty.";
                session.setShopId(input);
                player.sendMessage(MessageUtil.parse("<gold>[LofiShop] <gray>Shop ID set to <white>" + session.getShopId()));
            }
            case AWAITING_BUY_PRICE -> {
                if (draft == null) return null;
                if (none) {
                    draft.setBuyPrice(-1);
                } else {
                    Double price = parseDouble(input);
                    if (price == null) return "<red>That isn't a number. Enter a price, or leave it empty to disable buying.";
                    draft.setBuyPrice(price);
                }
            }
            case AWAITING_SELL_PRICE -> {
                if (draft == null) return null;
                if (none) {
                    draft.setSellPrice(-1);
                } else {
                    Double price = parseDouble(input);
                    if (price == null) return "<red>That isn't a number. Enter a price, or leave it empty to disable selling.";
                    draft.setSellPrice(price);
                }
            }
            case AWAITING_AMOUNT -> {
                if (draft == null) return null;
                Integer amount = parseInt(input);
                if (amount == null) return "<red>Enter a whole number, like 1, 8 or 64.";
                draft.setAmount(amount);
            }
            case AWAITING_BUY_TIERS -> {
                if (draft == null) return null;
                if (none) {
                    draft.setBuyAmounts(new ArrayList<>());
                } else {
                    List<Integer> tiers = parseTiers(input);
                    if (tiers == null) return "<red>Use whole numbers separated by commas, like 1,8,16,64.";
                    draft.setBuyAmounts(tiers);
                }
            }
            case AWAITING_SELL_TIERS -> {
                if (draft == null) return null;
                if (none) {
                    draft.setSellAmounts(new ArrayList<>());
                } else {
                    List<Integer> tiers = parseTiers(input);
                    if (tiers == null) return "<red>Use whole numbers separated by commas, like 1,8,16,64.";
                    draft.setSellAmounts(tiers);
                }
            }
            case AWAITING_BUY_LIMIT -> {
                if (draft == null) return null;
                Integer limit = parseInt(input);
                if (limit == null) return "<red>Enter a whole number. Use -1 for unlimited.";
                draft.setPersonalBuyLimit(limit);
            }
            case AWAITING_SELL_LIMIT -> {
                if (draft == null) return null;
                Integer limit = parseInt(input);
                if (limit == null) return "<red>Enter a whole number. Use -1 for unlimited.";
                draft.setPersonalSellLimit(limit);
            }
            default -> {}
        }
        return null;
    }

    // ── Inventory close ───────────────────────────────────────────────────────

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        if (!plugin.getShopCreatorManager().hasSession(player)) return;
        // Don't end session on close — the editor closes while an input window is open
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Closes the editor and asks for a value in a pop-up window. */
    private void promptInput(Player player, ShopCreatorSession session,
                             ShopCreatorSession.State state, String title, String hint, String initial) {
        session.setState(state);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            showInput(player, session, state, title, hint, null, initial);
        });
    }

    private void showInput(Player player, ShopCreatorSession session, ShopCreatorSession.State state,
                           String title, String hint, String error, String initial) {
        InputDialog.show(plugin, player, title, hint, error, initial,
                value -> {
                    if (!isStillWaiting(player, session, state)) return;
                    String problem = applyInput(player, session, state, value);
                    if (problem != null) {
                        // Ask again, keeping what they typed and showing what was wrong
                        showInput(player, session, state, title, hint, problem, value);
                    } else {
                        reopenEditor(player, session, state);
                    }
                },
                () -> {
                    if (!isStillWaiting(player, session, state)) return;
                    reopenEditor(player, session, state);
                });
    }

    /** False if the session ended or moved on while the window was open. */
    private boolean isStillWaiting(Player player, ShopCreatorSession session, ShopCreatorSession.State state) {
        return plugin.getShopCreatorManager().getSession(player) == session && session.getState() == state;
    }

    /** Goes back to whichever editor the value belongs to. */
    private void reopenEditor(Player player, ShopCreatorSession session, ShopCreatorSession.State state) {
        boolean shopField = state == ShopCreatorSession.State.AWAITING_SHOP_NAME
                || state == ShopCreatorSession.State.AWAITING_SHOP_ID;
        ProductDraft draft = session.getEditingProduct();
        if (shopField || draft == null) {
            session.setState(ShopCreatorSession.State.MAIN_EDITOR);
            new ShopCreatorGui(plugin, player, session).open();
        } else {
            session.setState(ShopCreatorSession.State.PRODUCT_EDITOR);
            new ProductEditorGui(plugin, player, session, draft).open();
        }
    }

    private static String priceText(double price) {
        return price < 0 ? "" : BigDecimal.valueOf(price).stripTrailingZeros().toPlainString();
    }

    private static String tiersText(List<Integer> tiers) {
        return tiers == null ? "" : tiers.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static Double parseDouble(String input) {
        try { return Double.parseDouble(input); } catch (NumberFormatException e) { return null; }
    }

    private static Integer parseInt(String input) {
        try { return Integer.parseInt(input); } catch (NumberFormatException e) { return null; }
    }

    private void refreshMain(Player player, ShopCreatorSession session) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            org.bukkit.inventory.Inventory current =
                    player.getOpenInventory().getTopInventory();
            // Update in-place to avoid close/reopen desync
            if (current.getSize() == 54) {
                current.clear();
                new ShopCreatorGui(plugin, player, session).populate(current);
            } else {
                new ShopCreatorGui(plugin, player, session).open();
            }
        });
    }

    private void refreshProduct(Player player, ShopCreatorSession session, ProductDraft draft) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            org.bukkit.inventory.Inventory current =
                    player.getOpenInventory().getTopInventory();
            // Update in-place to avoid close/reopen desync
            if (current.getSize() == 27) {
                current.clear();
                new ProductEditorGui(plugin, player, session, draft).populate(current);
            } else {
                new ProductEditorGui(plugin, player, session, draft).open();
            }
        });
    }

    private void returnToMain(Player player, ShopCreatorSession session) {
        session.setState(ShopCreatorSession.State.MAIN_EDITOR);
        session.setEditingProduct(null);
        plugin.getServer().getScheduler().runTask(plugin, () ->
                new ShopCreatorGui(plugin, player, session).open());
    }

    private void cycleResetType(ProductDraft draft) {
        String[] cycle = {"NEVER", "DAILY", "WEEKLY"};
        for (int i = 0; i < cycle.length; i++) {
            if (cycle[i].equals(draft.getResetType())) {
                draft.setResetType(cycle[(i + 1) % cycle.length]);
                return;
            }
        }
        draft.setResetType("NEVER");
    }

    private void cycleFiller(ShopCreatorSession session) {
        String[] cycle = {
            "GRAY_STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE",
            "WHITE_STAINED_GLASS_PANE", "BLUE_STAINED_GLASS_PANE",
            "GREEN_STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE",
            "YELLOW_STAINED_GLASS_PANE", "PURPLE_STAINED_GLASS_PANE"
        };
        String cur = session.getFillerMaterial();
        for (int i = 0; i < cycle.length; i++) {
            if (cycle[i].equals(cur)) {
                session.setFillerMaterial(cycle[(i + 1) % cycle.length]);
                return;
            }
        }
        session.setFillerMaterial(cycle[0]);
    }

    private List<Integer> parseTiers(String input) {
        List<Integer> result = new ArrayList<>();
        for (String s : input.split(",")) {
            try { result.add(Integer.parseInt(s.trim())); }
            catch (NumberFormatException e) { return null; }
        }
        return result.isEmpty() ? null : result;
    }
}
