package com.github.cerealklla.terrytories.kingdom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.github.cerealklla.cartographyr.api.Cartography;
import com.github.cerealklla.cartographyr.geo.Classification;
import com.github.cerealklla.cartographyr.geo.EntityDefinition;
import com.github.cerealklla.cartographyr.geo.EntityType;
import com.github.cerealklla.cartographyr.geo.GeographicEntity;
import com.github.cerealklla.cartographyr.geo.Geometry;
import com.github.cerealklla.cartographyr.geo.LifecycleState;
import com.github.cerealklla.terrytories.faction.FactionStore;
import com.github.cerealklla.terrytories.faction.SettlementControlStore;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

/**
 * "When the world is fresh spawned I'd like you to generate a Kingdom containing the first 8
 * settlements in a large polygon. This may include more within the area, that's fine." -- explicit
 * user spec. The Kingdom is now a real minted {@code faction.FactionRecord} too (explicit user
 * correction, 2026-11-08: "a data point in the settlement is the Faction who controls the
 * settlement currently") -- each of the 8 member settlements' {@code
 * faction.SettlementControlStore} entry is set to it, so quest-completion reputation
 * (@{code reputation.QuestReputationListener}) actually trickles a small share to this Kingdom.
 *
 * <p>Deliberately implemented independently here rather than depending on Arryval's own near-
 * identical {@code starttown.StartingSettlementStore#discover} -- see this mod's own
 * design-document.md "Dependency direction" section (Terrytories never depends on Arryval). The
 * search-ring technique (origin plus an 8-direction compass ring, each a fresh {@code
 * Cartography.predictSettlementAt} call) is copied, but simplified: this only needs real positions
 * to build a polygon from, never a confirmed-REALIZED/force-loaded bind location the way Arryval's
 * starting-town picker does, so none of that force-load-until-realized machinery is needed here.
 *
 * <p><b>Member settlements' {@code settlementCoreId} is derived assuming every one of the 8 is a
 * natural (world-gen) village</b> -- {@code new UUID(level.getSeed(), entity.id().value())}, the
 * exact same derivation Settlemynts itself uses (see {@code
 * api.Settlemynts#findSettlementCoreIdForPlot}'s own doc). Safe in practice: this runs once, on the
 * very first server boot, before any player has ever logged in to found anything -- there is no
 * real possibility of one of the 8 nearest-to-origin settlements already being player-founded at
 * that moment.
 */
public final class KingdomGenerator {

    private static final int DESIRED_SETTLEMENT_COUNT = 8;
    private static final double SEARCH_CENTER_DISTANCE_BLOCKS = 2500.0;
    private static final double SEARCH_RADIUS_BLOCKS = 2500.0;
    /** Placeholder/tunable -- how far the Kingdom's hull is padded out past its 8 corner settlements' own centers, so it comfortably contains their full footprints (and, per the user's own spec, "may include more within the area, that's fine"). */
    private static final int BUFFER_BLOCKS = 500;

    public record Result(GeographicEntity territory, UUID factionId) {
    }

    private KingdomGenerator() {
    }

    public static Optional<Result> generateInitialKingdom(ServerLevel level) {
        List<GeographicEntity> known = new ArrayList<>(Cartography.findEntities(level, Classification.CONSTRUCTED, com.github.cerealklla.cartographyr.geo.Layer.SETTLEMENT_ID));
        if (known.size() < DESIRED_SETTLEMENT_COUNT) {
            List<BlockPos> searchCenters = new ArrayList<>();
            searchCenters.add(BlockPos.ZERO);
            for (int i = 0; i < 8; i++) {
                double angle = i * (Math.PI / 4.0);
                searchCenters.add(new BlockPos(
                        (int) Math.round(Math.cos(angle) * SEARCH_CENTER_DISTANCE_BLOCKS), level.getSeaLevel(),
                        (int) Math.round(Math.sin(angle) * SEARCH_CENTER_DISTANCE_BLOCKS)));
            }
            for (BlockPos center : searchCenters) {
                Cartography.predictSettlementAt(level, center, SEARCH_RADIUS_BLOCKS).ifPresent(entity -> {
                    if (known.stream().noneMatch(existing -> existing.id().equals(entity.id()))) {
                        known.add(entity);
                    }
                });
            }
        }
        known.sort(Comparator.comparingDouble(KingdomGenerator::distanceSqFromOrigin));

        List<GeographicEntity> members = known.stream().limit(DESIRED_SETTLEMENT_COUNT).toList();
        List<Geometry.Polygon.Vertex> points = members.stream()
                .map(entity -> new Geometry.Polygon.Vertex(entity.geometry().centerBlockX(), entity.geometry().centerBlockZ()))
                .toList();
        if (points.size() < 3) {
            // Not enough known/predicted settlements near origin yet to form a real polygon from --
            // nothing to do; a later boot with more settlements discovered could still succeed, but
            // this isn't retried automatically (see KingdomStore's own doc).
            return Optional.empty();
        }

        Geometry hull = Geometry.Polygon.convexHull(points);
        Geometry kingdomGeometry = hull instanceof Geometry.Polygon polygon
                ? Geometry.Polygon.expandedBy(polygon, BUFFER_BLOCKS)
                : hull; // degenerate/collinear input -- Geometry.Polygon.convexHull's own fallback, left unbuffered rather than guessed at.

        String kingdomName = "The Kingdom"; // placeholder -- no naming system exists for this yet.
        EntityDefinition definition = new EntityDefinition(
                level.dimension(), Classification.CONSTRUCTED, new EntityType(Identifier.fromNamespaceAndPath("terrytories", "kingdom")),
                KingdomLayer.ID, Optional.of(kingdomName), kingdomGeometry, LifecycleState.REALIZED, Optional.empty());
        GeographicEntity territory = Cartography.createEntity(level, definition);

        UUID factionId = FactionStore.create(kingdomName);
        long worldSeed = level.getSeed();
        for (GeographicEntity member : members) {
            UUID settlementCoreId = new UUID(worldSeed, member.id().value());
            SettlementControlStore.setController(settlementCoreId, factionId);
        }

        return Optional.of(new Result(territory, factionId));
    }

    private static double distanceSqFromOrigin(GeographicEntity entity) {
        double x = entity.geometry().centerBlockX();
        double z = entity.geometry().centerBlockZ();
        return x * x + z * z;
    }
}
