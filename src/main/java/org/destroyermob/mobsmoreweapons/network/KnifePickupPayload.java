package org.destroyermob.mobsmoreweapons.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import org.destroyermob.mobsmoreweapons.MoreWeapons;

public record KnifePickupPayload(boolean offHand) implements CustomPacketPayload {
    public static final Type<KnifePickupPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreWeapons.MOD_ID, "knife_pickup")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, KnifePickupPayload> STREAM_CODEC = StreamCodec.of(
            KnifePickupPayload::encode,
            KnifePickupPayload::decode
    );

    public KnifePickupPayload(InteractionHand hand) {
        this(hand == InteractionHand.OFF_HAND);
    }

    public InteractionHand hand() {
        return offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, KnifePickupPayload payload) {
        buffer.writeBoolean(payload.offHand);
    }

    private static KnifePickupPayload decode(RegistryFriendlyByteBuf buffer) {
        return new KnifePickupPayload(buffer.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
