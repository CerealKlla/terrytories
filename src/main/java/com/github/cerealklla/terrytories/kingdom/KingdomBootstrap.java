package com.github.cerealklla.terrytories.kingdom;

import com.github.cerealklla.terrytories.config.TerrytoriesConfig;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Loads (or, on first boot, generates) the world-spawn Kingdom -- mirrors Arryval's own {@code
 * starttown.StartingSettlementBootstrap}'s identical shape. {@link
 * TerrytoriesConfig#KINGDOM_GENERATION_TIMING} controls *when* this runs on a fresh boot: EAGER
 * (default, the original behavior) runs it synchronously at {@code ServerStartingEvent}; LAZY
 * defers it to roughly {@link #LAZY_DELAY_TICKS} (1 real minute) into the first real tick after
 * boot instead. Added 2026-11-09 after {@code KingdomGenerator#generateInitialKingdom}'s own {@code
 * Geometry.Polygon.expandedBy} call turned out to be slow enough on one real world's settlement
 * layout to trip the server watchdog during startup -- LAZY is a workaround for *when* the cost
 * lands (off the startup critical path), not a fix for the cost itself, which stays a separate,
 * still-open item.
 */
public final class KingdomBootstrap {

    private static final long LAZY_DELAY_TICKS = 1200L;

    private MinecraftServer pendingServer;
    private long ticksWaited;
    private boolean started;

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        if (TerrytoriesConfig.KINGDOM_GENERATION_TIMING.get() == TerrytoriesConfig.KingdomGenerationTiming.EAGER) {
            started = true;
            KingdomStore.loadOrGenerate(event.getServer());
        } else {
            pendingServer = event.getServer();
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (started || pendingServer == null) {
            return;
        }
        if (++ticksWaited < LAZY_DELAY_TICKS) {
            return;
        }
        started = true;
        KingdomStore.loadOrGenerate(pendingServer);
        pendingServer = null;
    }
}
