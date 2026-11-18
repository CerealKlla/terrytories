package com.github.cerealklla.terrytories.reputation;

import com.github.cerealklla.settlemynts.api.Settlemynts;
import com.github.cerealklla.terrytories.api.Terrytories;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Installs Terrytories' Friendly-standing predicate into Settlemynts' {@code
 * guardhouse.GuardAllyExtension} (via {@code api.Settlemynts#registerGuardAllyExtension}) -- mirrors
 * {@link GuardHostilityBridge} exactly, the other direction. Without this, a Friendly player who
 * grazes their own town's Guard (a sweep attack, a stray arrow, a zombie fight that drifts too
 * close) has the damage correctly zeroed by {@code CombatReputationListener}, but the Guard's own
 * {@code HurtByTargetGoal} still latches onto them as a target regardless -- real report, 2026-11-18:
 * "my own guard isn't hurting me but he's still wasting time attacking me instead of patrolling."
 */
public final class GuardFriendlinessBridge {

    private GuardFriendlinessBridge() {
    }

    public static void install() {
        Settlemynts.registerGuardAllyExtension((settlementCoreId, playerId) -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return false;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            return player != null && Terrytories.getStanding(player, settlementCoreId) == Standing.FRIENDLY;
        });
    }
}
