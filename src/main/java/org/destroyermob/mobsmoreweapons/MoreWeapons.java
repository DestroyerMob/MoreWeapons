package org.destroyermob.mobsmoreweapons;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.destroyermob.mobsmoreweapons.config.MoreWeaponsConfig;
import org.destroyermob.mobsmoreweapons.combat.IaiStanceSystem;
import org.destroyermob.mobsmoreweapons.entity.ModEntityTypes;
import org.destroyermob.mobsmoreweapons.item.ModItems;
import org.destroyermob.mobsmoreweapons.item.SpearItem;
import org.destroyermob.mobsmoreweapons.network.ModNetworking;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MoreWeapons.MOD_ID)
public class MoreWeapons {

    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "mobsmoreweapons";

    public MoreWeapons(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, MoreWeaponsConfig.SPEC);
        ModItems.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        modEventBus.addListener(ModNetworking::registerPayloads);

        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(IaiStanceSystem::tickPlayer);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, IaiStanceSystem::prepareAttack);
        NeoForge.EVENT_BUS.addListener(IaiStanceSystem::applyDamage);
        NeoForge.EVENT_BUS.addListener(IaiStanceSystem::finishAttack);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
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
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        ItemStack weapon = event.getEntity().getMainHandItem();
        if (SpearItem.isSpear(weapon)
                && (event.getEntity().getAttackStrengthScale(0.5F) < 1.0F
                || !SpearItem.isValidJabTarget(event.getEntity(), event.getTarget()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCriticalHit(CriticalHitEvent event) {
        if (SpearItem.isSpear(event.getEntity().getMainHandItem())) {
            event.setCriticalHit(false);
            event.setDamageMultiplier(1.0F);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

}
