package org.destroyermob.mobsmoreweapons.client;

import java.util.List;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.destroyermob.mobsmoreweapons.MoreWeapons;

@EventBusSubscriber(modid = MoreWeapons.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MoreWeaponsClientModels {
    private static final List<String> SPEARS = List.of(
            "wooden_spear", "stone_spear", "copper_spear", "iron_spear",
            "golden_spear", "diamond_spear", "netherite_spear"
    );

    private MoreWeaponsClientModels() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        SPEARS.forEach(name -> event.register(handModel(name)));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void modifyModels(ModelEvent.ModifyBakingResult event) {
        for (String name : SPEARS) {
            ModelResourceLocation compactLocation = ModelResourceLocation.inventory(id(name));
            BakedModel compact = event.getModels().get(compactLocation);
            BakedModel inHand = event.getModels().get(handModel(name));
            if (compact != null && inHand != null) {
                event.getModels().put(compactLocation, new SpearDisplayContextModel(compact, inHand));
            }
        }
    }

    private static ModelResourceLocation handModel(String name) {
        return ModelResourceLocation.standalone(id("item/" + name + "_in_hand"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MoreWeapons.MOD_ID, path);
    }
}
