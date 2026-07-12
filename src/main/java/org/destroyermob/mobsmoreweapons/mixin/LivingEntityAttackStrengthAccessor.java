package org.destroyermob.mobsmoreweapons.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAttackStrengthAccessor {
    @Accessor("attackStrengthTicker")
    void mobsmoreweapons$setAttackStrengthTicker(int ticks);
}
