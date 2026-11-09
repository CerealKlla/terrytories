package com.github.cerealklla.terrytories.reputation;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Generic "some holder's standing with some Faction just changed" hook -- deliberately the only
 * thing this mod ever exposes about *why* reputation changed, so a consumer (Arryval's "first time
 * ever Friendly with any natural settlement, send a letter" rule) can react to the effect without
 * this mod needing to know anything about tutorials/onboarding at all. See this mod's own
 * design-document.md "Dependency direction" section -- Terrytories never depends on Arryval.
 *
 * <p>{@code holderId} is generic (see {@link HolderReputation}'s own doc -- a player, a settlement,
 * or a Kingdom can all be holders), NOT necessarily a player -- a consumer that only cares about
 * players (like Arryval's letter) must resolve it back to a {@code ServerPlayer} itself (e.g. via
 * {@code ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(holderId)}) and skip if
 * that comes back null.
 */
public final class ReputationHooks {

    @FunctionalInterface
    public interface Hook {
        void onStandingChanged(UUID holderId, UUID factionId, Standing oldStanding, Standing newStanding);
    }

    private static final List<Hook> HOOKS = new CopyOnWriteArrayList<>();

    private ReputationHooks() {
    }

    public static void register(Hook hook) {
        HOOKS.add(hook);
    }

    public static void fire(UUID holderId, UUID factionId, Standing oldStanding, Standing newStanding) {
        if (oldStanding == newStanding) {
            return;
        }
        for (Hook hook : HOOKS) {
            hook.onStandingChanged(holderId, factionId, oldStanding, newStanding);
        }
    }
}
