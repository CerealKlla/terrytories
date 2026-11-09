package com.github.cerealklla.terrytories;

import java.util.List;

import com.github.cerealklla.terrytories.reputation.client.ClientReputationBarState;
import com.github.cerealklla.terrytories.reputation.client.ReputationBarOverlay;
import com.github.cerealklla.terrytories.reputation.client.ReputationTab;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Contributes Terrytories' "Reputation" tab to Nuclyus' shared K menu, and the transient reputation-gain bar HUD -- see {@code com.github.cerealklla.nuclyus.menu.client.MenuContributor}'s own doc, and {@code reputation.client.ReputationBarOverlay}'s own doc (a direct port of Lyfe's own XP bar HUD). */
@Mod(value = TerrytoriesMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = TerrytoriesMod.MODID, value = Dist.CLIENT)
public class TerrytoriesModClient {

    public TerrytoriesModClient(ModContainer container) {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        TerrytoriesMod.LOGGER.info("Terrytories client setup");
        com.github.cerealklla.nuclyus.menu.client.NuclyusMenuRegistry.registerContributor(
                () -> List.of(new ReputationTab()));
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(TerrytoriesMod.MODID, "reputation_bar_overlay"), new ReputationBarOverlay());
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        ClientReputationBarState.tick();
    }
}
