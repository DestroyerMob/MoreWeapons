package org.destroyermob.mobsmoreweapons.combat;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.destroyermob.mobsmoreweapons.config.MoreWeaponsConfig;
import org.destroyermob.mobsmoreweapons.item.KatanaItem;
import org.destroyermob.mobsmoreweapons.network.IaiStatePayload;

/** Server-authoritative state for every katana's base Iaijutsu stance. */
public final class IaiStanceSystem {
    private static final Map<Player, IaiState> STATES = new WeakHashMap<>();

    private IaiStanceSystem() {
    }

    public static void beginStance(Player player, ItemStack weapon) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(weapon.getItem() instanceof KatanaItem)) {
            return;
        }
        serverPlayer.setSprinting(false);
        IaiState existing = STATES.get(player);
        if (existing != null
                && existing.using
                && existing.selectedSlot == player.getInventory().selected) {
            sendState(serverPlayer, existing, true);
            return;
        }
        IaiState state = new IaiState(player.getInventory().selected);
        state.using = true;
        state.charging = true;
        STATES.put(player, state);
        sendState(serverPlayer, state, true);
    }

    public static void releaseStance(Player player, ItemStack weapon) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        IaiState state = STATES.get(player);
        if (state == null
                || !state.using
                || state.selectedSlot != player.getInventory().selected
                || !(weapon.getItem() instanceof KatanaItem)) {
            return;
        }
        state.using = false;
        state.charging = false;
        if (state.ready) {
            state.primedTicks = MoreWeaponsConfig.IAI_PRIMED_TICKS.get();
            sendState(serverPlayer, state, true);
        } else {
            sendInactive(serverPlayer);
            STATES.remove(player);
        }
    }

    public static void tickPlayer(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        IaiState state = STATES.get(player);
        if (state == null) {
            return;
        }

        if (state.strikeTicks > 0 && --state.strikeTicks == 0) {
            state.strikeTargetId = -1;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (player.getInventory().selected != state.selectedSlot
                || !(mainHand.getItem() instanceof KatanaItem)) {
            sendInactive(player);
            STATES.remove(player);
            return;
        }
        boolean holdingStance = player.isUsingItem()
                && player.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                && player.getUseItem().getItem() instanceof KatanaItem;
        if (holdingStance) {
            if (player.isSprinting()) {
                player.setSprinting(false);
            }
            state.using = true;
            state.primedTicks = 0;
            updateCharge(player, state);
            return;
        }

        if (state.using) {
            releaseStance(player, mainHand);
            state = STATES.get(player);
            if (state == null) {
                return;
            }
        }
        if (state.primedTicks > 0 && --state.primedTicks == 0) {
            state.ready = false;
            state.chargeTicks = 0;
            sendInactive(player);
        }
        if (!state.using && state.primedTicks <= 0 && state.strikeTicks <= 0) {
            STATES.remove(player);
        }
    }

    private static void updateCharge(ServerPlayer player, IaiState state) {
        int requiredTicks = MoreWeaponsConfig.IAI_CHARGE_TICKS.get();
        if (!state.charging) {
            state.charging = true;
            sendState(player, state, true);
        }
        if (!state.ready && state.chargeTicks < requiredTicks) {
            state.chargeTicks++;
        }
        if (!state.ready
                && state.chargeTicks >= requiredTicks
                && player.getAttackStrengthScale(0.0F) >= 1.0F) {
            state.ready = true;
            sendState(player, state, true);
        }
    }

    public static void prepareAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        IaiState state = STATES.get(player);
        if (state == null || state.primedTicks <= 0 || !state.ready
                || player.getInventory().selected != state.selectedSlot
                || !(player.getMainHandItem().getItem() instanceof KatanaItem)) {
            return;
        }

        applyLungeImpulse(player, event.getTarget());
        state.primedTicks = 0;
        state.ready = false;
        state.chargeTicks = 0;
        state.strikeTargetId = event.getTarget().getId();
        state.strikeTicks = 4;
        state.damageApplied = false;
        sendInactive(player);
    }

    /** Gives the base quickdraw a payoff independently of any enchantment. */
    public static void applyDamage(LivingDamageEvent.Pre event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player) || event.getNewDamage() <= 0.0F) {
            return;
        }
        ItemStack sourceWeapon = event.getSource().getWeaponItem();
        IaiState state = STATES.get(player);
        if (state == null || state.damageApplied || state.strikeTicks <= 0
                || state.strikeTargetId != event.getEntity().getId()
                || sourceWeapon == null
                || !(sourceWeapon.getItem() instanceof KatanaItem)) {
            return;
        }
        event.setNewDamage(event.getNewDamage() * MoreWeaponsConfig.IAI_DAMAGE_MULTIPLIER.get().floatValue());
        state.damageApplied = true;
    }

    /** Makes a successful quickdraw visually and audibly distinct from a normal hit. */
    public static void finishAttack(LivingDamageEvent.Post event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player)) {
            return;
        }
        IaiState state = STATES.get(player);
        if (state == null || !state.damageApplied || state.strikeTargetId != event.getEntity().getId()) {
            return;
        }
        if (event.getNewDamage() > 0.0F) {
            player.serverLevel().sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    event.getEntity().getX(),
                    event.getEntity().getY(0.55D),
                    event.getEntity().getZ(),
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
            player.serverLevel().playSound(
                    null,
                    event.getEntity().blockPosition(),
                    SoundEvents.PLAYER_ATTACK_CRIT,
                    SoundSource.PLAYERS,
                    0.9F,
                    1.3F
            );
        }
        state.strikeTicks = 0;
        state.strikeTargetId = -1;
    }

    /** Stable optional-integration signal used by enchantments that enhance an Iaijutsu strike. */
    public static boolean isFullyChargedStrike(Player player, Entity target) {
        IaiState state = STATES.get(player);
        return state != null && state.strikeTicks > 0 && state.strikeTargetId == target.getId();
    }

    private static void applyLungeImpulse(ServerPlayer player, Entity target) {
        Vec3 offset = target.position().subtract(player.position());
        Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
        double horizontalDistance = horizontal.length();
        if (horizontalDistance < 1.0E-4D) {
            return;
        }
        double force = MoreWeaponsConfig.IAI_LUNGE_FORCE.get();
        if (force > 0.0D) {
            Vec3 movement = player.getDeltaMovement();
            Vec3 impulse = horizontal.scale(force / horizontalDistance);
            player.setDeltaMovement(movement.x + impulse.x, movement.y, movement.z + impulse.z);
            player.hasImpulse = true;
            player.hurtMarked = true;
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    private static void sendState(ServerPlayer player, IaiState state, boolean active) {
        PacketDistributor.sendToPlayer(player, new IaiStatePayload(
                active,
                state.charging,
                state.primedTicks > 0,
                state.chargeTicks,
                MoreWeaponsConfig.IAI_CHARGE_TICKS.get(),
                state.ready
        ));
    }

    private static void sendInactive(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new IaiStatePayload(
                false,
                false,
                false,
                0,
                MoreWeaponsConfig.IAI_CHARGE_TICKS.get(),
                false
        ));
    }

    private static final class IaiState {
        private final int selectedSlot;
        private boolean using;
        private boolean charging;
        private boolean ready;
        private int chargeTicks;
        private int primedTicks;
        private int strikeTargetId = -1;
        private int strikeTicks;
        private boolean damageApplied;

        private IaiState(int selectedSlot) {
            this.selectedSlot = selectedSlot;
        }
    }
}
