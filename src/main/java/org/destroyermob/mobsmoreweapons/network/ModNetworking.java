package org.destroyermob.mobsmoreweapons.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.destroyermob.mobsmoreweapons.client.IaiIndicator;
import org.destroyermob.mobsmoreweapons.client.GreatSwordAnimationCompatibility;
import org.destroyermob.mobsmoreweapons.client.KnifePickupAnimationCompatibility;
import org.destroyermob.mobsmoreweapons.item.SpearItem;

public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "3";

    private ModNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(IaiStatePayload.TYPE, IaiStatePayload.STREAM_CODEC, ModNetworking::handleIaiState);
        registrar.playToClient(KnifePickupPayload.TYPE, KnifePickupPayload.STREAM_CODEC, ModNetworking::handleKnifePickup);
        registrar.playToClient(GreatSwordSweepAnimationPayload.TYPE, GreatSwordSweepAnimationPayload.STREAM_CODEC, ModNetworking::handleGreatSwordSweepAnimation);
        registrar.playToServer(SpearJabPayload.TYPE, SpearJabPayload.STREAM_CODEC, ModNetworking::handleSpearJab);
    }

    public static void sendSpearJab() {
        PacketDistributor.sendToServer(SpearJabPayload.INSTANCE);
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
                SpearItem.jab(player);
            }
        });
    }
}
