package com.github.cerealklla.terrytories.faction;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;

/** One Settlement's current controlling Faction -- see {@link SettlementControlStore}'s own doc. */
public record SettlementControl(UUID settlementCoreId, UUID controllingFactionId) {

    public static final Codec<SettlementControl> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("settlement_core_id").forGetter(SettlementControl::settlementCoreId),
            UUIDUtil.CODEC.fieldOf("controlling_faction_id").forGetter(SettlementControl::controllingFactionId)
    ).apply(i, SettlementControl::new));
}
