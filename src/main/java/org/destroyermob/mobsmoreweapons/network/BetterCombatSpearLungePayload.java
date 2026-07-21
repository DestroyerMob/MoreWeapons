package org.destroyermob.mobsmoreweapons.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.destroyermob.mobsmoreweapons.MoreWeapons;

/** Client claim that a Better Combat spear upswing has just begun. */
public record BetterCombatSpearLungePayload() implements CustomPacketPayload {
    public static final BetterCombatSpearLungePayload INSTANCE = new BetterCombatSpearLungePayload();
    public static final Type<BetterCombatSpearLungePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreWeapons.MOD_ID, "better_combat_spear_lunge")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BetterCombatSpearLungePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
