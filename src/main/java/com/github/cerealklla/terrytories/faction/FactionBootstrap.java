package com.github.cerealklla.terrytories.faction;

import com.github.cerealklla.terrytories.reputation.ReputationStore;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/** Loads every server-wide store this mod owns -- must run before {@code kingdom.KingdomBootstrap}, which mints a Faction and sets settlement controllers on first boot. */
public final class FactionBootstrap {

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        FactionStore.loadOrGenerate(event.getServer());
        SettlementControlStore.loadOrGenerate(event.getServer());
        ReputationStore.loadOrGenerate(event.getServer());
    }
}
