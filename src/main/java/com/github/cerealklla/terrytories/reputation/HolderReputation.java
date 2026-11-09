package com.github.cerealklla.terrytories.reputation;

import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;

/**
 * One holder's reputation with every Faction it has a nonzero value for -- a "holder" is
 * deliberately NOT restricted to a player. Explicit user correction, 2026-11-08: "don't lock this
 * in as only players can gain/lose rep with factions... a kingdom may have rep with another
 * kingdom, which determines all kinds of things." A Settlement (its own {@code settlementCoreId})
 * or a minted {@code faction.FactionRecord} id (a Kingdom) can be a holder exactly the same way a
 * player's own UUID is -- nothing here cares which kind of id it got. (No settlement-held or
 * Kingdom-held reputation is actually granted by anything yet -- see this mod's own
 * design-document.md "Settlement-to-Kingdom standing" entry, still unsettled design -- this is
 * purely making sure the storage itself doesn't foreclose it.)
 *
 * <p>Values clamped to {@link #MIN}/{@link #MAX} on every write -- see {@code Standing} for the
 * tier thresholds derived from a given value.
 */
public record HolderReputation(UUID holderId, Map<UUID, Integer> reputation) {

    public static final int MIN = -100;
    public static final int MAX = 100;

    public static final Codec<HolderReputation> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("holder_id").forGetter(HolderReputation::holderId),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).fieldOf("reputation").forGetter(HolderReputation::reputation)
    ).apply(i, HolderReputation::new));

    public static HolderReputation empty(UUID holderId) {
        return new HolderReputation(holderId, Map.of());
    }

    public int get(UUID factionId) {
        return reputation.getOrDefault(factionId, 0);
    }

    /** Returns a new {@link HolderReputation} with {@code factionId}'s value adjusted by {@code delta} (clamped). */
    public HolderReputation adjust(UUID factionId, int delta) {
        int updated = Math.clamp(get(factionId) + delta, MIN, MAX);
        var next = new java.util.HashMap<>(reputation);
        next.put(factionId, updated);
        return new HolderReputation(holderId, Map.copyOf(next));
    }
}
