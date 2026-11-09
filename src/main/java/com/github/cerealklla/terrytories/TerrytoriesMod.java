package com.github.cerealklla.terrytories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.cerealklla.cartographyr.api.Cartography;
import com.github.cerealklla.terrytories.api.Terrytories;
import com.github.cerealklla.terrytories.config.TerrytoriesConfig;
import com.github.cerealklla.terrytories.faction.FactionBootstrap;
import com.github.cerealklla.terrytories.kingdom.KingdomBootstrap;
import com.github.cerealklla.terrytories.kingdom.KingdomLayer;
import com.github.cerealklla.terrytories.network.ReputationPayload;
import com.github.cerealklla.terrytories.network.RequestReputationPayload;
import com.github.cerealklla.terrytories.reputation.CombatReputationListener;
import com.github.cerealklla.terrytories.reputation.GuardHostilityBridge;
import com.github.cerealklla.terrytories.reputation.QuestReputationListener;
import com.github.cerealklla.terrytories.reputation.ReputationGainPayload;
import com.github.cerealklla.terrytories.reputation.TownDefenseReputationListener;
import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(TerrytoriesMod.MODID)
public class TerrytoriesMod {

    public static final String MODID = "terrytories";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TerrytoriesMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);
        modContainer.registerConfig(ModConfig.Type.SERVER, TerrytoriesConfig.SPEC);

        Cartography.registerLayer(KingdomLayer.LAYER);

        new QuestReputationListener();
        NeoForge.EVENT_BUS.register(new CombatReputationListener());
        NeoForge.EVENT_BUS.register(new TownDefenseReputationListener());
        NeoForge.EVENT_BUS.register(new FactionBootstrap());
        NeoForge.EVENT_BUS.register(new KingdomBootstrap());
        GuardHostilityBridge.install();
    }

    private void commonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        LOGGER.info("Terrytories common setup");
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(RequestReputationPayload.TYPE, RequestReputationPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        sendReputation(player);
                    }
                });
        registrar.playToClient(ReputationPayload.TYPE, ReputationPayload.STREAM_CODEC,
                (payload, context) -> com.github.cerealklla.terrytories.reputation.client.ReputationTab.setEntries(payload.entries()));
        registrar.playToClient(ReputationGainPayload.TYPE, ReputationGainPayload.STREAM_CODEC,
                (payload, context) -> com.github.cerealklla.terrytories.reputation.client.ClientReputationBarState.onReputationGain(
                        payload.factionName(), payload.oldValue(), payload.newValue()));
    }

    /** Only every Faction (settlement or minted) with a genuinely nonzero reputation is sent -- see {@link ReputationPayload}'s own doc. */
    private static void sendReputation(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        List<ReputationPayload.Entry> entries = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : Terrytories.getAllReputation(player).entrySet()) {
            if (entry.getValue() == 0) {
                continue;
            }
            String name = Terrytories.resolveFactionName(level, entry.getKey()).orElse("Unknown Faction");
            entries.add(new ReputationPayload.Entry(name, entry.getValue()));
        }
        entries.sort((a, b) -> Integer.compare(b.value(), a.value()));
        PacketDistributor.sendToPlayer(player, new ReputationPayload(entries));
    }
}
