package com.github.cerealklla.terrytories.faction;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.github.cerealklla.nuclyus.server.PersistedServerStore;

import net.minecraft.server.MinecraftServer;

/**
 * Server-wide (never per-player) {@code settlementCoreId -> controllingFactionId} mapping -- "a
 * data point in the settlement is the Faction who controls the settlement currently," explicit
 * user spec, 2026-11-08. Starts empty; a settlement with no entry here is simply uncontrolled (no
 * trickle-share reputation to anyone -- see {@code reputation.QuestReputationListener}). Entries
 * are expected to change over time (a settlement captured by a different Kingdom later) -- this is
 * exactly why this is tracked separately from, and never affects, a player's own reputation with
 * the settlement itself (see this mod's own decisions.md for the user's own "the denizens still
 * remember the player" framing).
 */
public final class SettlementControlStore {

    private static final PersistedServerStore<UUID, SettlementControl> STORE =
            new PersistedServerStore<>("terrytories_settlement_control.json", SettlementControl.CODEC, SettlementControl::settlementCoreId) {
                @Override
                protected Map<UUID, SettlementControl> generate() {
                    return Map.of();
                }
            };

    private SettlementControlStore() {
    }

    public static void loadOrGenerate(MinecraftServer server) {
        STORE.loadOrGenerate(server);
    }

    public static Optional<UUID> getController(UUID settlementCoreId) {
        return STORE.get(settlementCoreId).map(SettlementControl::controllingFactionId);
    }

    public static void setController(UUID settlementCoreId, UUID controllingFactionId) {
        STORE.put(settlementCoreId, new SettlementControl(settlementCoreId, controllingFactionId));
    }
}
