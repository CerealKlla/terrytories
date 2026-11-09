package com.github.cerealklla.terrytories.reputation.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.cerealklla.terrytories.reputation.HolderReputation;
import com.github.cerealklla.terrytories.reputation.Standing;

/**
 * Client-side holder for every currently-active transient reputation bar, one per Faction display
 * name. Direct structural port of Lyfe's own {@code xpbar.client.ClientXpBarState} ("fade away
 * after 3 seconds" -- same fill/hold/fade timing, reused verbatim). Updated whenever a {@code
 * ReputationGainPayload} arrives ({@link #onReputationGain}); advanced once per client tick via
 * {@link #tick()}. No client-only imports beyond this package's own render-data record, so it's
 * harmless if classloaded on a dedicated server.
 *
 * <p>Unlike an XP bar (always filling toward a "next level"), reputation is a bounded value in
 * {@link HolderReputation#MIN}..{@link HolderReputation#MAX} with three discrete {@link Standing}
 * tiers rather than levels -- the bar's fill fraction is just the value's position in that range,
 * and its fill color reflects the *resulting* standing tier (red/grey/green), not the direction of
 * this specific change; the direction is shown instead as a "+N"/"-N" prefix on the label.
 */
public final class ClientReputationBarState {

    // All placeholder/tunable except HOLD_DURATION_TICKS, same "fade away after 3 seconds" spec
    // Lyfe's own XP bar already follows.
    private static final int FILL_DURATION_TICKS = 10;
    private static final int HOLD_DURATION_TICKS = 60;
    private static final int FADE_DURATION_TICKS = 10;
    private static final int TOTAL_LIFETIME_TICKS = FILL_DURATION_TICKS + HOLD_DURATION_TICKS + FADE_DURATION_TICKS;

    private static final Map<String, ActiveBar> ACTIVE = new HashMap<>();

    private ClientReputationBarState() {
    }

    public static void onReputationGain(String factionName, int oldValue, int newValue) {
        double startFraction = fractionOf(oldValue);
        double endFraction = fractionOf(newValue);
        ActiveBar existing = ACTIVE.get(factionName);
        // Continue from wherever the bar currently sits, not the stale start of a bar that's
        // already mid-animation -- repeated rapid gains (e.g. several defense kills in a row) read
        // as one continuously-refreshed bar, not a reset-and-restart flicker.
        double continueFrom = existing != null ? existing.currentFraction() : startFraction;
        int delta = newValue - oldValue;
        ACTIVE.put(factionName, new ActiveBar(factionName, delta, continueFrom, endFraction, Standing.forValue(newValue)));
    }

    private static double fractionOf(int value) {
        return (double) (value - HolderReputation.MIN) / (HolderReputation.MAX - HolderReputation.MIN);
    }

    public static void tick() {
        ACTIVE.values().removeIf(bar -> {
            bar.ticksAlive++;
            return bar.ticksAlive >= TOTAL_LIFETIME_TICKS;
        });
    }

    /** Sorted alphabetically by Faction name -- stable and deterministic as bars independently appear/expire. */
    public static List<RenderBar> activeBars() {
        List<RenderBar> result = new ArrayList<>(ACTIVE.size());
        for (ActiveBar bar : ACTIVE.values()) {
            result.add(new RenderBar(bar.factionName, bar.delta, bar.currentFraction(), bar.standing, bar.alpha()));
        }
        result.sort(Comparator.comparing(RenderBar::factionName));
        return result;
    }

    private static final class ActiveBar {
        final String factionName;
        final int delta;
        final double startFraction;
        final double endFraction;
        final Standing standing;
        int ticksAlive;

        ActiveBar(String factionName, int delta, double startFraction, double endFraction, Standing standing) {
            this.factionName = factionName;
            this.delta = delta;
            this.startFraction = startFraction;
            this.endFraction = endFraction;
            this.standing = standing;
        }

        double currentFraction() {
            if (ticksAlive >= FILL_DURATION_TICKS) {
                return endFraction;
            }
            double t = (double) ticksAlive / FILL_DURATION_TICKS;
            return startFraction + (endFraction - startFraction) * t;
        }

        float alpha() {
            int fadeStartTick = FILL_DURATION_TICKS + HOLD_DURATION_TICKS;
            if (ticksAlive < fadeStartTick) {
                return 1.0f;
            }
            float fadeProgress = (float) (ticksAlive - fadeStartTick) / FADE_DURATION_TICKS;
            return Math.max(0.0f, 1.0f - fadeProgress);
        }
    }

    public record RenderBar(String factionName, int delta, double fraction, Standing standing, float alpha) {
    }
}
