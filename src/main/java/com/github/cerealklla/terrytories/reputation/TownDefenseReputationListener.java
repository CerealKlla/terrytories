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
 * position -- any of the settlement's own finalized plots, the same real territory check every
 * other "is this position part of settlement X" call in this suite already uses. A hostile mob
 * killed just outside every plot (open ground between buildings, the settlement's outer buffer)
 * doesn't count -- a deliberate, narrower scope than "anywhere near the settlement," consistent
 * with how plot-based territory is resolved everywhere else.
 */
public final class TownDefenseReputationListener {

    /** Placeholder/tunable -- deliberately small, this is incidental defense, not a dedicated quest. No controlling-Kingdom trickle (see {@code QuestReputationListener}'s own doc) -- that's quest-specific, not applied here. */
    public static final int REPUTATION_PER_DEFENSE_KILL = 1;

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        LivingEntity deadMob = event.getEntity();
        if (!(deadMob instanceof Enemy) || !(deadMob.level() instanceof ServerLevel level)) {
            return;
        }
        UUID settlementCoreId = Settlemynts.findPlotAt(level, deadMob.blockPosition())
                .map(Settlemynts.PlotHandle::settlementCoreId)
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
