# MoreWeapons

MoreWeapons is a NeoForge 1.21.1 weapon expansion for Minecraft Beyond. It adds vanilla-tier weapon families, early combat behavior tweaks, and data hooks for the pack's modular tool and enchanting systems.

## Current Status

Active internal playtesting on the `1.21.1-neoforge` branch. The core weapon set, recipes, models, and pack integrations are present; balance and final art are still in progress.

## Project Facts

- Mod id: `mobsmoreweapons`
- Current version: `0.4-1.21.1-neoforge`
- Target: Minecraft 1.21.1, NeoForge 21.1.234, Java 21
- Optional integrations: Mobs Tool Forging 0.1.0+, Better Enchanting, and Punchy
- Common config: `config/mobsmoreweapons-common.toml`

## Current Features

- Five weapon families across wood, stone, iron, gold, diamond, and netherite tiers:
  - Great Swords
  - Katanas
  - Battle Axes
  - Knives
  - Machetes
- Katanas have extended interaction range; knives have reduced interaction range.
- Optional player bow accuracy fix, disabled by default, with configurable full-draw strain timing.
- Recipes, item models, textures, and vanilla item tags for the main weapons.
- Optional Mobs Tool Forging bridge data for great sword, katana, battle axe, knife, and machete tool types.
- Mobs Tool Forging part items, forge templates, stat rules, tool visuals, and JEI-facing data.
- Better Enchanting target tags, tag display definitions, enchantment limits, and part-to-finished-weapon routing.
- Canonical family tags for all five weapon types so combat and enchanting mods can target the families without enumerating every material tier.
- Punchy compatibility metadata for first-person animations across the complete weapon set.

## Configuration

The common config is `mobsmoreweapons-common.toml`.

- `ranged_combat.player_bow_accuracy_fix=false` keeps vanilla bow spread by default.
- `ranged_combat.bow_strain_grace_ticks=60` controls how long a fully drawn bow stays perfectly steady after the optional accuracy fix is enabled.

## Supported Versions

- Minecraft 1.21.1
- NeoForge 21.1.234
- Java 21

## Minecraft Beyond Integration

Minecraft Beyond removes the direct recipes for these weapons so Mobs Tool Forging owns their normal progression. MoreWeapons supplies the bridge definitions for shaping and assembling its weapon parts, while Better Enchanting and Mobs Combat consume the shared weapon-family tags. Punchy uses the bundled compatibility metadata for first-person animation classification.

The standalone mod still includes conventional recipes and does not require the other local projects unless a pack chooses to use those integrations.

## Building

```sh
./gradlew build
```

The built jar is written to `build/libs/`.

## Known Limitations

- Weapon family balance is still tuned for pack playtesting, not public release.
- Weapon families currently differ primarily through attributes, reach where applicable, tags, and integration data; most do not yet have bespoke attack mechanics.
- Mobs Tool Forging integration depends on that mod being present in the pack.

## License

MIT. See [LICENSE](LICENSE).
