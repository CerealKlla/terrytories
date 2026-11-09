package com.github.cerealklla.terrytories.reputation;

import com.github.cerealklla.settlemynts.api.Settlemynts;
import com.github.cerealklla.terrytories.api.Terrytories;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Installs Terrytories' hostile-standing predicate into Settlemynts' {@code
 * guardhouse.GuardHostilityExtension} (via {@code api.Settlemynts#registerGuardHostilityExtension})
 * -- "when rep is sufficiently low... NPCs for that settlement will target you aggressively," Guards
 * specifically. Settlemynts' callback only ever hands over raw ids (it never depends on this mod),
 * so {@link ServerLifecycleHooks#getCurrentServer()} (this suite's established idiom for "the live
 * server, from anywhere," see Lyfe's own {@code summoning.DevSummoningStoneContribution}) is how the
 * bare {@code playerId} gets resolved back to a real {@link ServerPlayer}.
 *
 * <p>Deliberately Guards only -- worker NPCs and tagged natural Villagers have no real combat
 * capability at all (plain peaceful {@code Villager} subclasses with no attack goal), so giving them
 * a targeting goal would accomplish nothing visible; see this mod's own design-document.md.
 */
public final class GuardHostilityBridge {

    private GuardHostilityBridge() {
    }

    public static void install() {
        Settlemynts.registerGuardHostilityExtension((settlementCoreId, playerId) -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return false;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            return player != null && Terrytories.getStanding(player, settlementCoreId) == Standing.HOSTILE;
        });
    }
}
