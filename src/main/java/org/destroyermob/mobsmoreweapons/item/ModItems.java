package org.destroyermob.mobsmoreweapons.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.item.tier.ModTiers;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MoreWeapons.MOD_ID);
    private static final float GREAT_SWORD_ATTACK_SPEED = -3.625F;
    private static final ResourceLocation KATANA_REACH_ID = ResourceLocation.fromNamespaceAndPath(MoreWeapons.MOD_ID, "katana_reach");
    private static final ResourceLocation KNIFE_REACH_ID = ResourceLocation.fromNamespaceAndPath(MoreWeapons.MOD_ID, "knife_reach");

    // Greatswords
    public static final DeferredItem<Item> WOODENGREATSWORD = ITEMS.register("wooden_great_sword",
            () -> new GreatSwordItem(Tiers.WOOD, weaponProperties(Tiers.WOOD, 12, GREAT_SWORD_ATTACK_SPEED)));
    public static final DeferredItem<Item> STONEGREATSWORD = ITEMS.register("stone_great_sword",
            () -> new GreatSwordItem(Tiers.STONE, weaponProperties(Tiers.STONE, 12, GREAT_SWORD_ATTACK_SPEED)));
    public static final DeferredItem<Item> IRONGREATSWORD = ITEMS.register("iron_great_sword",
            () -> new GreatSwordItem(Tiers.IRON, weaponProperties(Tiers.IRON, 12, GREAT_SWORD_ATTACK_SPEED)));
    public static final DeferredItem<Item> GOLDGREATSWORD = ITEMS.register("golden_great_sword",
            () -> new GreatSwordItem(Tiers.GOLD, weaponProperties(Tiers.GOLD, 12, GREAT_SWORD_ATTACK_SPEED)));
    public static final DeferredItem<Item> DIAMONDGREATSWORD = ITEMS.register("diamond_great_sword",
            () -> new GreatSwordItem(Tiers.DIAMOND, weaponProperties(Tiers.DIAMOND, 12, GREAT_SWORD_ATTACK_SPEED)));
    public static final DeferredItem<Item> NETHERITEGREATSWORD = ITEMS.register("netherite_great_sword",
            () -> new GreatSwordItem(Tiers.NETHERITE, weaponProperties(Tiers.NETHERITE, 12, GREAT_SWORD_ATTACK_SPEED)));

    // Katanas
    public static final DeferredItem<Item> WOODENKATANA = ITEMS.register("wooden_katana",
            () -> new KatanaItem(Tiers.WOOD, weaponProperties(Tiers.WOOD, 2, -2.1f, KATANA_REACH_ID, 2.0D)));
    public static final DeferredItem<Item> STONEKATANA = ITEMS.register("stone_katana",
            () -> new KatanaItem(Tiers.STONE, weaponProperties(Tiers.STONE, 2, -2.1f, KATANA_REACH_ID, 2.0D)));
    public static final DeferredItem<Item> IRONKATANA = ITEMS.register("iron_katana",
            () -> new KatanaItem(Tiers.IRON, weaponProperties(Tiers.IRON, 2, -2.1f, KATANA_REACH_ID, 2.0D)));
    public static final DeferredItem<Item> GOLDKATANA = ITEMS.register("golden_katana",
            () -> new KatanaItem(Tiers.GOLD, weaponProperties(Tiers.GOLD, 2, -2.1f, KATANA_REACH_ID, 2.0D)));
    public static final DeferredItem<Item> DIAMONDKATANA = ITEMS.register("diamond_katana",
            () -> new KatanaItem(Tiers.DIAMOND, weaponProperties(Tiers.DIAMOND, 2, -2.1f, KATANA_REACH_ID, 2.0D)));
    public static final DeferredItem<Item> NETHERITEKATANA = ITEMS.register("netherite_katana",
            () -> new KatanaItem(Tiers.NETHERITE, weaponProperties(Tiers.NETHERITE, 2, -2.1f, KATANA_REACH_ID, 2.0D)));

    // Battle Axes
    public static final DeferredItem<Item> WOODENBATTLEAXE = ITEMS.register("wooden_battle_axe",
            () -> new BattleAxeItem(Tiers.WOOD, weaponProperties(Tiers.WOOD, 7, -3f)));
    public static final DeferredItem<Item> STONEBATTLEAXE = ITEMS.register("stone_battle_axe",
            () -> new BattleAxeItem(Tiers.STONE, weaponProperties(Tiers.STONE, 8, -3f)));
    public static final DeferredItem<Item> IRONBATTLEAXE = ITEMS.register("iron_battle_axe",
            () -> new BattleAxeItem(Tiers.IRON, weaponProperties(Tiers.IRON, 7, -3f)));
    public static final DeferredItem<Item> GOLDBATTLEAXE = ITEMS.register("golden_battle_axe",
            () -> new BattleAxeItem(Tiers.GOLD, weaponProperties(Tiers.GOLD, 7, -3f)));
    public static final DeferredItem<Item> DIAMONDBATTLEAXE = ITEMS.register("diamond_battle_axe",
            () -> new BattleAxeItem(Tiers.DIAMOND, weaponProperties(Tiers.DIAMOND, 6, -3f)));
    public static final DeferredItem<Item> NETHERITEBATTLEAXE = ITEMS.register("netherite_battle_axe",
            () -> new BattleAxeItem(Tiers.NETHERITE, weaponProperties(Tiers.NETHERITE, 6, -3f)));

    // Knives
    public static final DeferredItem<Item> WOODENKNIFE = ITEMS.register("wooden_knife",
            () -> new KnifeItem(Tiers.WOOD, weaponProperties(Tiers.WOOD, 1, -2f, KNIFE_REACH_ID, -1.0D)));
    public static final DeferredItem<Item> STONEKNIFE = ITEMS.register("stone_knife",
            () -> new KnifeItem(Tiers.STONE, weaponProperties(Tiers.STONE, 1, -2f, KNIFE_REACH_ID, -1.0D)));
    public static final DeferredItem<Item> IRONKNIFE = ITEMS.register("iron_knife",
            () -> new KnifeItem(Tiers.IRON, weaponProperties(Tiers.IRON, 1, -2f, KNIFE_REACH_ID, -1.0D)));
    public static final DeferredItem<Item> GOLDENKNIFE = ITEMS.register("golden_knife",
            () -> new KnifeItem(Tiers.GOLD, weaponProperties(Tiers.GOLD, 1, -2f, KNIFE_REACH_ID, -1.0D)));
    public static final DeferredItem<Item> DIAMONDKNIFE = ITEMS.register("diamond_knife",
            () -> new KnifeItem(Tiers.DIAMOND, weaponProperties(Tiers.DIAMOND, 1, -2f, KNIFE_REACH_ID, -1.0D)));
    public static final DeferredItem<Item> NETHERITEKNIFE = ITEMS.register("netherite_knife",
            () -> new KnifeItem(Tiers.NETHERITE, weaponProperties(Tiers.NETHERITE, 1, -2f, KNIFE_REACH_ID, -1.0D)));

    // Machetes
    public static final DeferredItem<Item> WOODENMACHETE = ITEMS.register("wooden_machete",
            () -> new MacheteItem(Tiers.WOOD, weaponProperties(Tiers.WOOD, 5, -2.7f)));
    public static final DeferredItem<Item> STONEMACHETE = ITEMS.register("stone_machete",
            () -> new MacheteItem(Tiers.STONE, weaponProperties(Tiers.STONE, 5, -2.7f)));
    public static final DeferredItem<Item> IRONMACHETE = ITEMS.register("iron_machete",
            () -> new MacheteItem(Tiers.IRON, weaponProperties(Tiers.IRON, 5, -2.7f)));
    public static final DeferredItem<Item> GOLDENMACHETE = ITEMS.register("golden_machete",
            () -> new MacheteItem(Tiers.GOLD, weaponProperties(Tiers.GOLD, 5, -2.7f)));
    public static final DeferredItem<Item> DIAMONDMACHETE = ITEMS.register("diamond_machete",
            () -> new MacheteItem(Tiers.DIAMOND, weaponProperties(Tiers.DIAMOND, 5, -2.7f)));
    public static final DeferredItem<Item> NETHERITEMACHETE = ITEMS.register("netherite_machete",
            () -> new MacheteItem(Tiers.NETHERITE, weaponProperties(Tiers.NETHERITE, 5, -2.7f)));

    // Spears
    public static final DeferredItem<Item> WOODENSPEAR = ITEMS.register("wooden_spear",
            () -> new SpearItem(Tiers.WOOD, spearProperties(Tiers.WOOD, 0, -2.46f)));
    public static final DeferredItem<Item> STONESPEAR = ITEMS.register("stone_spear",
            () -> new SpearItem(Tiers.STONE, spearProperties(Tiers.STONE, 0, -2.67f)));
    public static final DeferredItem<Item> COPPERSPEAR = ITEMS.register("copper_spear",
            () -> new SpearItem(ModTiers.COPPER, spearProperties(ModTiers.COPPER, 0, -2.82f)));
    public static final DeferredItem<Item> IRONSPEAR = ITEMS.register("iron_spear",
            () -> new SpearItem(Tiers.IRON, spearProperties(Tiers.IRON, 0, -2.95f)));
    public static final DeferredItem<Item> GOLDENSPEAR = ITEMS.register("golden_spear",
            () -> new SpearItem(Tiers.GOLD, spearProperties(Tiers.GOLD, 0, -2.95f)));
    public static final DeferredItem<Item> DIAMONDSPEAR = ITEMS.register("diamond_spear",
            () -> new SpearItem(Tiers.DIAMOND, spearProperties(Tiers.DIAMOND, 0, -3.05f)));
    public static final DeferredItem<Item> NETHERITESPEAR = ITEMS.register("netherite_spear",
            () -> new SpearItem(Tiers.NETHERITE, spearProperties(Tiers.NETHERITE, 0, -3.13f)));

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

    public static final DeferredItem<Item> COPPERSPEARHEAD = part("copper_spear_head");
    public static final DeferredItem<Item> IRONSPEARHEAD = part("iron_spear_head");
    public static final DeferredItem<Item> GOLDENSPEARHEAD = part("golden_spear_head");
    public static final DeferredItem<Item> DIAMONDSPEARHEAD = part("diamond_spear_head");

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

    private static Item.Properties weaponProperties(Tier tier, int attackDamage, float attackSpeed, ResourceLocation reachId, double reachModifier) {
        ItemAttributeModifiers attributes = SwordItem.createAttributes(tier, attackDamage, attackSpeed)
                .withModifierAdded(
                        Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(reachId, reachModifier, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                );
        return new Item.Properties()
                .durability(tier.getUses())
                .attributes(attributes);
    }

    private static Item.Properties spearProperties(Tier tier, int attackDamage, float attackSpeed) {
        return new Item.Properties()
                .durability(tier.getUses())
                .attributes(SpearItem.createAttributes(tier, attackDamage, attackSpeed));
    }

}
