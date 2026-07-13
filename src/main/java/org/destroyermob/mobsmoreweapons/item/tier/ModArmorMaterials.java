package org.destroyermob.mobsmoreweapons.item.tier;

import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.registry.ModSoundEvents;

/** Armor materials backported from later vanilla releases. */
public final class ModArmorMaterials {
    private static final int COPPER_DURABILITY_MULTIPLIER = 11;
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(
            Registries.ARMOR_MATERIAL,
            MoreWeapons.MOD_ID
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> COPPER = ARMOR_MATERIALS.register(
            "copper",
            () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.BOOTS, 1,
                            ArmorItem.Type.LEGGINGS, 3,
                            ArmorItem.Type.CHESTPLATE, 4,
                            ArmorItem.Type.HELMET, 2,
                            ArmorItem.Type.BODY, 4
                    ),
                    8,
                    ModSoundEvents.ARMOR_EQUIP_COPPER,
                    () -> Ingredient.of(Items.COPPER_INGOT),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MoreWeapons.MOD_ID, "copper"))),
                    0.0F,
                    0.0F
            )
    );

    private ModArmorMaterials() {
    }

    public static int copperDurability(ArmorItem.Type type) {
        return type.getDurability(COPPER_DURABILITY_MULTIPLIER);
    }

    public static void register(IEventBus bus) {
        ARMOR_MATERIALS.register(bus);
    }
}
