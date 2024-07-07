package org.destroyermob.mobsmoreweapons.item;

import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.destroyermob.mobsmoreweapons.MoreWeapons;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MoreWeapons.MOD_ID);

    // Greatswords
    public static final RegistryObject<Item> WOODENGREATSWORD = ITEMS.register("wooden_great_sword",
            () -> new SwordItem(Tiers.WOOD, 12, -3.5f, new Item.Properties()));
    public static final RegistryObject<Item> STONEGREATSWORD = ITEMS.register("stone_great_sword",
            () -> new SwordItem(Tiers.STONE, 12, -3.5f, new Item.Properties()));
    public static final RegistryObject<Item> IRONGREATSWORD = ITEMS.register("iron_great_sword",
            () -> new SwordItem(Tiers.IRON, 12, -3.5f, new Item.Properties()));
    public static final RegistryObject<Item> GOLDGREATSWORD = ITEMS.register("golden_great_sword",
            () -> new SwordItem(Tiers.GOLD, 12, -3.5f, new Item.Properties()));
    public static final RegistryObject<Item> DIAMONDGREATSWORD = ITEMS.register("diamond_great_sword",
            () -> new SwordItem(Tiers.DIAMOND, 12, -3.5f, new Item.Properties()));
    public static final RegistryObject<Item> NETHERITEGREATSWORD = ITEMS.register("netherite_great_sword",
            () -> new SwordItem(Tiers.NETHERITE, 12, -3.5f, new Item.Properties()));

    // Katanas
    public static final RegistryObject<Item> WOODENKATANA = ITEMS.register("wooden_katana",
            () -> new SwordItem(Tiers.WOOD, 2, -2.1f, new Item.Properties()));
    public static final RegistryObject<Item> STONEKATANA = ITEMS.register("stone_katana",
            () -> new SwordItem(Tiers.STONE, 2, -2.1f, new Item.Properties()));
    public static final RegistryObject<Item> IRONKATANA = ITEMS.register("iron_katana",
            () -> new SwordItem(Tiers.IRON, 2, -2.1f, new Item.Properties()));
    public static final RegistryObject<Item> GOLDKATANA = ITEMS.register("golden_katana",
            () -> new SwordItem(Tiers.GOLD, 2, -2.1f, new Item.Properties()));
    public static final RegistryObject<Item> DIAMONDKATANA = ITEMS.register("diamond_katana",
            () -> new SwordItem(Tiers.DIAMOND, 2, -2.1f, new Item.Properties()));
    public static final RegistryObject<Item> NETHERITEKATANA = ITEMS.register("netherite_katana",
            () -> new SwordItem(Tiers.NETHERITE, 2, -2.1f, new Item.Properties()));

    // Battle Axes
    public static final RegistryObject<Item> WOODENBATTLEAXE = ITEMS.register("wooden_battle_axe",
            () -> new SwordItem(Tiers.WOOD, 7, -3f, new Item.Properties()));
    public static final RegistryObject<Item> STONEBATTLEAXE = ITEMS.register("stone_battle_axe",
            () -> new SwordItem(Tiers.STONE, 8, -3f, new Item.Properties()));
    public static final RegistryObject<Item> IRONBATTLEAXE = ITEMS.register("iron_battle_axe",
            () -> new SwordItem(Tiers.IRON, 7, -3f, new Item.Properties()));
    public static final RegistryObject<Item> GOLDBATTLEAXE = ITEMS.register("golden_battle_axe",
            () -> new SwordItem(Tiers.GOLD, 7, -3f, new Item.Properties()));
    public static final RegistryObject<Item> DIAMONDBATTLEAXE = ITEMS.register("diamond_battle_axe",
            () -> new SwordItem(Tiers.DIAMOND, 6, -3f, new Item.Properties()));
    public static final RegistryObject<Item> NETHERITEBATTLEAXE = ITEMS.register("netherite_battle_axe",
            () -> new SwordItem(Tiers.NETHERITE, 6, -3f, new Item.Properties()));

    // Knives
    public static final RegistryObject<Item> WOODENKNIFE = ITEMS.register("wooden_knife",
            () -> new SwordItem(Tiers.WOOD, 1, -2f, new Item.Properties()));
    public static final RegistryObject<Item> STONEKNIFE = ITEMS.register("stone_knife",
            () -> new SwordItem(Tiers.STONE, 1, -2f, new Item.Properties()));
    public static final RegistryObject<Item> IRONKNIFE = ITEMS.register("iron_knife",
            () -> new SwordItem(Tiers.IRON, 1, -2f, new Item.Properties()));
    public static final RegistryObject<Item> GOLDENKNIFE = ITEMS.register("golden_knife",
            () -> new SwordItem(Tiers.GOLD, 1, -2f, new Item.Properties()));
    public static final RegistryObject<Item> DIAMONDKNIFE = ITEMS.register("diamond_knife",
            () -> new SwordItem(Tiers.DIAMOND, 1, -2f, new Item.Properties()));
    public static final RegistryObject<Item> NETHERITEKNIFE = ITEMS.register("netherite_knife",
            () -> new SwordItem(Tiers.NETHERITE, 1, -2f, new Item.Properties()));

    // Machetes
    public static final RegistryObject<Item> WOODENMACHETE = ITEMS.register("wooden_machete",
            () -> new SwordItem(Tiers.WOOD, 6, -2.5f, new Item.Properties()));
    public static final RegistryObject<Item> STONEMACHETE = ITEMS.register("stone_machete",
            () -> new SwordItem(Tiers.STONE, 6, -2.5f, new Item.Properties()));
    public static final RegistryObject<Item> IRONMACHETE = ITEMS.register("iron_machete",
            () -> new SwordItem(Tiers.IRON, 6, -2.5f, new Item.Properties()));
    public static final RegistryObject<Item> GOLDENMACHETE = ITEMS.register("golden_machete",
            () -> new SwordItem(Tiers.GOLD, 6, -2.5f, new Item.Properties()));
    public static final RegistryObject<Item> DIAMONDMACHETE = ITEMS.register("diamond_machete",
            () -> new SwordItem(Tiers.DIAMOND, 6, -2.5f, new Item.Properties()));
    public static final RegistryObject<Item> NETHERITEMACHETE = ITEMS.register("netherite_machete",
            () -> new SwordItem(Tiers.NETHERITE, 6, -2.5f, new Item.Properties()));


    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

}
