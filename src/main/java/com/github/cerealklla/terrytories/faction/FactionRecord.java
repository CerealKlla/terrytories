package com.github.cerealklla.terrytories.faction;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;

/**
 * A minted Faction -- added 2026-11-08, explicit user correction of the earlier Phase D design:
 * "There should be a list of Factions, and separately a list of Settlements, and a data point in
 * the settlement is the Faction who controls the settlement currently." A Settlement already has
 * its own real identity (via Settlemynts/Cartographyr, resolved by {@code
 * api.Settlemynts#getAnySettlementName}) and doubles as its own implicit Faction for player-
 * reputation purposes -- its {@code settlementCoreId} IS its faction id, with no separate {@link
 * FactionRecord} needed. This store is only for Factions with no existing settlement identity to
 * borrow: a Kingdom (controls settlements but isn't one), or a future territory-free faction (e.g.
 * "mobs like the zombie pigmen," per the user's own example -- not built yet, see this mod's own
 * design-document.md).
 */
public record FactionRecord(UUID id, String name) {

    public static final Codec<FactionRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(FactionRecord::id),
            Codec.STRING.fieldOf("name").forGetter(FactionRecord::name)
    ).apply(i, FactionRecord::new));
}
