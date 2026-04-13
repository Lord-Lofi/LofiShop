# LofiShop — User Guide

> **Plugin version:** see `plugin.yml`
> **Server version:** Paper 1.21.x
> **Required dependencies:** Vault + an economy plugin (EssentialsX recommended)
> **Optional dependencies:** PlaceholderAPI, LuckPerms

---

## Table of Contents

1. [Server Admin — Installation & Setup](#1-server-admin--installation--setup)
2. [Server Admin — Configuration Files](#2-server-admin--configuration-files)
3. [Server Admin — YAML Shop Creation](#3-server-admin--yaml-shop-creation)
4. [Server Admin — Admin Shops](#4-server-admin--admin-shops)
5. [Server Admin — Block Shops](#5-server-admin--block-shops)
6. [Server Admin — Permissions & LuckPerms](#6-server-admin--permissions--luckperms)
7. [Server Admin — Commands Reference](#7-server-admin--commands-reference)
8. [Server Admin — PlaceholderAPI](#8-server-admin--placeholderapi)
9. [Shop Owner — In-Game Shop Creator](#9-shop-owner--in-game-shop-creator)
10. [Shop Owner — Managing Block Shops](#10-shop-owner--managing-block-shops)
11. [Shop Owner — Sell Wand](#11-shop-owner--sell-wand)
12. [Buyer — Using Shops](#12-buyer--using-shops)
13. [Buyer — Quick Sell](#13-buyer--quick-sell)
14. [Buyer — Sell Wand (if granted)](#14-buyer--sell-wand-if-granted)
15. [Custom Plugin Items (MMOItems, Oraxen, etc.)](#15-custom-plugin-items-mmoitems-oraxen-etc)
16. [Troubleshooting](#16-troubleshooting)

---

## 1. Server Admin — Installation & Setup

### Requirements

| Dependency | Role | Required? |
|---|---|---|
| **Vault** | Economy abstraction layer | Yes |
| **EssentialsX** | Economy provider | Yes (or another Vault economy plugin) |
| **PlaceholderAPI** | `%lofishop_*%` placeholders | No |
| **LuckPerms** | Fine-grained permissions | No (any permissions plugin works) |

### Installation Steps

1. Drop `LofiShop.jar` into your server's `plugins/` folder.
2. Ensure **Vault** and **EssentialsX** are also in `plugins/`.
3. Start the server. LofiShop will generate:
   ```
   plugins/LofiShop/
   ├── config.yml
   ├── messages.yml
   └── shops/
       ├── example.yml
       └── admin.yml
   ```
4. Stop the server, configure `config.yml` to your liking (see [Section 2](#2-server-admin--configuration-files)), then restart.

> **Note:** LofiShop does **not** create its own economy. Vault must be configured with a provider (EssentialsX does this automatically on startup).

---

## 2. Server Admin — Configuration Files

### `config.yml`

```yaml
general:
  economy: vault          # Economy provider: "vault" or "essentials"
  currency-symbol: "$"    # Symbol shown in menus
  price-decimals: 2       # Decimal places for prices
  sell-enabled: true      # Toggle selling globally
  buy-enabled: true       # Toggle buying globally
  log-transactions: true  # Print buy/sell events to console

limits:
  storage: yaml                 # Limit data storage format
  daily-reset-time: "00:00"     # Server time for daily limit resets (24h)
  weekly-reset-day: MONDAY      # Day weekly limits reset

sell-wand:
  material: BLAZE_ROD
  name: "<gold><bold>Sell Wand"
  lore:
    - "<gray>Right-click a chest to sell its contents automatically."
  consume-on-use: false   # Whether the wand is destroyed after one use
  max-uses: -1            # Max uses before breaking (-1 = infinite)

menus:
  filler-material: GRAY_STAINED_GLASS_PANE
  filler-name: " "
  auto-price-lore: true   # Append buy/sell prices to item lore automatically
  auto-limit-lore: true   # Append limit info to item lore automatically
```

### `messages.yml`

All messages use [MiniMessage](https://docs.advntr.dev/minimessage) formatting. Edit them to match your server's style. Available placeholders differ per message — they are documented inline in the file.

---

## 3. Server Admin — YAML Shop Creation

Each `.yml` file inside `plugins/LofiShop/shops/` becomes one shop. The file name (without `.yml`) is the **shop ID** used in commands.

### Minimal shop file

```yaml
shop-name: "<gold>My Shop"
title: "<dark_gray>[ <gold>My Shop</gold> ]"
rows: 6
admin-shop: false
open-permission: ""

layout:
  - "F F F F F F F F F"
  - "F a F b F F F F F"
  - "F F F F F F F F F"
  - "F F F F F F F F F"
  - "F F F F F F F F F"
  - "F F F F F F F F F"

products:
  a:
    item:
      material: DIAMOND
      name: "<aqua>Diamond"
    buy-price:
      - type: vault
        amount: 250.0
    sell-price:
      - type: vault
        amount: 100.0
    amount: 1

  b:
    item:
      material: IRON_INGOT
      name: "<white>Iron Ingot"
    buy-price:
      - type: vault
        amount: 20.0
    amount: 1

filler:
  material: GRAY_STAINED_GLASS_PANE
  name: " "
```

### Layout Keys

The `layout` section is a grid of 9 characters per row (space-separated). Each character maps to a product key defined under `products:`.

| Key | Meaning |
|---|---|
| `F` | Filler item (background glass) |
| Any other character | Product key (must match a key under `products:`) |

### Product Fields

| Field | Description | Default |
|---|---|---|
| `item.material` | Bukkit material name | `STONE` |
| `item.name` | MiniMessage display name | (none) |
| `item.lore` | List of MiniMessage lore lines | (none) |
| `item.custom-model-data` | Custom model data integer | (none) |
| `item.item-data` | Base64 serialized item (set by in-game creator for custom plugin items) | (none) |
| `buy-price` | List of `{type, amount}` maps | (not buyable) |
| `sell-price` | List of `{type, amount}` maps | (not sellable) |
| `amount` | Items per click (when no tiers configured) | `1` |
| `buy-amounts` | List of quantity options for buy picker menu | (none) |
| `sell-amounts` | List of quantity options for sell picker menu | (none) |
| `limits` | See limits table below | unlimited |
| `actions.buy` / `actions.sell` | Action strings run on transaction | (none) |
| `conditions.buy` / `conditions.sell` | Condition strings checked before transaction | (none) |

### Limit Fields

```yaml
limits:
  personal-buy: 64       # Max a single player can buy per period (-1 = unlimited)
  personal-sell: -1      # Max a single player can sell per period
  global-buy: 100        # Max total buys across all players per period
  global-sell: -1        # Max total sells across all players per period
  reset: DAILY           # NEVER | DAILY | WEEKLY | CRON
  cron: "0 0 * * MON"   # Cron expression (only used when reset: CRON)
```

### Action Strings

| Format | Effect |
|---|---|
| `[message] <green>Hello!` | Sends a chat message to the player |
| `[actionbar] <yellow>Sold!` | Displays an action bar message |
| `[title] Title\|Subtitle` | Shows a title screen |
| `[sound] ENTITY_EXPERIENCE_ORB_PICKUP` | Plays a sound |
| `[command] fly {player}` | Runs a command as the player |
| `[console] give {player} diamond 1` | Runs a command from console |

### Condition Strings

| Format | Passes when... |
|---|---|
| `[permission] lofishop.vip` | Player has the permission |
| `[money] >= 1000` | Player's balance meets the condition |
| `[level] >= 30` | Player's XP level meets the condition |
| `[placeholder] %someplaceholder% == value` | PlaceholderAPI value matches |

Operators supported: `==`, `!=`, `>=`, `<=`, `>`, `<`

### Reload

After editing any shop YAML:
```
/lofishop reload
```
No server restart needed. All shops reload hot.

---

## 4. Server Admin — Admin Shops

Admin shops have **infinite stock**. Players can buy without the shop having any physical inventory, and buy/sell limits are automatically bypassed for the shop itself.

Enable by adding `admin-shop: true` to any shop file:

```yaml
shop-name: "<dark_purple>Server Shop"
admin-shop: true
rows: 6
```

**When to use admin shops:**
- Server resource shops (ores, wood, food) that should never run dry
- Starter item dispensers
- Premium/VIP item shops

**Admin shops still require:**
- The player to have enough money to buy
- Any configured `conditions` to pass (e.g. `[permission] lofishop.vip`)

---

## 5. Server Admin — Block Shops

Block shops attach a shop product to a physical block in the world. A floating animated item rotates above it. Players right-click the block to interact.

### Block Shop Modes

Every block shop has a mode set at creation time:

| Mode | Right-click behaviour | Best for |
|---|---|---|
| `FULL` | Opens the entire shop GUI — all products browsable | Category shops (ore shop, wood shop) — the block is the entrance |
| `SMALL` | Opens a compact 3-row GUI: item in the centre, green Buy button left, red Sell button right | Dedicated vending-machine-style blocks |
| `QUICK` | Instantly purchases the product's default amount — no GUI, feedback in chat | High-traffic impulse buys |

### Creating a Block Shop

Look directly at a block and run:

```
/lofishop createblock <shopId> <productKey> [FULL|SMALL|QUICK]
```

The mode defaults to `FULL` if omitted. Examples:

```
# A slab that opens the full ore shop
/lofishop createblock ore_shop d

# A block that shows a single-product buy/sell panel for diamonds
/lofishop createblock ore_shop d SMALL

# A block that instantly buys 1 diamond on right-click
/lofishop createblock ore_shop d QUICK
```

The floating item above the block always shows the linked product, regardless of mode.

### Removing a Block Shop

Look at the block and run:
```
/lofishop removeblock
```

Or break the block — if you have `lofishop.admin` the block shop is automatically cleaned up.

### Citizens NPC Integration

Any Citizens NPC can open a shop for the player who right-clicks it. In Citizens, add a **server command** to the NPC:

```
/shop open <shopId> %player_name%
```

Citizens resolves `%player_name%` to the interacting player's name before the command runs. The command executes as console, so no permission check applies and the shop opens for the correct player. This works with any shop mode — combine it with `FULL`, `SMALL`, or use separate blocks for different modes alongside the NPC.

**Example Citizens NPC setup:**
1. Create an NPC: `/npc create ShopKeeper`
2. Add a command trigger: `/npc command add -p /shop open ore_shop %player_name%`
3. Done — right-clicking the NPC opens the ore shop for whoever clicked.

### Notes

- Block shops persist across restarts via `blockshops.yml`. Display entities are re-spawned 20 ticks after the server loads.
- The mode is stored in `blockshops.yml` — re-creating the shop is the only way to change the mode.
- `SMALL` mode respects quantity tiers — if the product has `buy-amounts` configured, clicking Buy opens the picker sub-menu.

---

## 6. Server Admin — Permissions & LuckPerms

LofiShop uses a permission hierarchy modeled after UltimateShop, so existing LuckPerms groups transfer easily.

### Core Nodes

| Node | Default | Description |
|---|---|---|
| `lofishop.admin` | op | Grants everything |
| `lofishop.use` | true | Base player access (open, buy, sell, quicksell, sell wand) |
| `lofishop.reload` | op | `/lofishop reload` |
| `lofishop.creator` | op | Use the shop creator wand |
| `lofishop.give.creator` | op | Give creator wand to players |
| `lofishop.give.sellwand` | op | Give sell wands to players |

### Shop Access Nodes

| Node | Description |
|---|---|
| `lofishop.open` | Open shops via block right-click or NPC interaction |
| `lofishop.open.command` | Open shops via the `/shop open` command. Without this, players must use a physical block shop or NPC — they cannot open shops directly from chat. |
| `lofishop.open.<shopId>` | Open one specific shop (e.g. `lofishop.open.vip-shop`) |
| `lofishop.buy` | Buy in any shop |
| `lofishop.buy.<shopId>` | Buy in a specific shop |
| `lofishop.buy.<shopId>.<productId>` | Buy one specific product |
| `lofishop.sell` | Sell in any shop |
| `lofishop.sell.<shopId>` | Sell in a specific shop |
| `lofishop.sell.<shopId>.<productId>` | Sell one specific product |

### Bypass Nodes

| Node | Default | Description |
|---|---|---|
| `lofishop.bypass.limits` | op | Ignore all buy/sell limits |
| `lofishop.bypass.conditions` | op | Ignore all product conditions |

### Quick Sell / Sell Wand

| Node | Default | Description |
|---|---|---|
| `lofishop.quicksell` | true | Use `/lofishop quicksell` |
| `lofishop.sellwand.use` | true | Use a sell wand on chests |

### Example LuckPerms Setup

```bash
# Give all players basic access (already default, but explicit)
lp group default permission set lofishop.use true

# Force players to use physical block shops — deny the command shortcut
# (lofishop.open.command is op-only by default; explicitly deny it for safety)
lp group default permission set lofishop.open.command false

# Staff can open any shop via command
lp group staff permission set lofishop.open.command true

# VIP group — access a VIP-only shop and bypass daily limits
lp group vip permission set lofishop.open.vip-shop true
lp group vip permission set lofishop.buy.vip-shop true
lp group vip permission set lofishop.bypass.limits true

# Restrict a product to staff only
lp group staff permission set lofishop.buy.example.s true

# Give a moderator the sell wand
lp user SomeMod permission set lofishop.give.sellwand true
```

---

## 7. Server Admin — Commands Reference

All commands use the base `/lofishop` (aliases: `/shop`, `/ls`).

| Command | Permission | Description |
|---|---|---|
| `/lofishop open <shopId>` | `lofishop.open.command` | Open a shop via command |
| `/lofishop open <shopId> <player>` | `lofishop.admin` or console | Open a shop for another player (Citizens NPC use) |
| `/lofishop list` | `lofishop.use` | List all available shops |
| `/lofishop reload` | `lofishop.reload` | Reload all configs and shops |
| `/lofishop quicksell` | `lofishop.quicksell` | Open quick-sell menu |
| `/lofishop give sellwand [player]` | `lofishop.give.sellwand` | Give a sell wand |
| `/lofishop givecreator [player]` | `lofishop.give.creator` | Give the shop creator wand |
| `/lofishop createblock <shopId> <productKey> [FULL\|SMALL\|QUICK]` | `lofishop.admin` | Attach a block shop (look at block) |
| `/lofishop removeblock` | `lofishop.admin` | Remove the nearest block shop |
| `/lofishop help` | `lofishop.use` | Show help |
| `/sellwand [player]` | `lofishop.give.sellwand` | Shorthand sell wand give command |

---

## 8. Server Admin — PlaceholderAPI

Requires PlaceholderAPI installed and `placeholders.enabled: true` in `config.yml`.

| Placeholder | Returns |
|---|---|
| `%lofishop_balance%` | Player's economy balance (raw number) |
| `%lofishop_balance_formatted%` | Balance formatted with currency symbol |
| `%lofishop_buy_limit_<shopId>_<productId>%` | Remaining personal buy limit for a product |
| `%lofishop_sell_limit_<shopId>_<productId>%` | Remaining personal sell limit for a product |

**Example usage in a scoreboard plugin:**
```
Balance: %lofishop_balance_formatted%
Diamonds left today: %lofishop_buy_limit_example_d%
```

---

## 9. Shop Owner — In-Game Shop Creator

The shop creator lets you build and save shops entirely in-game without touching any YAML files.

### Getting the Creator Wand

Ask a server admin to run:
```
/lofishop givecreator <yourname>
```
Or if you have the permission yourself:
```
/lofishop givecreator
```
You will receive a **Nether Star** with a gold glow — this is the creator wand.

### Opening the Shop Editor

Right-click with the creator wand. A 54-slot editor GUI opens.

### Main Shop Editor (54 slots)

```
[Shop Name]  [Shop ID]  [Rows]  [Admin]  [Filler]  ...  [Cancel]
[Product 1] [Product 2] [Product 3] ...  (slots 9–44)
...
[Prev Page]  ...  [Save Shop]  ...  [Next Page]
```

| Slot | Item | Action |
|---|---|---|
| 0 | Name tag | Click → type new name in chat |
| 1 | Paper | Click → type custom shop ID in chat |
| 2 | Comparator | Click → type number of rows (1–6) in chat |
| 3 | Beacon | Click → toggle Admin Shop mode on/off |
| 4 | Glass pane | Click → type a new filler material name in chat |
| 8 | Barrier | Cancel and discard the session |
| 9–44 | Your products | Click to edit; Shift-click to remove |
| Empty slot (after last product) | Lime glass | Click to add a new product |
| 45 | Arrow (left) | Previous page of products |
| 49 | Lime wool / Red wool | Save the shop (green when valid) |
| 53 | Arrow (right) | Next page of products |

**Tips:**
- The shop ID is auto-derived from the shop name (lowercase, spaces become underscores). You can override it by clicking the Paper slot.
- Toggling Admin Shop ON means infinite stock and no limit enforcement.
- Type `cancel` in chat at any input prompt to abort and return to the GUI.

### Product Editor (27 slots)

Clicking a product slot (or the green "add new" slot) opens the product editor.

```
[Buy $]  [Sell $]  [Amount]  [Buy Tiers]  [Sell Tiers]  [Buy Limit]  [Sell Limit]  [Reset]  [...]
                             [Item — drag here (slot 13)]
[Back]   ...                              [Save Product]
```

| Slot | Item | Action |
|---|---|---|
| 13 | Your item / green glass | **Drag your item here from your inventory** |
| 0 | Gold Ingot | Click → type buy price in chat (`none` to disable buying) |
| 1 | Iron Ingot | Click → type sell price in chat (`none` to disable selling) |
| 2 | Comparator | Click → type default amount per click |
| 3 | Ladder | Click → type buy quantity tiers, comma-separated (e.g. `1,8,16,64`), or `none` |
| 4 | Hopper | Click → type sell quantity tiers, comma-separated, or `none` |
| 5 | Barrier | Click → type personal buy limit per reset period (`-1` = unlimited) |
| 6 | Barrier | Click → type personal sell limit per reset period (`-1` = unlimited) |
| 7 | Clock | Click → cycle reset type: NEVER → DAILY → WEEKLY |
| 18 | Arrow | Back to main editor |
| 26 | Lime/Red Wool | Save product (turns green when item + at least one price are set) |

**Setting a custom item:**
1. Hold the item in your inventory.
2. Drag it into slot 13 of the product editor.
3. The slot updates with your item. If it's a custom plugin item (MMOItems, Oraxen, etc.), extra lore will show the detected plugin and PDC data confirmation.
4. All item identity data is captured automatically — no manual key configuration needed.

**Setting quantity tiers:**
- When buy tiers are set (e.g. `1,8,16,64`), clicking the product in the shop opens a sub-menu letting the buyer choose how many they want.
- Price scales with quantity: a $10 item with tier 64 costs $640.
- Leave empty to buy/sell a fixed `amount` per click.

**Save flow:**
1. Fill in item + at least one price.
2. Click Save Product (slot 26, lime wool).
3. You return to the main editor with the product added.
4. Repeat for each product.
5. Click Save Shop (slot 49) in the main editor.
6. The shop YAML is written to `plugins/LofiShop/shops/<id>.yml` and loaded immediately — no reload command needed.

---

## 10. Shop Owner — Managing Block Shops

Block shops require the `lofishop.admin` permission.

### Placing a Block Shop

1. Place any block where you want the shop (a slab works well visually).
2. Look directly at the block.
3. Run the createblock command with your chosen mode:

```
/lofishop createblock <shopId> <productKey> [FULL|SMALL|QUICK]
```

| Mode | What the player sees on right-click |
|---|---|
| `FULL` | The full shop GUI with all products |
| `SMALL` | A 3-row panel: item display in the centre, Buy button (left), Sell button (right) |
| `QUICK` | No GUI — instantly buys the default amount, confirms in chat |

The floating item appears above the block within a second. The mode is saved to `blockshops.yml`. To change the mode, remove the block shop and recreate it.

### Removing a Block Shop

Look at the block shop and run:
```
/lofishop removeblock
```

Breaking the block also removes the block shop if you have `lofishop.admin`.

---

## 11. Shop Owner — Sell Wand

The sell wand lets players sell the entire contents of a chest in one click.

### Giving a Sell Wand

```
/lofishop give sellwand <playername>
# or
/sellwand <playername>
```

Requires `lofishop.give.sellwand`.

### How It Works

1. Hold the sell wand (Blaze Rod by default).
2. Right-click any chest.
3. LofiShop scans every item in the chest, finds the best sell price across all shops, and sells everything sellable instantly.
4. The player receives a chat message with total items sold and money earned.

### Configuration

In `config.yml` under `sell-wand:`:
- `consume-on-use: true` — wand disappears after one use.
- `max-uses: 10` — wand breaks after 10 uses (tracked via PDC).
- `max-uses: -1` — infinite uses.

---

## 12. Buyer — Using Shops

### Opening a Shop

There are several ways to open a shop depending on what your server admin has set up:

1. **Block shop:** Right-click a physical block shop in the world — always available.
2. **NPC:** Right-click a Citizens NPC configured to open a shop.
3. **Command:** `/shop open <shopId>` — only if your server admin has granted you `lofishop.open.command`. Many servers intentionally restrict this so players must visit the physical shop location.

To see which shops are available:
```
/shop list
```

### Buying an Item

1. Open the shop.
2. Click the item you want to buy. If the item has quantity tiers, a sub-menu opens — click the quantity you want.
3. If you have enough money and meet any conditions, the item is added to your inventory and the price is deducted.

**You cannot buy if:**
- You don't have enough money.
- You have reached your personal buy limit for that item.
- The global buy limit for that item has been reached.
- You don't meet a condition (e.g. missing a permission node or XP level).

### Selling an Item

1. Open the shop.
2. Right-click an item that has a sell price. If it has sell tiers, a sub-menu opens.
3. Confirm the sale. Items are removed from your inventory and money is added.

**You cannot sell if:**
- You don't have the item in your inventory.
- The item doesn't match what the shop expects (exact item, including any custom plugin data).
- You have reached your personal or global sell limit.

### Limits

Some products have limits on how many you can buy or sell per day or week:
- **Daily limits** reset every day at the time configured by the server admin (default midnight).
- **Weekly limits** reset once a week on the configured day (default Monday).
- When a limit is reached, you will see a message and cannot continue until it resets.
- The item's lore (when `auto-limit-lore` is on) shows how many you have remaining.

---

## 13. Buyer — Quick Sell

Quick sell scans your entire inventory for sellable items and shows you the total value before you confirm.

### Opening Quick Sell

```
/shop quicksell
```
Or via a command block, button, or menu your server may have set up.

### How It Works

1. The menu opens showing all sellable items found in your inventory.
2. A total sell value is displayed.
3. Click **Confirm** (green button) to sell everything and receive the money.
4. Click **Cancel** (red button) to close without selling.

If your inventory has nothing sellable, you'll receive a message and no menu opens.

---

## 14. Buyer — Sell Wand (if granted)

If a server admin or shop owner has given you a sell wand:

1. Hold the **sell wand** (gold Blaze Rod) in your hand.
2. Walk up to a chest that contains items you want to sell.
3. Right-click the chest.
4. LofiShop automatically sells all sellable items inside and deposits the money to your account.
5. A message shows how many items were sold and for how much.

**Notes:**
- The wand only sells items that are in a shop's sell list.
- Items without a sell price are left in the chest untouched.
- If the wand has a use limit, each successful chest sell counts as one use.

---

## 15. Custom Plugin Items (MMOItems, Oraxen, etc.)

LofiShop natively supports items created by:

| Plugin | Supported |
|---|---|
| MMOItems | Yes |
| Oraxen | Yes |
| Nexo | Yes |
| ItemsAdder | Yes |
| MythicMobs / Crucible | Yes |
| ExecutableItems | Yes |
| Any other PDC-based plugin | Yes |

### How Matching Works

When you drag a custom item into the product editor's item slot (slot 13), LofiShop captures the full item — including all Persistent Data Container (PDC) keys — and stores it as a base64 snapshot in the shop YAML under `item-data:`.

When a player tries to sell, their item's PDC data is compared against the snapshot. If all identity keys match, the item is accepted. Mutable fields (damage, enchantments added by use) are intentionally excluded so a "used" item still matches.

### For Players

Custom items work the same as vanilla items — just place them in your inventory and use the shop normally. You do not need to do anything special.

### For Admins / Shop Owners (via Creator)

1. Get the exact custom item in your inventory (e.g. an MMOItems sword).
2. Open the product editor (creator wand → add/edit product).
3. Drag the item into slot 13.
4. Lore on the slot will confirm the detected plugin and PDC key summary, e.g.:
   ```
   ── Item Data ──
   Plugin: MMOItems
   PDC: mmoitems:item_id, mmoitems:item_type (+1 more)
   Full data captured automatically.
   ```
5. Save the product. Done — no manual key configuration required.

### Manual YAML (Advanced)

If you are writing a shop YAML by hand for a custom item, use the `item-data` field:

1. Get the base64 string from another LofiShop-created shop file (the creator writes it automatically).
2. Or use the creator to generate the YAML, then copy the `item-data` value to another shop file.

```yaml
products:
  sword:
    item:
      material: DIAMOND_SWORD     # human-readable hint only
      item-data: "<base64 string here>"
    buy-price:
      - type: vault
        amount: 5000.0
    amount: 1
```

If `item-data` is present, it takes priority over `material`/`name`/`lore`.

---

## 16. Troubleshooting

### Shop doesn't load after reload

Check the console for errors. Common causes:
- Malformed YAML (wrong indentation, tabs instead of spaces).
- Unknown material name — ensure it matches the exact Bukkit `Material` enum name (all caps, underscores).
- Missing required fields (`buy-price` or `sell-price` both absent — product won't be interactive but will still display).

### "Economy provider not found"

Vault cannot find an economy plugin. Ensure EssentialsX (or another Vault economy provider) is installed and loaded before LofiShop. Check startup logs for Vault registration messages.

### Custom item not matching when selling

- The item must be **identical** in plugin identity to the one captured in the shop. If the plugin version updated and changed PDC keys, the shop entry needs to be re-saved via the creator.
- Ensure you are not trying to sell a different tier or variant of the item.

### Block shop floating item not appearing

Run `/lofishop reload` — block shops are respawned on plugin enable. If the display still doesn't appear, check that the world is loaded and the block still exists at the stored location.

### Players can't open a shop via command

Players need `lofishop.open.command` to use `/shop open`. This is intentionally `op`-only by default so players are required to visit physical block shops or NPCs. Grant it explicitly if you want players to use the command:
```
lp group default permission set lofishop.open.command true
```

### Players can't open a shop via block or NPC

1. Check the shop's `open-permission` field — if set, the player needs that permission node.
2. Verify they have `lofishop.use` (which grants `lofishop.open`).
3. Check the console for limit or condition failures.

### Limits not resetting

Verify `daily-reset-time` and `weekly-reset-day` in `config.yml`. Limit data is stored in `plugins/LofiShop/limits/`. Deleting the entire `limits/` folder resets all limits (use with caution on live servers).

---

*LofiShop — [github.com/Lord-Lofi/LofiShop](https://github.com/Lord-Lofi/LofiShop)*
