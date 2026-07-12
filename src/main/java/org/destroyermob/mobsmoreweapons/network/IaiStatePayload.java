package org.destroyermob.mobsmoreweapons.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.destroyermob.mobsmoreweapons.MoreWeapons;

public record IaiStatePayload(
        boolean active,
        boolean charging,
        boolean primed,
        int chargeTicks,
        int requiredTicks,
        boolean ready
) implements CustomPacketPayload {
    public static final Type<IaiStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreWeapons.MOD_ID, "iai_state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, IaiStatePayload> STREAM_CODEC = StreamCodec.of(
            IaiStatePayload::encode,
            IaiStatePayload::decode
    );

    private static void encode(RegistryFriendlyByteBuf buffer, IaiStatePayload payload) {
        buffer.writeBoolean(payload.active);
        buffer.writeBoolean(payload.charging);
        buffer.writeBoolean(payload.primed);
        buffer.writeVarInt(payload.chargeTicks);
        buffer.writeVarInt(payload.requiredTicks);
        buffer.writeBoolean(payload.ready);
    }

    private static IaiStatePayload decode(RegistryFriendlyByteBuf buffer) {
        return new IaiStatePayload(
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
