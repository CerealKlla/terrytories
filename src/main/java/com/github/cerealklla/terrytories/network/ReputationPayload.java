package com.github.cerealklla.terrytories.network;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;

import com.github.cerealklla.terrytories.TerrytoriesMod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-client: the requesting player's current reputation standings, name-resolved
 * server-side (the client never resolves a settlement/kingdom id itself). Only every group with a
 * genuinely nonzero reputation is included -- a never-interacted-with group has nothing to show,
 * same convention Lyfe's Hunter's Journal uses for "never engaged" mobs. {@code value} is the raw
 * ledger number; the client derives {@code reputation.Standing} from it itself via {@code
 * Standing#forValue} (a pure function, safe to duplicate client-side rather than also sending the
 * tier).
 *
 * <p>Deliberately generic ("group," not "settlement") even though only settlements populate this
 * today -- see this mod's own design-document.md on the still-unbuilt territory-free Faction
 * concept (e.g. a hostile-mob faction) this will need to widen to cover later. No code change
 * needed here when that lands, just more entries from a different source.
 */
public record ReputationPayload(List<Entry> entries) implements CustomPacketPayload {

    public record Entry(String groupName, int value) {
    }

    private static final StreamCodec<ByteBuf, Entry> ENTRY_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Entry::groupName,
            ByteBufCodecs.VAR_INT, Entry::value,
            Entry::new);

    public static final Type<ReputationPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TerrytoriesMod.MODID, "reputation"));

    public static final StreamCodec<ByteBuf, ReputationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ENTRY_CODEC), ReputationPayload::entries,
            ReputationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
