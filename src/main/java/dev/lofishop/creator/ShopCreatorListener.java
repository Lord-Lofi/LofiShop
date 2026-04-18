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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all interactions with the shop creator wizard:
 * - Wand right-click → open wizard
 * - GUI clicks in main editor and product editor
 * - Drag-and-drop item into product slot
 * - Chat input capture for text fields
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
                promptChat(player, session, ShopCreatorSession.State.AWAITING_SHOP_NAME,
                        "<gold>Type the <white>shop display name <gold>in chat. <gray>(e.g. My Shop)");
            }
            case ShopCreatorGui.SLOT_ID -> {
                promptChat(player, session, ShopCreatorSession.State.AWAITING_SHOP_ID,
                        "<gold>Type the <white>shop file ID <gold>in chat. <gray>(e.g. my_shop — no spaces)");
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
                promptChat(player, session, ShopCreatorSession.State.AWAITING_BUY_PRICE,
                        "<gold>Type the <white>buy price <gold>in chat. <gray>Type 'none' to disable.");
            case ProductEditorGui.SLOT_SELL_PRICE ->
                promptChat(player, session, ShopCreatorSession.State.AWAITING_SELL_PRICE,
                        "<gold>Type the <white>sell price <gold>in chat. <gray>Type 'none' to disable.");
            case ProductEditorGui.SLOT_AMOUNT ->
                promptChat(player, session, ShopCreatorSession.State.AWAITING_AMOUNT,
                        "<gold>Type the <white>default amount <gold>in chat. <gray>(e.g. 1, 8, 64)");
            case ProductEditorGui.SLOT_BUY_TIERS ->
                promptChat(player, session, ShopCreatorSession.State.AWAITING_BUY_TIERS,
                        "<gold>Type <white>buy tier amounts <gold>comma-separated. <gray>(e.g. 1,8,16,64) Type 'none' to clear.");
            case ProductEditorGui.SLOT_SELL_TIERS ->
                promptChat(player, session, ShopCreatorSession.State.AWAITING_SELL_TIERS,
                        "<gold>Type <white>sell tier amounts <gold>comma-separated. <gray>(e.g. 1,8,16,64) Type 'none' to clear.");
            case ProductEditorGui.SLOT_BUY_LIMIT ->
                promptChat(player, session, ShopCreatorSession.State.AWAITING_BUY_LIMIT,
                        "<gold>Type <white>personal buy limit <gold>in chat. <gray>-1 = unlimited.");
            case ProductEditorGui.SLOT_SELL_LIMIT ->
                promptChat(player, session, ShopCreatorSession.State.AWAITING_SELL_LIMIT,
                        "<gold>Type <white>personal sell limit <gold>in chat. <gray>-1 = unlimited.");
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

    // ── Chat capture ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getShopCreatorManager().hasSession(player)) return;

        ShopCreatorSession session = plugin.getShopCreatorManager().getSession(player);
        ShopCreatorSession.State state = session.getState();

        if (state == ShopCreatorSession.State.MAIN_EDITOR
                || state == ShopCreatorSession.State.PRODUCT_EDITOR) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();

        // Run on main thread
        plugin.getServer().getScheduler().runTask(plugin, () ->
                handleChatInput(player, session, state, input));
    }

    private void handleChatInput(Player player, ShopCreatorSession session,
                                  ShopCreatorSession.State state, String input) {
        ProductDraft draft = session.getEditingProduct();

        switch (state) {
            case AWAITING_SHOP_NAME -> {
                session.setShopName(input);
                player.sendMessage(MessageUtil.parse("<gold>[LofiShop] <gray>Shop name set to <white>" + input));
                session.setState(ShopCreatorSession.State.MAIN_EDITOR);
                new ShopCreatorGui(plugin, player, session).open();
            }
            case AWAITING_SHOP_ID -> {
                session.setShopId(input);
                player.sendMessage(MessageUtil.parse("<gold>[LofiShop] <gray>Shop ID set to <white>" + session.getShopId()));
                session.setState(ShopCreatorSession.State.MAIN_EDITOR);
                new ShopCreatorGui(plugin, player, session).open();
            }
            case AWAITING_BUY_PRICE -> {
                if (draft == null) return;
                if (input.equalsIgnoreCase("none")) {
                    draft.setBuyPrice(-1);
                } else {
                    try { draft.setBuyPrice(Double.parseDouble(input)); }
                    catch (NumberFormatException e) {
                        player.sendMessage(MessageUtil.parse("<red>Invalid number. Try again or type 'none'."));
                        return;
                    }
                }
                session.setState(ShopCreatorSession.State.PRODUCT_EDITOR);
                new ProductEditorGui(plugin, player, session, draft).open();
            }
            case AWAITING_SELL_PRICE -> {
                if (draft == null) return;
                if (input.equalsIgnoreCase("none")) {
                    draft.setSellPrice(-1);
                } else {
                    try { draft.setSellPrice(Double.parseDouble(input)); }
                    catch (NumberFormatException e) {
                        player.sendMessage(MessageUtil.parse("<red>Invalid number. Try again or type 'none'."));
                        return;
                    }
                }
                session.setState(ShopCreatorSession.State.PRODUCT_EDITOR);
                new ProductEditorGui(plugin, player, session, draft).open();
            }
            case AWAITING_AMOUNT -> {
                if (draft == null) return;
                try {
                    draft.setAmount(Integer.parseInt(input));
                    session.setState(ShopCreatorSession.State.PRODUCT_EDITOR);
                    new ProductEditorGui(plugin, player, session, draft).open();
                } catch (NumberFormatException e) {
                    player.sendMessage(MessageUtil.parse("<red>Invalid number. Enter a whole number like 1, 8, or 64."));
                }
            }
            case AWAITING_BUY_TIERS -> {
                if (draft == null) return;
                if (input.equalsIgnoreCase("none")) {
                    draft.setBuyAmounts(new ArrayList<>());
                } else {
                    List<Integer> tiers = parseTiers(input);
                    if (tiers == null) {
                        player.sendMessage(MessageUtil.parse("<red>Invalid format. Use comma-separated numbers: 1,8,16,64"));
                        return;
                    }
                    draft.setBuyAmounts(tiers);
                }
                session.setState(ShopCreatorSession.State.PRODUCT_EDITOR);
                new ProductEditorGui(plugin, player, session, draft).open();
            }
            case AWAITING_SELL_TIERS -> {
                if (draft == null) return;
                if (input.equalsIgnoreCase("none")) {
                    draft.setSellAmounts(new ArrayList<>());
                } else {
                    List<Integer> tiers = parseTiers(input);
                    if (tiers == null) {
                        player.sendMessage(MessageUtil.parse("<red>Invalid format. Use comma-separated numbers: 1,8,16,64"));
                        return;
                    }
                    draft.setSellAmounts(tiers);
                }
                session.setState(ShopCreatorSession.State.PRODUCT_EDITOR);
                new ProductEditorGui(plugin, player, session, draft).open();
            }
            case AWAITING_BUY_LIMIT -> {
                if (draft == null) return;
                try {
                    draft.setPersonalBuyLimit(Integer.parseInt(input));
                    session.setState(ShopCreatorSession.State.PRODUCT_EDITOR);
                    new ProductEditorGui(plugin, player, session, draft).open();
                } catch (NumberFormatException e) {
                    player.sendMessage(MessageUtil.parse("<red>Invalid number. Use -1 for unlimited."));
                }
            }
            case AWAITING_SELL_LIMIT -> {
                if (draft == null) return;
                try {
                    draft.setPersonalSellLimit(Integer.parseInt(input));
                    session.setState(ShopCreatorSession.State.PRODUCT_EDITOR);
                    new ProductEditorGui(plugin, player, session, draft).open();
                } catch (NumberFormatException e) {
                    player.sendMessage(MessageUtil.parse("<red>Invalid number. Use -1 for unlimited."));
                }
            }
            default -> {}
        }
    }

    // ── Inventory close ───────────────────────────────────────────────────────

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        if (!plugin.getShopCreatorManager().hasSession(player)) return;
        // Don't end session on close — player may have typed in chat and will reopen
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void promptChat(Player player, ShopCreatorSession session,
                             ShopCreatorSession.State nextState, String prompt) {
        session.setState(nextState);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            player.sendMessage(MessageUtil.parse(prompt));
            player.sendMessage(MessageUtil.parse("<gray>Type <red>cancel <gray>to abort."));
            openChatScreen(player);
        });
    }

    private void openChatScreen(Player player) {
        try {
            Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object conn = nmsPlayer.getClass().getField("connection").get(nmsPlayer);
            Class<?> pktClass = Class.forName(
                    "net.minecraft.network.protocol.game.ClientboundOpenChatScreenPacket");
            Object pkt = pktClass.getConstructor(String.class).newInstance("");
            Class<?> packetBase = Class.forName("net.minecraft.network.protocol.Packet");
            conn.getClass().getMethod("send", packetBase).invoke(conn, pkt);
        } catch (Exception ignored) {
            // Paper version doesn't support it; player presses T manually
        }
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
