package com.github.cerealklla.terrytories.faction;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.github.cerealklla.nuclyus.server.PersistedServerStore;

import net.minecraft.server.MinecraftServer;

/**
 * Server-wide (never per-player) registry of minted {@link FactionRecord}s -- see that class's own
 * doc for why only non-settlement-backed factions (Kingdoms, future territory-free factions) ever
 * need a real entry here. Starts empty; grows over time via {@link #create} (world-spawn Kingdom
 * generation is the first real caller), unlike every other {@code
 * nuclyus.server.PersistedServerStore} consumer in this suite, which generates its whole content
 * once and never changes it afterward -- this one uses the newer incremental {@link
 * PersistedServerStore#put} path instead (added 2026-11-03 for Audyble's NPC identity stores).
 */
public final class FactionStore {

    private static final PersistedServerStore<UUID, FactionRecord> STORE =
            new PersistedServerStore<>("terrytories_factions.json", FactionRecord.CODEC, FactionRecord::id) {
                @Override
                protected Map<UUID, FactionRecord> generate() {
                    return Map.of();
                }
            };

    private FactionStore() {
    }

    public static void loadOrGenerate(MinecraftServer server) {
        STORE.loadOrGenerate(server);
    }

    /** Mints a brand-new Faction with a fresh random id, persists it immediately, and returns that id. */
    public static UUID create(String name) {
        UUID id = UUID.randomUUID();
        STORE.put(id, new FactionRecord(id, name));
        return id;
    }

    public static Optional<String> getName(UUID id) {
        return STORE.get(id).map(FactionRecord::name);
    }

    /** Whether {@code id} is a real minted Faction (as opposed to a plain settlementCoreId acting as its own implicit faction). */
    public static boolean exists(UUID id) {
        return STORE.get(id).isPresent();
    }
}
