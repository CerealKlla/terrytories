package com.github.cerealklla.terrytories.api;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.github.cerealklla.settlemynts.api.Settlemynts;
import com.github.cerealklla.terrytories.faction.FactionStore;
import com.github.cerealklla.terrytories.faction.SettlementControlStore;
import com.github.cerealklla.terrytories.reputation.ReputationGainPayload;
import com.github.cerealklla.terrytories.reputation.ReputationHooks;
import com.github.cerealklla.terrytories.reputation.ReputationStore;
import com.github.cerealklla.terrytories.reputation.Standing;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Public facade for Terrytories' Kingdoms/Reputation system -- other mods should only ever go
 * through this, never the internal {@code reputation}/{@code faction}/{@code kingdom} packages
 * directly, same convention every other mod in this suite follows for its own {@code api.*} class.
 *
 * <p>Reputation is "holder -> Faction," where a holder is an arbitrary id -- a player, a
 * settlement, or a Kingdom (itself a minted Faction) can all hold reputation with some other
 * Faction. Explicit user correction, 2026-11-08: "don't lock this in as only players can gain/lose
 * rep with factions... a kingdom may have rep with another kingdom." The {@code ServerPlayer}
 * overloads below are pure convenience (every real caller today happens to be reacting to a
 * player's own actions) -- they just forward {@code player.getUUID()} to the generic, holder-
 * agnostic methods; nothing about the underlying storage assumes the holder is a player.
 */
public final class Terrytories {

    private Terrytories() {
    }

    public static int getReputation(UUID holderId, UUID factionId) {
        return ReputationStore.get(holderId, factionId);
    }

    public static int getReputation(ServerPlayer holder, UUID factionId) {
        return getReputation(holder.getUUID(), factionId);
    }

    public static Standing getStanding(UUID holderId, UUID factionId) {
        return Standing.forValue(getReputation(holderId, factionId));
    }

    public static Standing getStanding(ServerPlayer holder, UUID factionId) {
        return getStanding(holder.getUUID(), factionId);
    }

    /** Every Faction {@code holderId} has a nonzero reputation value for. */
    public static Map<UUID, Integer> getAllReputation(UUID holderId) {
        return ReputationStore.all(holderId);
    }

    public static Map<UUID, Integer> getAllReputation(ServerPlayer holder) {
        return getAllReputation(holder.getUUID());
    }

    /**
     * Adjusts {@code holderId}'s reputation with {@code factionId} by {@code delta} (clamped, see
     * {@code reputation.HolderReputation}) and fires {@code reputation.ReputationHooks} if the
     * {@link Standing} tier actually changed. Does NOT itself apply any controlling-Kingdom trickle
     * share -- a caller adjusting a settlement's own reputation is responsible for separately
     * resolving {@link #getSettlementController} and calling this again for that faction too, if it
     * wants that (see {@code reputation.QuestReputationListener}, the real example).
     */
    public static Standing adjustReputation(UUID holderId, UUID factionId, int delta) {
        int oldValue = getReputation(holderId, factionId);
        Standing oldStanding = Standing.forValue(oldValue);
        int newValue = ReputationStore.adjust(holderId, factionId, delta);
        Standing newStanding = Standing.forValue(newValue);
        ReputationHooks.fire(holderId, factionId, oldStanding, newStanding);
        notifyGainIfPlayer(holderId, factionId, oldValue, newValue);
        return newStanding;
    }

    public static Standing adjustReputation(ServerPlayer holder, UUID factionId, int delta) {
        return adjustReputation(holder.getUUID(), factionId, delta);
    }

    /**
     * Triggers the transient reputation bar HUD (see {@code reputation.client
     * .ClientReputationBarState}) -- same "centralize it in the one real mutation method, not each
     * individual caller" shape Lyfe's own {@code api.Lyfe#addXp} already uses for its XP bar. Only
     * ever sent when {@code holderId} resolves to a real connected player -- a settlement/Kingdom
     * holder has no screen to show this on, and simply gets no notification at all.
     */
    private static void notifyGainIfPlayer(UUID holderId, UUID factionId, int oldValue, int newValue) {
        if (oldValue == newValue) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(holderId);
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        String name = resolveFactionName(level, factionId).orElse("Unknown Faction");
        PacketDistributor.sendToPlayer(player, new ReputationGainPayload(name, oldValue, newValue));
    }

    /** See {@code reputation.ReputationHooks}' own doc -- a generic "standing changed" hook other mods can subscribe to. {@code holderId} is NOT necessarily a player -- see that class's own doc. */
    public static void registerStandingChangedHook(ReputationHooks.Hook hook) {
        ReputationHooks.register(hook);
    }

    /** Mints a brand-new Faction (e.g. a Kingdom) and returns its id -- see {@code faction.FactionRecord}'s own doc. */
    public static UUID createFaction(String name) {
        return FactionStore.create(name);
    }

    /** Whether {@code factionId} is a real minted Faction, as opposed to a plain settlementCoreId acting as its own implicit faction. */
    public static boolean isMintedFaction(UUID factionId) {
        return FactionStore.exists(factionId);
    }

    /** {@code factionId}'s display name, whether it's a minted Faction or a settlement acting as its own implicit one. */
    public static Optional<String> resolveFactionName(ServerLevel level, UUID factionId) {
        Optional<String> minted = FactionStore.getName(factionId);
        return minted.isPresent() ? minted : Settlemynts.getAnySettlementName(level, factionId);
    }

    /** Which Faction currently controls {@code settlementCoreId}, if any -- see {@code faction.SettlementControlStore}'s own doc. */
    public static Optional<UUID> getSettlementController(UUID settlementCoreId) {
        return SettlementControlStore.getController(settlementCoreId);
    }

    public static void setSettlementController(UUID settlementCoreId, UUID controllingFactionId) {
        SettlementControlStore.setController(settlementCoreId, controllingFactionId);
    }
}
