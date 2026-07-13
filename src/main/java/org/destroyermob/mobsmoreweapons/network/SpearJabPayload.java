package org.destroyermob.mobsmoreweapons.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.destroyermob.mobsmoreweapons.MoreWeapons;

/** Backports the dedicated vanilla spear stab action added after 1.21.1. */
public record SpearJabPayload() implements CustomPacketPayload {
    public static final SpearJabPayload INSTANCE = new SpearJabPayload();
    public static final Type<SpearJabPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreWeapons.MOD_ID, "spear_jab")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SpearJabPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
