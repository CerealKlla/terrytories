package com.github.cerealklla.terrytories.kingdom;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/** Loads (or, on first boot, generates) the world-spawn Kingdom -- mirrors Arryval's own {@code starttown.StartingSettlementBootstrap}'s identical shape. */
public final class KingdomBootstrap {

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        KingdomStore.loadOrGenerate(event.getServer());
    }
}
