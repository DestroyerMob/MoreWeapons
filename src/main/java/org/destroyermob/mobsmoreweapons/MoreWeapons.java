package org.destroyermob.mobsmoreweapons;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.destroyermob.mobsmoreweapons.config.MoreWeaponsConfig;
import org.destroyermob.mobsmoreweapons.combat.BattleAxeHookSystem;
import org.destroyermob.mobsmoreweapons.combat.IaiStanceSystem;
import org.destroyermob.mobsmoreweapons.combat.GreatSwordSweepSystem;
import org.destroyermob.mobsmoreweapons.entity.ModEntityTypes;
import org.destroyermob.mobsmoreweapons.item.ModItems;
import org.destroyermob.mobsmoreweapons.item.BattleAxeItem;
import org.destroyermob.mobsmoreweapons.item.GreatSwordItem;
import org.destroyermob.mobsmoreweapons.item.tier.ModArmorMaterials;
import org.destroyermob.mobsmoreweapons.network.ModNetworking;
import org.destroyermob.mobsmoreweapons.registry.ModSoundEvents;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MoreWeapons.MOD_ID)
public class MoreWeapons {

    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "mobsmoreweapons";

    public MoreWeapons(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, MoreWeaponsConfig.SPEC);
        ModSoundEvents.register(modEventBus);
        ModArmorMaterials.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        modEventBus.addListener(ModNetworking::registerPayloads);

        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(IaiStanceSystem::tickPlayer);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, IaiStanceSystem::prepareAttack);
        NeoForge.EVENT_BUS.addListener(IaiStanceSystem::applyDamage);
        NeoForge.EVENT_BUS.addListener(IaiStanceSystem::finishAttack);
        NeoForge.EVENT_BUS.addListener(GreatSwordSweepSystem::tickPlayer);
        NeoForge.EVENT_BUS.addListener(BattleAxeHookSystem::tickPlayer);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.COPPER_SWORD);
            event.accept(ModItems.COPPER_AXE);
            event.accept(ModItems.COPPER_HELMET);
            event.accept(ModItems.COPPER_CHESTPLATE);
            event.accept(ModItems.COPPER_LEGGINGS);
            event.accept(ModItems.COPPER_BOOTS);

            // Greatswords
            event.accept(ModItems.WOODENGREATSWORD);
            event.accept(ModItems.STONEGREATSWORD);
            event.accept(ModItems.IRONGREATSWORD);
            event.accept(ModItems.GOLDGREATSWORD);
            event.accept(ModItems.DIAMONDGREATSWORD);
            event.accept(ModItems.NETHERITEGREATSWORD);

            // Katanas
            event.accept(ModItems.WOODENKATANA);
            event.accept(ModItems.STONEKATANA);
            event.accept(ModItems.IRONKATANA);
            event.accept(ModItems.GOLDKATANA);
            event.accept(ModItems.DIAMONDKATANA);
            event.accept(ModItems.NETHERITEKATANA);

            // Battle Axes
            event.accept(ModItems.WOODENBATTLEAXE);
            event.accept(ModItems.STONEBATTLEAXE);
            event.accept(ModItems.IRONBATTLEAXE);
            event.accept(ModItems.GOLDBATTLEAXE);
            event.accept(ModItems.DIAMONDBATTLEAXE);
            event.accept(ModItems.NETHERITEBATTLEAXE);

            // Knives
            event.accept(ModItems.WOODENKNIFE);
            event.accept(ModItems.STONEKNIFE);
            event.accept(ModItems.IRONKNIFE);
            event.accept(ModItems.GOLDENKNIFE);
            event.accept(ModItems.DIAMONDKNIFE);
            event.accept(ModItems.NETHERITEKNIFE);

            // Machetes
            event.accept(ModItems.WOODENMACHETE);
            event.accept(ModItems.STONEMACHETE);
            event.accept(ModItems.IRONMACHETE);
            event.accept(ModItems.GOLDENMACHETE);
            event.accept(ModItems.DIAMONDMACHETE);
            event.accept(ModItems.NETHERITEMACHETE);

            // Spears
            event.accept(ModItems.WOODENSPEAR);
            event.accept(ModItems.STONESPEAR);
            event.accept(ModItems.COPPERSPEAR);
            event.accept(ModItems.IRONSPEAR);
            event.accept(ModItems.GOLDENSPEAR);
            event.accept(ModItems.DIAMONDSPEAR);
            event.accept(ModItems.NETHERITESPEAR);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.COPPER_SHOVEL);
            event.accept(ModItems.COPPER_PICKAXE);
            event.accept(ModItems.COPPER_AXE);
            event.accept(ModItems.COPPER_HOE);
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (GreatSwordSweepSystem.blocksAttack(event.getEntity())
                || BattleAxeHookSystem.blocksAttack(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void addWeaponAttributes(ItemAttributeModifierEvent event) {
        if (event.getItemStack().getItem() instanceof GreatSwordItem) {
            event.addModifier(
                    Attributes.SWEEPING_DAMAGE_RATIO,
                    new AttributeModifier(
                            GreatSwordItem.SWEEP_DAMAGE_MODIFIER_ID,
                            0.25D,
                            AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
            );
            event.addModifier(
                    Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(
                            GreatSwordItem.REACH_MODIFIER_ID,
                            1.0D,
                            AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
            );
        }
        if (event.getItemStack().getItem() instanceof BattleAxeItem) {
            event.addModifier(
                    Attributes.ATTACK_KNOCKBACK,
                    new AttributeModifier(
                            BattleAxeItem.KNOCKBACK_MODIFIER_ID,
                            0.25D,
                            AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
            );
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

}
