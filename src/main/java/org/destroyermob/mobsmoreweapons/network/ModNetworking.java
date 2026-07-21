package org.destroyermob.mobsmoreweapons.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.client.IaiIndicator;
import org.destroyermob.mobsmoreweapons.client.GreatSwordAnimationCompatibility;
import org.destroyermob.mobsmoreweapons.client.KnifePickupAnimationCompatibility;
import org.destroyermob.mobsmoreweapons.item.SpearItem;

public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "4";

    private ModNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(IaiStatePayload.TYPE, IaiStatePayload.STREAM_CODEC, ModNetworking::handleIaiState);
        registrar.playToClient(KnifePickupPayload.TYPE, KnifePickupPayload.STREAM_CODEC, ModNetworking::handleKnifePickup);
        registrar.playToClient(GreatSwordSweepAnimationPayload.TYPE, GreatSwordSweepAnimationPayload.STREAM_CODEC, ModNetworking::handleGreatSwordSweepAnimation);
        registrar.playToServer(SpearJabPayload.TYPE, SpearJabPayload.STREAM_CODEC, ModNetworking::handleSpearJab);
        registrar.playToServer(
                BetterCombatSpearLungePayload.TYPE,
                BetterCombatSpearLungePayload.STREAM_CODEC,
                ModNetworking::handleBetterCombatSpearLunge
        );
    }

    public static void sendSpearJab() {
        PacketDistributor.sendToServer(SpearJabPayload.INSTANCE);
    }

    public static void sendBetterCombatSpearLunge() {
        PacketDistributor.sendToServer(BetterCombatSpearLungePayload.INSTANCE);
    }

    private static void handleIaiState(IaiStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> IaiIndicator.acceptState(payload));
    }

    private static void handleKnifePickup(KnifePickupPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> KnifePickupAnimationCompatibility.accept(payload));
    }

    private static void handleGreatSwordSweepAnimation(GreatSwordSweepAnimationPayload payload, IPayloadContext context) {
        context.enqueueWork(GreatSwordAnimationCompatibility::triggerSweep);
    }

    private static void handleSpearJab(SpearJabPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MoreWeapons.LOGGER.debug("Received spear jab payload for {}", player.getScoreboardName());
                SpearItem.jab(player);
            }
        });
    }

    private static void handleBetterCombatSpearLunge(
            BetterCombatSpearLungePayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (ModList.get().isLoaded("bettercombat")
                    && context.player() instanceof ServerPlayer player) {
                SpearItem.tryApplyBetterCombatLunge(player);
            }
        });
    }
}
