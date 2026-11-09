package com.github.cerealklla.terrytories.reputation;

/**
 * Reputation tiers derived from a raw {@code HolderReputation} value (range {@link
 * HolderReputation#MIN}..{@link HolderReputation#MAX}). Thresholds are placeholder/tunable, same
 * "flagged, revisit by feel" convention every other distance/balance constant in this suite uses --
 * tuned so Arryval's "10 quests for your village, +5 reputation each" milestone exactly crosses
 * {@link #FRIENDLY}, matching the user's own stated math.
 */
public enum Standing {
    HOSTILE,
    NEUTRAL,
    FRIENDLY;

    public static final int HOSTILE_THRESHOLD = -50;
    public static final int FRIENDLY_THRESHOLD = 50;

    public static Standing forValue(int reputation) {
        if (reputation <= HOSTILE_THRESHOLD) {
            return HOSTILE;
        }
        if (reputation >= FRIENDLY_THRESHOLD) {
            return FRIENDLY;
        }
        return NEUTRAL;
    }
}
