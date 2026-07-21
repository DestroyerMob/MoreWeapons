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
    public static final DeferredHolder<SoundEvent, SoundEvent> SPEAR_ATTACK = variable("item.spear.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPEAR_HIT = variable("item.spear.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPEAR_USE = variable("item.spear.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPEAR_WOOD_ATTACK = variable("item.spear_wood.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPEAR_WOOD_HIT = variable("item.spear_wood.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPEAR_WOOD_USE = variable("item.spear_wood.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPEAR_LUNGE = variable("item.spear.lunge");

    private ModSoundEvents() {
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> variable(String name) {
        return SOUND_EVENTS.register(name, SoundEvent::createVariableRangeEvent);
    }
}
