package com.github.cerealklla.terrytories;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TerrytoriesMod.MODID)
public class TerrytoriesMod {

    public static final String MODID = "terrytories";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TerrytoriesMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        LOGGER.info("Terrytories common setup");
    }
}
