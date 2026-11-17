package com.github.cerealklla.terrytories.reputation;

import com.github.cerealklla.settlemynts.api.Settlemynts;
import com.github.cerealklla.terrytories.api.Terrytories;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * "Attacking NPCs for a settlement reduces your rep with that settlement substantially" + "above a
 * certain amount you'll be 'Friendly' to that Settlement's NPCs, meaning you cannot damage each
 * other any more" -- explicit user spec, both halves. Resolves a settlement NPC via {@code
 * api.Settlemynts#findSettlementCoreIdForEntity} (worker/tagged-natural-Villager via its plot, or a
 * Guard directly via its own {@code effectiveSettlementCoreId} -- see that facade method's own doc).
 *
 * <p>Real fix, 2026-11-17: this used to resolve only via {@code findPlotIdForEntity}, which returns
 * nothing at all for a natural-village Guard (no real {@code plotId} exists for one) -- silently
 * defeating BOTH halves below for any such Guard, which is exactly what let a Friendly player keep
 * landing real, rep-costing hits on one ("I was friendly with my town and then was still able to
 * damage them and lost a shit load of reputation again").
 *
 * <p>Two separate real events, deliberately: {@link LivingIncomingDamageEvent} (fires before damage
 * is actually applied, same event {@code lyfe.durability.UnbreakableToolListener} already zeroes
 * damage through) is where Friendly mutual immunity is enforced -- zeroing the amount, the
 * established idiom in this suite for "no damage happens" via this event (it carries no real
 * cancel flag consumed elsewhere). {@link LivingDamageEvent.Post} (fires only once real damage
 * actually landed, mirrors {@code lyfe.combat.CombatStateTracker}'s own {@code getHealthDamage() <=
 * 0} guard) is where the reputation penalty is applied -- a hit fully absorbed/blocked (0 real
 * damage) shouldn't cost reputation either.
 */
public final class CombatReputationListener {

    /** Placeholder/tunable -- "substantially," per the user's own wording; several times a single quest's reward. */
    public static final int REPUTATION_LOSS_PER_ATTACK = -25;

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0 || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        resolveMutualPair(level, event.getSource().getEntity(), event.getEntity()).ifPresent(pair -> {
            if (Terrytories.getStanding(pair.player(), pair.settlementCoreId()) == Standing.FRIENDLY) {
                event.setAmount(0.0f);
            }
        });
    }

    @SubscribeEvent
    public void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getHealthDamage() <= 0 || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == event.getEntity()) {
            return;
        }
        Settlemynts.findSettlementCoreIdForEntity(level, event.getEntity())
                .ifPresent(settlementCoreId -> Terrytories.adjustReputation(attacker, settlementCoreId, REPUTATION_LOSS_PER_ATTACK));
    }

    private record MutualPair(ServerPlayer player, java.util.UUID settlementCoreId) {
    }

    /** Either direction (player attacking a settlement NPC, or a settlement NPC attacking a player) resolves to the same pair -- Friendly immunity is symmetric. */
    private static java.util.Optional<MutualPair> resolveMutualPair(ServerLevel level, Entity attacker, LivingEntity victim) {
        if (attacker instanceof ServerPlayer player) {
            return Settlemynts.findSettlementCoreIdForEntity(level, victim).map(coreId -> new MutualPair(player, coreId));
        }
        if (victim instanceof ServerPlayer player) {
            return Settlemynts.findSettlementCoreIdForEntity(level, attacker).map(coreId -> new MutualPair(player, coreId));
        }
        return java.util.Optional.empty();
    }
}
