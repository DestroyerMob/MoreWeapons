package org.destroyermob.mobsmoreweapons.item;

import net.minecraft.world.entity.Entity;

/** State attached to every living entity by the vanilla spear backport hooks. */
public interface SpearUser {
    float mobsmoreweapons$timeSinceLastSpearImpact(float partialTick);

    /**
     * Keeps an already-active contact latched while the spear still overlaps
     * the target. Missing or expired contacts are never created here.
     */
    boolean mobsmoreweapons$maintainSpearContactCooldown(Entity target, int cooldownTicks);

    /** Starts a contact latch after the spear actually applies an effect. */
    void mobsmoreweapons$startSpearContactCooldown(Entity target, int cooldownTicks);

    int mobsmoreweapons$countSpearContacts();
}
