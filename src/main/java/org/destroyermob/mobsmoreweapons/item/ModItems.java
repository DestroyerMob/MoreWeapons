package org.destroyermob.mobsmoreweapons.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.destroyermob.mobsmoreweapons.MoreWeapons;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MoreWeapons.MOD_ID);

    // Greatswords
    public static final DeferredItem<Item> WOODENGREATSWORD = ITEMS.register("wooden_great_sword",
            () -> new SwordItem(Tiers.WOOD, weaponProperties(Tiers.WOOD, 12, -3.5f)));
    public static final DeferredItem<Item> STONEGREATSWORD = ITEMS.register("stone_great_sword",
            () -> new SwordItem(Tiers.STONE, weaponProperties(Tiers.STONE, 12, -3.5f)));
    public static final DeferredItem<Item> IRONGREATSWORD = ITEMS.register("iron_great_sword",
            () -> new SwordItem(Tiers.IRON, weaponProperties(Tiers.IRON, 12, -3.5f)));
    public static final DeferredItem<Item> GOLDGREATSWORD = ITEMS.register("golden_great_sword",
            () -> new SwordItem(Tiers.GOLD, weaponProperties(Tiers.GOLD, 12, -3.5f)));
    public static final DeferredItem<Item> DIAMONDGREATSWORD = ITEMS.register("diamond_great_sword",
            () -> new SwordItem(Tiers.DIAMOND, weaponProperties(Tiers.DIAMOND, 12, -3.5f)));
    public static final DeferredItem<Item> NETHERITEGREATSWORD = ITEMS.register("netherite_great_sword",
            () -> new SwordItem(Tiers.NETHERITE, weaponProperties(Tiers.NETHERITE, 12, -3.5f)));

    // Katanas
    public static final DeferredItem<Item> WOODENKATANA = ITEMS.register("wooden_katana",
            () -> new SwordItem(Tiers.WOOD, weaponProperties(Tiers.WOOD, 2, -2.1f)));
    public static final DeferredItem<Item> STONEKATANA = ITEMS.register("stone_katana",
            () -> new SwordItem(Tiers.STONE, weaponProperties(Tiers.STONE, 2, -2.1f)));
    public static final DeferredItem<Item> IRONKATANA = ITEMS.register("iron_katana",
            () -> new SwordItem(Tiers.IRON, weaponProperties(Tiers.IRON, 2, -2.1f)));
    public static final DeferredItem<Item> GOLDKATANA = ITEMS.register("golden_katana",
            () -> new SwordItem(Tiers.GOLD, weaponProperties(Tiers.GOLD, 2, -2.1f)));
    public static final DeferredItem<Item> DIAMONDKATANA = ITEMS.register("diamond_katana",
            () -> new SwordItem(Tiers.DIAMOND, weaponProperties(Tiers.DIAMOND, 2, -2.1f)));
    public static final DeferredItem<Item> NETHERITEKATANA = ITEMS.register("netherite_katana",
            () -> new SwordItem(Tiers.NETHERITE, weaponProperties(Tiers.NETHERITE, 2, -2.1f)));

    // Battle Axes
    public static final DeferredItem<Item> WOODENBATTLEAXE = ITEMS.register("wooden_battle_axe",
            () -> new SwordItem(Tiers.WOOD, weaponProperties(Tiers.WOOD, 7, -3f)));
    public static final DeferredItem<Item> STONEBATTLEAXE = ITEMS.register("stone_battle_axe",
            () -> new SwordItem(Tiers.STONE, weaponProperties(Tiers.STONE, 8, -3f)));
    public static final DeferredItem<Item> IRONBATTLEAXE = ITEMS.register("iron_battle_axe",
            () -> new SwordItem(Tiers.IRON, weaponProperties(Tiers.IRON, 7, -3f)));
    public static final DeferredItem<Item> GOLDBATTLEAXE = ITEMS.register("golden_battle_axe",
            () -> new SwordItem(Tiers.GOLD, weaponProperties(Tiers.GOLD, 7, -3f)));
    public static final DeferredItem<Item> DIAMONDBATTLEAXE = ITEMS.register("diamond_battle_axe",
            () -> new SwordItem(Tiers.DIAMOND, weaponProperties(Tiers.DIAMOND, 6, -3f)));
    public static final DeferredItem<Item> NETHERITEBATTLEAXE = ITEMS.register("netherite_battle_axe",
            () -> new SwordItem(Tiers.NETHERITE, weaponProperties(Tiers.NETHERITE, 6, -3f)));

    // Knives
    public static final DeferredItem<Item> WOODENKNIFE = ITEMS.register("wooden_knife",
            () -> new SwordItem(Tiers.WOOD, weaponProperties(Tiers.WOOD, 1, -2f)));
    public static final DeferredItem<Item> STONEKNIFE = ITEMS.register("stone_knife",
            () -> new SwordItem(Tiers.STONE, weaponProperties(Tiers.STONE, 1, -2f)));
    public static final DeferredItem<Item> IRONKNIFE = ITEMS.register("iron_knife",
            () -> new SwordItem(Tiers.IRON, weaponProperties(Tiers.IRON, 1, -2f)));
    public static final DeferredItem<Item> GOLDENKNIFE = ITEMS.register("golden_knife",
            () -> new SwordItem(Tiers.GOLD, weaponProperties(Tiers.GOLD, 1, -2f)));
    public static final DeferredItem<Item> DIAMONDKNIFE = ITEMS.register("diamond_knife",
            () -> new SwordItem(Tiers.DIAMOND, weaponProperties(Tiers.DIAMOND, 1, -2f)));
    public static final DeferredItem<Item> NETHERITEKNIFE = ITEMS.register("netherite_knife",
            () -> new SwordItem(Tiers.NETHERITE, weaponProperties(Tiers.NETHERITE, 1, -2f)));

    // Machetes
    public static final DeferredItem<Item> WOODENMACHETE = ITEMS.register("wooden_machete",
            () -> new SwordItem(Tiers.WOOD, weaponProperties(Tiers.WOOD, 5, -2.7f)));
    public static final DeferredItem<Item> STONEMACHETE = ITEMS.register("stone_machete",
            () -> new SwordItem(Tiers.STONE, weaponProperties(Tiers.STONE, 5, -2.7f)));
    public static final DeferredItem<Item> IRONMACHETE = ITEMS.register("iron_machete",
            () -> new SwordItem(Tiers.IRON, weaponProperties(Tiers.IRON, 5, -2.7f)));
    public static final DeferredItem<Item> GOLDENMACHETE = ITEMS.register("golden_machete",
            () -> new SwordItem(Tiers.GOLD, weaponProperties(Tiers.GOLD, 5, -2.7f)));
    public static final DeferredItem<Item> DIAMONDMACHETE = ITEMS.register("diamond_machete",
            () -> new SwordItem(Tiers.DIAMOND, weaponProperties(Tiers.DIAMOND, 5, -2.7f)));
    public static final DeferredItem<Item> NETHERITEMACHETE = ITEMS.register("netherite_machete",
            () -> new SwordItem(Tiers.NETHERITE, weaponProperties(Tiers.NETHERITE, 5, -2.7f)));

    // Mobs Tool Forging compatibility parts
    public static final DeferredItem<Item> IRONGREATSWORDBLADE = part("iron_great_sword_blade");
    public static final DeferredItem<Item> GOLDENGREATSWORDBLADE = part("golden_great_sword_blade");
    public static final DeferredItem<Item> DIAMONDGREATSWORDBLADE = part("diamond_great_sword_blade");

    public static final DeferredItem<Item> IRONKATANABLADE = part("iron_katana_blade");
    public static final DeferredItem<Item> GOLDENKATANABLADE = part("golden_katana_blade");
    public static final DeferredItem<Item> DIAMONDKATANABLADE = part("diamond_katana_blade");

    public static final DeferredItem<Item> IRONBATTLEAXEHEAD = part("iron_battle_axe_head");
    public static final DeferredItem<Item> GOLDENBATTLEAXEHEAD = part("golden_battle_axe_head");
    public static final DeferredItem<Item> DIAMONDBATTLEAXEHEAD = part("diamond_battle_axe_head");

    public static final DeferredItem<Item> IRONKNIFEBLADE = part("iron_knife_blade");
    public static final DeferredItem<Item> GOLDENKNIFEBLADE = part("golden_knife_blade");
    public static final DeferredItem<Item> DIAMONDKNIFEBLADE = part("diamond_knife_blade");

    public static final DeferredItem<Item> IRONMACHETEBLADE = part("iron_machete_blade");
    public static final DeferredItem<Item> GOLDENMACHETEBLADE = part("golden_machete_blade");
    public static final DeferredItem<Item> DIAMONDMACHETEBLADE = part("diamond_machete_blade");

    public static final DeferredItem<Item> IRONWIDEGUARD = part("iron_wide_guard");
    public static final DeferredItem<Item> GOLDENWIDEGUARD = part("golden_wide_guard");
    public static final DeferredItem<Item> DIAMONDWIDEGUARD = part("diamond_wide_guard");


    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private static DeferredItem<Item> part(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static Item.Properties weaponProperties(Tier tier, int attackDamage, float attackSpeed) {
        return new Item.Properties()
                .durability(tier.getUses())
                .attributes(SwordItem.createAttributes(tier, attackDamage, attackSpeed));
    }

}
