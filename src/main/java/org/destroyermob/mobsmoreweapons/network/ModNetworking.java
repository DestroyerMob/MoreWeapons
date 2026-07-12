package org.destroyermob.mobsmoreweapons.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.destroyermob.mobsmoreweapons.client.IaiIndicator;

public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";

    private ModNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(IaiStatePayload.TYPE, IaiStatePayload.STREAM_CODEC, ModNetworking::handleIaiState);
    }

    private static void handleIaiState(IaiStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> IaiIndicator.acceptState(payload));
    }
}
