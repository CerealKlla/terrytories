package com.github.cerealklla.terrytories.kingdom;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;

/**
 * Persisted marker recording that the world-spawn Kingdom has already been generated -- its real
 * Cartographyr territory {@code EntityId} (so a server restart never creates a second one) and its
 * minted {@code faction.FactionRecord} id (so later code, e.g. a future patrol/garrison feature,
 * can find "the Kingdom's own faction" without re-deriving it). Only ever one entry (index 0)
 * today; the index field exists purely so this can reuse {@code
 * nuclyus.server.PersistedServerStore}'s generic shape (same "generate once, persist a tiny JSON
 * file" precedent every other one-shot server-wide generation in this suite uses) rather than a
 * bespoke single-value file format.
 */
public record KingdomEntry(int index, long territoryEntityIdValue, UUID factionId) {

    public static final Codec<KingdomEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("index").forGetter(KingdomEntry::index),
            Codec.LONG.fieldOf("territory_entity_id").forGetter(KingdomEntry::territoryEntityIdValue),
            UUIDUtil.CODEC.fieldOf("faction_id").forGetter(KingdomEntry::factionId)
    ).apply(i, KingdomEntry::new));
}
