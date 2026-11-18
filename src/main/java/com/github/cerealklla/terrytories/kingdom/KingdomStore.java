package com.github.cerealklla.terrytories.kingdom;

import java.util.Map;

import com.github.cerealklla.nuclyus.server.PersistedServerStore;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Generate-once wrapper around {@link KingdomGenerator} -- same "generate once on first boot,
 * persist to a JSON file, reuse thereafter" shape every other one-shot server-wide generation in
 * this suite uses (e.g. Arryval's own {@code starttown.StartingSettlementStore}). If generation
 * fails (not enough settlements near origin yet to form a polygon -- see {@code KingdomGenerator}'s
 * own doc), the persisted file simply has no entry; this is NOT retried on a later boot even once
 * more settlements exist, since nothing currently triggers a re-check -- an accepted, flagged gap
 * for a feature meant to run once at genuine world-spawn, not an ongoing concern.
 *
 * <p>Real bug fixed 2026-11-17: {@link #generate} mints a brand-new Faction (via {@link
 * KingdomGenerator#generateInitialKingdom}'s {@code Terrytories.createFaction} call) as a side
 * effect every time it runs -- {@code supportsMergeMissing} is overridden off since this store only
 * ever has one entry (index 0) that can never need the generic self-heal {@code
 * PersistedServerStore} offers, and calling {@link #generate} speculatively just to check for a
 * missing key was silently minting a fresh, orphaned "Kingdom" Faction on every single restart (see
 * that method's own doc for the full account). Also switched to {@code worldScoped = true} -- the
 * exact same "tied to one specific world's seed/settlement layout, not the game instance as a
 * whole" bug Arryval's starting-settlement picker already needed this for.
 */
public final class KingdomStore {

    private static ServerLevel pendingOverworld;

    private static final PersistedServerStore<Integer, KingdomEntry> STORE =
            new PersistedServerStore<>("terrytories_kingdoms.json", KingdomEntry.CODEC, KingdomEntry::index, true) {
                @Override
                protected Map<Integer, KingdomEntry> generate() {
                    return KingdomGenerator.generateInitialKingdom(pendingOverworld)
                            .map(result -> Map.of(0, new KingdomEntry(0, result.territory().id().value(), result.factionId())))
                            .orElse(Map.of());
                }

                @Override
                protected boolean supportsMergeMissing() {
                    return false;
                }
            };

    private KingdomStore() {
    }

    public static void loadOrGenerate(MinecraftServer server) {
        pendingOverworld = server.overworld();
        STORE.loadOrGenerate(server);
        pendingOverworld = null;
    }
}
