package org.destroyermob.mobsmoreweapons;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.destroyermob.mobsmoreweapons.item.ModItems;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MoreWeapons.MOD_ID)
public class MoreWeapons {

    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "mobsmoreweapons";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public MoreWeapons() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
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
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }
    }
}
