package org.destroyermob.mobsmoreweapons.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.destroyermob.mobsmoreweapons.MoreWeapons;

public record GreatSwordSweepAnimationPayload() implements CustomPacketPayload {
    public static final GreatSwordSweepAnimationPayload INSTANCE = new GreatSwordSweepAnimationPayload();
    public static final Type<GreatSwordSweepAnimationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreWeapons.MOD_ID, "great_sword_sweep_animation")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, GreatSwordSweepAnimationPayload> STREAM_CODEC =
            StreamCodec.ofMember(GreatSwordSweepAnimationPayload::encode, GreatSwordSweepAnimationPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
    }

    private static GreatSwordSweepAnimationPayload decode(RegistryFriendlyByteBuf buffer) {
        return INSTANCE;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
