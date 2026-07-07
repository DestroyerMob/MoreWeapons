# MoreWeapons

MoreWeapons is a NeoForge 1.21.1 weapon expansion for Minecraft Beyond. It adds vanilla-tier weapon families, early combat behavior tweaks, and data hooks for the pack's modular tool and enchanting systems.

## Current Status

Active internal playtesting on the `1.21.1-neoforge` branch. The core weapon set, recipes, models, and pack integrations are present; balance and final art are still in progress.

## Project Facts

- Mod id: `mobsmoreweapons`
- Current version: `0.4-1.21.1-neoforge`
- Target: Minecraft 1.21.1, NeoForge 21.1.234, Java 21
- Optional integration: Mobs Tool Forging 0.1.0 or newer
- Common config: `config/mobsmoreweapons-common.toml`

## Current Features

- Five weapon families across wood, stone, iron, gold, diamond, and netherite tiers:
  - Great Swords
  - Katanas
  - Battle Axes
  - Knives
  - Machetes
- Katanas can block with an old-style sword blocking behavior.
- Katanas have extended interaction range; knives have reduced interaction range.
- Optional player bow accuracy fix, disabled by default, with configurable full-draw strain timing.
- Recipes, item models, textures, and vanilla item tags for the main weapons.
- Optional Mobs Tool Forging bridge data for great sword, katana, battle axe, knife, and machete tool types.
- Mobs Tool Forging part items, forge templates, tool visuals, and JEI integration data.
- Better Enchanting tag display and enchantment target data for modular weapon parts.

## Configuration

The common config is `mobsmoreweapons-common.toml`.

- `ranged_combat.player_bow_accuracy_fix=false` keeps vanilla bow spread by default.
- `ranged_combat.bow_strain_grace_ticks=60` controls how long a fully drawn bow stays perfectly steady after the optional accuracy fix is enabled.

## Supported Versions

- Minecraft 1.21.1
- NeoForge 21.1.234
- Java 21

## Building

```sh
./gradlew build
```

The built jar is written to `build/libs/`.

## Known Limitations

- Weapon family balance is still tuned for pack playtesting, not public release.
- Great Sword, Battle Axe, Knife, and Machete classes currently use base sword behavior aside from their attributes and integration data.
- Mobs Tool Forging integration depends on that mod being present in the pack.

## License

MIT. See [LICENSE](LICENSE).
