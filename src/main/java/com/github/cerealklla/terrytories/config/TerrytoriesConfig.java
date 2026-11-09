package com.github.cerealklla.terrytories.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-administrator-facing tuning knobs for Terrytories. */
public final class TerrytoriesConfig {

    public enum KingdomGenerationTiming {
        EAGER, LAZY
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<KingdomGenerationTiming> KINGDOM_GENERATION_TIMING = BUILDER
            .comment("Controls when the one-time world-spawn Kingdom generation runs on a fresh world. "
                    + "EAGER runs it synchronously while the server is still starting up (the original "
                    + "behavior, where it ran until 2026-11-09); LAZY defers it to roughly 1 real minute "
                    + "after the server has finished booting instead, so a slow run (see "
                    + "kingdom.KingdomGenerator's own doc -- Geometry.Polygon.expandedBy can be slow on "
                    + "a sprawling settlement layout) can't trip the startup watchdog. Only matters once, "
                    + "the very first time a world boots -- every later boot just loads the "
                    + "already-persisted result regardless of this setting.")
            .defineEnum("kingdomGenerationTiming", KingdomGenerationTiming.EAGER);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private TerrytoriesConfig() {
    }
}
