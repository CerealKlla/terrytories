package com.github.cerealklla.terrytories.reputation;

import io.netty.buffer.ByteBuf;

import com.github.cerealklla.terrytories.TerrytoriesMod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-client: a holder's reputation with some Faction just changed, carrying both the
 * before and after totals so the client can animate a fill (not just snap to the new value)
 * without needing to cache a previous total itself -- same shape as Lyfe's own {@code
 * xpbar.XpGainPayload}. Only ever sent when the holder resolves to a real connected player (see
 * {@code api.Terrytories#adjustReputation}'s own doc) -- a settlement/Kingdom holder has no screen
 * to show this on. {@code factionName} is resolved server-side (the client never resolves a
 * faction id itself).
 */
public record ReputationGainPayload(String factionName, int oldValue, int newValue) implements CustomPacketPayload {

    public static final Type<ReputationGainPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TerrytoriesMod.MODID, "reputation_gain"));

    public static final StreamCodec<ByteBuf, ReputationGainPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ReputationGainPayload::factionName,
            ByteBufCodecs.VAR_INT, ReputationGainPayload::oldValue,
            ByteBufCodecs.VAR_INT, ReputationGainPayload::newValue,
            ReputationGainPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
