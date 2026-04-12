# LofiShop

A powerful, configurable shop plugin for Paper 1.21.x — built as a feature-complete alternative to UltimateShop.

## Features

- **GUI-based shops** — fully configurable chest-style menus built from YAML
- **In-game shop creator** — build shops with a wand and GUI wizard, no YAML editing required
- **Admin shops** — infinite stock, no restocking ever needed
- **Block shops** — attach any shop product to a physical block with a floating animated item above it
- **Quantity tiers** — let buyers choose 1, 8, 16, or 64 at a time from a picker sub-menu
- **Custom item support** — full compatibility with MMOItems, Oraxen, Nexo, ItemsAdder, MythicMobs, MobHeads, HeadDatabase, and any PDC-based plugin
- **Skull/head matching** — decorative heads matched by skin texture, not just material
- **Sell wand** — right-click a chest to sell its entire contents automatically
- **Quick sell** — scan your inventory and sell everything in one confirm click
- **Per-product limits** — personal and global buy/sell limits with daily, weekly, or cron resets
- **Actions & conditions** — trigger sounds, messages, titles, and commands on transactions; gate purchases behind permissions, balance, level, or PlaceholderAPI values
- **LuckPerms-compatible permissions** — fine-grained per-shop and per-product nodes
- **PlaceholderAPI support** — expose balance and limit data to other plugins
- **MiniMessage formatting** — full gradient/color support everywhere
- **Hot reload** — `/lofishop reload` with no server restart

## Requirements

| Dependency | Required |
|---|---|
| [Paper](https://papermc.io) 1.21.x | Yes |
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | Yes |
| [EssentialsX](https://essentialsx.net) (or another Vault economy) | Yes |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | No |
| [LuckPerms](https://luckperms.net) | No |

## Installation

1. Drop `LofiShop.jar` into your server's `plugins/` folder alongside Vault and EssentialsX.
2. Start the server — config files and example shops generate automatically.
3. Configure `plugins/LofiShop/config.yml` to your liking.
4. Use `/lofishop reload` to apply changes without restarting.

## Quick Start

### Open a shop
```
/shop open example
```

### Give yourself the shop creator wand
```
/lofishop givecreator
```
Right-click with the Nether Star to open the shop editor GUI.

### Give a sell wand
```
/lofishop give sellwand <player>
```

### Create a block shop
Look at a block and run:
```
/lofishop createblock <shopId> <productKey>
```

## Commands

| Command | Description |
|---|---|
| `/shop open <id>` | Open a shop |
| `/shop list` | List all shops |
| `/shop quicksell` | Open quick-sell menu |
| `/shop reload` | Reload all configs and shops |
| `/shop give sellwand [player]` | Give a sell wand |
| `/shop givecreator [player]` | Give the shop creator wand |
| `/shop createblock <shopId> <productKey>` | Attach a block shop to the block you're looking at |
| `/shop removeblock` | Remove the nearest block shop |
| `/sellwand [player]` | Shorthand sell wand give |

Aliases: `/shop`, `/ls`, `/lofishop`

## Permissions

| Node | Default | Description |
|---|---|---|
| `lofishop.admin` | op | Full access — grants everything |
| `lofishop.use` | true | Base player access |
| `lofishop.open.<shopId>` | — | Open a specific shop |
| `lofishop.buy.<shopId>` | — | Buy in a specific shop |
| `lofishop.buy.<shopId>.<productId>` | — | Buy a specific product |
| `lofishop.sell.<shopId>` | — | Sell in a specific shop |
| `lofishop.bypass.limits` | op | Ignore all buy/sell limits |
| `lofishop.bypass.conditions` | op | Ignore all product conditions |
| `lofishop.creator` | op | Use the shop creator wand |

See [docs/user-guide.md](docs/user-guide.md) for the full permission tree.

## Shop YAML Example

```yaml
shop-name: "<gold>Example Shop"
title: "<dark_gray>[ <gold>Example Shop</gold> ]"
rows: 6
admin-shop: false

layout:
  - "F F F F F F F F F"
  - "F d F i F F F F F"
  - "F F F F F F F F F"
  - "F F F F F F F F F"
  - "F F F F F F F F F"
  - "F F F F F F F F F"

products:
  d:
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
    buy-amounts: [1, 8, 16, 64]
    sell-amounts: [1, 8, 16, 64]
    limits:
      personal-buy: 64
      reset: DAILY
```

## Custom Plugin Items

Drag any custom item into the shop creator — LofiShop captures it automatically.

| Plugin | How matched |
|---|---|
| MMOItems, Oraxen, Nexo, ItemsAdder, MythicMobs, ExecutableItems | PDC identity keys |
| MobHeads, HeadDatabase, decorative heads | Skull texture property |
| Vanilla + resource pack | Custom Model Data |
| Plain vanilla | Material |

No manual key configuration needed. The creator wand GUI confirms which plugin was detected and what data was captured.

## PlaceholderAPI

| Placeholder | Returns |
|---|---|
| `%lofishop_balance%` | Player's balance |
| `%lofishop_balance_formatted%` | Balance with currency symbol |
| `%lofishop_buy_limit_<shopId>_<productId>%` | Remaining personal buy limit |
| `%lofishop_sell_limit_<shopId>_<productId>%` | Remaining personal sell limit |

## Documentation

Full setup and usage guide: [docs/user-guide.md](docs/user-guide.md)

Covers:
- Server admin installation and configuration
- YAML shop format reference (all fields, actions, conditions)
- In-game shop creator walkthrough
- Block shop setup
- Sell wand and quick sell
- LuckPerms example setup
- Custom plugin item workflow
- Troubleshooting

## License

See [LICENSE](LICENSE).
