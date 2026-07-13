package org.destroyermob.mobsmoreweapons.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.destroyermob.mobsmoreweapons.client.IaiIndicator;
import org.destroyermob.mobsmoreweapons.client.GreatSwordAnimationCompatibility;
import org.destroyermob.mobsmoreweapons.client.KnifePickupAnimationCompatibility;

public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "2";

    private ModNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(IaiStatePayload.TYPE, IaiStatePayload.STREAM_CODEC, ModNetworking::handleIaiState);
        registrar.playToClient(KnifePickupPayload.TYPE, KnifePickupPayload.STREAM_CODEC, ModNetworking::handleKnifePickup);
        registrar.playToClient(GreatSwordSweepAnimationPayload.TYPE, GreatSwordSweepAnimationPayload.STREAM_CODEC, ModNetworking::handleGreatSwordSweepAnimation);
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
}
