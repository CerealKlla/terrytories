package com.github.cerealklla.terrytories.reputation;

import java.util.Set;
import java.util.UUID;

import com.github.cerealklla.settlemynts.api.Settlemynts;
import com.github.cerealklla.terrytories.api.Terrytories;
import com.github.cerealklla.terrytories.bridge.LyfeAssistBridge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * "Town defense" -- explicit user spec, 2026-11-08: a small reputation gain for helping kill
 * hostile mobs within a settlement. Every player credited toward the kill gets the full +1, not
 * split -- "helping kill" matches Myssions' own identical "every player who has an assist marker
 * on that mob will be given equal credit" phrasing for its kill-quest milestones, so this reuses
 * the exact same General assist-marker system (a new {@code bridge.LyfeAssistBridge} here, falling
 * back to just the killing blow if Lyfe isn't loaded at all -- same fallback shape {@code
 * quest.QuestKillListener} already uses).
 *
 * <p>"Within a settlement" is resolved via {@code api.Settlemynts#findPlotAt} at the mob's death
 * position first (any of the settlement's own finalized plots), falling back to {@code
 * #findSettlementCoreIdNear} (added 2026-11-17, real report: "I think I'm only gaining settlement
 * rep for defense if I'm standing in a plot, not within the settlement perimeter") for anywhere
 * else inside the settlement's own overall registered polygon -- open ground between buildings,
 * roads, the commons a Guard's own patrol route actually spends most of its time on. The original
 * plot-only scope was a deliberate choice at the time, but in practice a hostile mob is at least as
 * likely to die on a road or in open ground as inside a specific plot's own tighter polygon, so
 * restricting credit to plots alone silently missed most real defense. Still doesn't extend past
 * the settlement's own outer buffer -- a kill genuinely outside the settlement doesn't count.
 */
public final class TownDefenseReputationListener {

    /** Placeholder/tunable -- deliberately small, this is incidental defense, not a dedicated quest. No controlling-Kingdom trickle (see {@code QuestReputationListener}'s own doc) -- that's quest-specific, not applied here. */
    public static final int REPUTATION_PER_DEFENSE_KILL = 1;

    /** Zero margin -- strictly within the settlement's own registered polygon (which already includes its outer buffer), not beyond it. See this class's own doc for why a plot-only check isn't enough. */
    private static final double SETTLEMENT_PERIMETER_MARGIN_BLOCKS = 0.0;

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        LivingEntity deadMob = event.getEntity();
        if (!(deadMob instanceof Enemy) || !(deadMob.level() instanceof ServerLevel level)) {
            return;
        }
        UUID settlementCoreId = Settlemynts.findPlotAt(level, deadMob.blockPosition())
                .map(Settlemynts.PlotHandle::settlementCoreId)
                .or(() -> Settlemynts.findSettlementCoreIdNear(level, deadMob.blockPosition(), SETTLEMENT_PERIMETER_MARGIN_BLOCKS))
                .orElse(null);
        if (settlementCoreId == null) {
            return;
        }
        Set<UUID> creditedIds;
        if (LyfeAssistBridge.isLoaded()) {
            creditedIds = LyfeAssistBridge.getAssistCredit(deadMob);
        } else if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            creditedIds = Set.of(killer.getUUID());
        } else {
            return;
        }
        for (UUID playerId : creditedIds) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                Terrytories.adjustReputation(player, settlementCoreId, REPUTATION_PER_DEFENSE_KILL);
            }
        }
    }
}
