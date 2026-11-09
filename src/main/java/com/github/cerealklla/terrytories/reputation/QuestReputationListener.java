package com.github.cerealklla.terrytories.reputation;

import java.util.UUID;

import com.github.cerealklla.myssions.api.Myssions;
import com.github.cerealklla.myssions.quest.QuestEndHooks;
import com.github.cerealklla.settlemynts.api.Settlemynts;
import com.github.cerealklla.terrytories.api.Terrytories;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * "Doing quests for settlements grant Settlement rep, and a small amount of rep for the Kingdom
 * which controls the area" -- explicit user spec, both halves. Registers against Myssions' global
 * {@code QuestEndHooks} (added the same day specifically for this and Arryval's own "do 10 quests
 * for your village" quest) since this mod can't enumerate every quest id that will ever exist
 * across every mod. Resolves the completed instance's issuing plot ({@code
 * api.Myssions#getIssuingPlotId}) to its owning settlement ({@code
 * api.Settlemynts#findSettlementCoreIdForPlot}) -- a quest with no issuing plot (an off-site/
 * wilderness quest with no real settlement tie) grants nothing.
 *
 * <p>The two grants are deliberately independent, per the user's own explicit correction,
 * 2026-11-08 -- a player's reputation with the settlement itself never depends on who currently
 * controls it ("the denizens of the town would still remember a player who did 20 quests to help
 * them"); the controlling Faction (see {@code faction.SettlementControlStore}) only decides who
 * gets the smaller second grant, and only if the settlement is actually controlled by anyone.
 */
public final class QuestReputationListener {

    /** Placeholder/tunable -- 10 quests at this rate exactly crosses {@link Standing#FRIENDLY_THRESHOLD}, matching the user's own stated math ("10 quests... enough to become Friendly"). */
    public static final int REPUTATION_PER_QUEST = 5;

    /** Placeholder/tunable -- the Kingdom's "small amount," 20% of the settlement's own grant. */
    public static final int CONTROLLING_FACTION_REPUTATION_PER_QUEST = 1;

    public QuestReputationListener() {
        QuestEndHooks.registerGlobal(this::onQuestEnded);
    }

    private void onQuestEnded(ServerPlayer player, UUID instanceId, Identifier questId, QuestEndHooks.EndReason reason) {
        if (reason != QuestEndHooks.EndReason.COMPLETED || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Myssions.getIssuingPlotId(instanceId)
                .flatMap(plotId -> Settlemynts.findSettlementCoreIdForPlot(level, plotId))
                .ifPresent(settlementCoreId -> {
                    Terrytories.adjustReputation(player, settlementCoreId, REPUTATION_PER_QUEST);
                    Terrytories.getSettlementController(settlementCoreId)
                            .filter(controllerId -> !controllerId.equals(settlementCoreId))
                            .ifPresent(controllerId -> Terrytories.adjustReputation(player, controllerId, CONTROLLING_FACTION_REPUTATION_PER_QUEST));
                });
    }
}
