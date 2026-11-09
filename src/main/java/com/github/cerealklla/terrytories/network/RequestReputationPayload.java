package com.github.cerealklla.terrytories.network;

import io.netty.buffer.ByteBuf;

import com.github.cerealklla.terrytories.TerrytoriesMod;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server: sent once per client session when the Reputation tab is first shown, asking
 * for the player's own current reputation standings ({@link ReputationPayload}). Empty -- the
 * player is already known server-side from the packet's own connection, same shape as Lyfe's
 * {@code socket.RequestRunewordRollsPayload}.
 */
public record RequestReputationPayload() implements CustomPacketPayload {

    public static final Type<RequestReputationPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TerrytoriesMod.MODID, "request_reputation"));

    public static final StreamCodec<ByteBuf, RequestReputationPayload> STREAM_CODEC =
            StreamCodec.unit(new RequestReputationPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
