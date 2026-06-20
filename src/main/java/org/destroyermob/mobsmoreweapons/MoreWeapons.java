package org.destroyermob.mobsmoreweapons;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.destroyermob.mobsmoreweapons.item.ModItems;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MoreWeapons.MOD_ID)
public class MoreWeapons {

    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "mobsmoreweapons";

    public MoreWeapons(IEventBus modEventBus) {
        ModItems.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);

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
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

}
