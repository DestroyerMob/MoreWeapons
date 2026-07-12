package org.destroyermob.mobsmoreweapons.entity;

import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.destroyermob.mobsmoreweapons.MoreWeapons;

public final class ModEntityTypes {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MoreWeapons.MOD_ID);

    public static final Supplier<EntityType<ThrownKnife>> THROWN_KNIFE = ENTITY_TYPES.register(
            "thrown_knife",
            () -> EntityType.Builder.<ThrownKnife>of(ThrownKnife::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(MoreWeapons.MOD_ID + ":thrown_knife")
    );

    private ModEntityTypes() {
    }

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}
