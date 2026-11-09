package com.github.cerealklla.terrytories.reputation;

import java.util.Map;
import java.util.UUID;

import com.github.cerealklla.nuclyus.server.PersistedServerStore;

import net.minecraft.server.MinecraftServer;

/**
 * Server-wide reputation storage, keyed by an arbitrary holder id -- see {@link HolderReputation}'s
 * own doc for why a holder isn't restricted to a player. Replaced the earlier per-player
 * {@code net.neoforged.neoforge.attachment} approach outright, 2026-11-08: a NeoForge data
 * attachment can only ever be hung off a real game entity (a {@code Player}, mainly), which
 * structurally can't represent "a Settlement's own reputation with a Kingdom" at all -- a natural
 * village has no entity whatsoever. Same "generate empty, grow via incremental {@code put}" shape
 * as {@code faction.FactionStore}/{@code faction.SettlementControlStore}.
 */
public final class ReputationStore {

    private static final PersistedServerStore<UUID, HolderReputation> STORE =
            new PersistedServerStore<>("terrytories_reputation.json", HolderReputation.CODEC, HolderReputation::holderId) {
                @Override
                protected Map<UUID, HolderReputation> generate() {
                    return Map.of();
                }
            };

    private ReputationStore() {
    }

    public static void loadOrGenerate(MinecraftServer server) {
        STORE.loadOrGenerate(server);
    }

    public static int get(UUID holderId, UUID factionId) {
        return STORE.get(holderId).map(h -> h.get(factionId)).orElse(0);
    }

    /** Returns the new, clamped value. */
    public static int adjust(UUID holderId, UUID factionId, int delta) {
        HolderReputation current = STORE.get(holderId).orElse(HolderReputation.empty(holderId));
        HolderReputation updated = current.adjust(factionId, delta);
        STORE.put(holderId, updated);
        return updated.get(factionId);
    }

    public static Map<UUID, Integer> all(UUID holderId) {
        return STORE.get(holderId).map(HolderReputation::reputation).orElse(Map.of());
    }
}
