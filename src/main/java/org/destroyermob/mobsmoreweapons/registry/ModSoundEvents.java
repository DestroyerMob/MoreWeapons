package org.destroyermob.mobsmoreweapons.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.destroyermob.mobsmoreweapons.MoreWeapons;

public final class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(
            Registries.SOUND_EVENT,
            MoreWeapons.MOD_ID
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMOR_EQUIP_COPPER = SOUND_EVENTS.register(
            "item.armor.equip_copper",
            SoundEvent::createVariableRangeEvent
    );

    private ModSoundEvents() {
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
