package com.github.cerealklla.terrytories.kingdom;

import com.github.cerealklla.cartographyr.geo.Layer;

import net.minecraft.resources.Identifier;

/**
 * Terrytories' own Cartographyr {@link Layer} -- "vast geographic regions which can contain
 * multiple settlements under 1 banner." No built-in Kingdom-scale layer existed in Cartographyr
 * before this (confirmed via research, not assumed); registering a new one needs no Cartographyr-
 * side change at all (`Layer` is an open, trust-based registry). Placement 2 -- above Settlement
 * (1) and Region/Roadway (0), since a Kingdom is the broadest geographic tier.
 */
public final class KingdomLayer {

    public static final Identifier ID = Identifier.fromNamespaceAndPath("terrytories", "kingdom");
    public static final Layer LAYER = new Layer(ID, "Kingdom", 2);

    private KingdomLayer() {
    }
}
