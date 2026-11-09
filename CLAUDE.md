# Terrytories

Kingdoms + per-settlement/per-kingdom Reputation mod for a modular Minecraft project (Minecraft
Java Edition). Part of a suite of intercommunicating mods that expose public APIs so other mods —
the user's own and third parties' — can integrate.

**Naming note**: the overall mod suite/modpack is branded "Kyngdoms" (AutoModpack's `modpack.name`,
`Kyngdoms.mrpack`, etc.) — this mod is a separate thing, **Terrytories**, named distinctly on
purpose (explicit user choice, 2026-11-08) to avoid confusion between the suite's own branding and
this one mod.

## Context directory — read this first

`context/` is a **separate private repo** (https://github.com/CerealKlla/terrytories-context), not
part of this one — it's listed in `.gitignore` here and must never be committed to this repo. It's
cloned as a subdirectory at `context/` for local convenience. If this directory is missing (e.g. a
fresh clone of just this repo), restore it with:

```
git clone https://github.com/CerealKlla/terrytories-context.git context
```

- `context/design-document.md` — the authoritative design spec. Start here for anything about
  intended shape or scope.
- `context/decisions.md` — dated log of decisions made during implementation that extend or
  override the design document, with rationale.
- `context/classes/` — one short markdown file per implemented class, once there is one.

Keep this current the same way every other mod in this suite does: update design-document.md/
decisions.md when a decision is made, update classes/ when a class's public surface changes,
commit+push context/ separately from this repo's own commits.

## Status

Scaffolded 2026-11-08 (see [context/decisions.md](context/decisions.md)):
- Loader: **NeoForge**, Minecraft **26.1.2**, Java **25**
- Group ID: `com.github.cerealklla.terrytories` / Mod ID: `terrytories`
- **Required dependencies on Nuclyus, Cartographyr, Settlemynts, Myssions, Yconomics, and
  Coryspondance** — none optional; the whole feature needs all six.
- Two prerequisite changes landed in sibling mods the same day: Myssions gained a global
  quest-end hook (`QuestEndHooks#registerGlobal`) and an issuing-plot lookup
  (`api.Myssions#getIssuingPlotId`); Settlemynts gained `api.Settlemynts#findSettlementCoreIdForPlot`
  — both needed so this mod (and Arryval's own new "do 10 quests for your village" quest) can learn
  which settlement a completed quest was actually done for, without either consumer needing to
  enumerate every quest id that will ever exist.
- **Phase D (reputation core) built, 2026-11-08** (see [context/decisions.md](context/decisions.md)):
  per-player `data.ReputationLedger` (settlement + kingdom, modeled on Lyfe's own `PlayerSkills`),
  `reputation.Standing` tiers (Hostile/Neutral/Friendly), `reputation.ReputationHooks` (generic
  standing-changed hook), quest-completion reputation gain, combat reputation loss + Friendly mutual
  damage immunity, and Guard hostile-targeting (via a new Settlemynts-side extension point — Guards
  only, worker NPCs/tagged natural Villagers have no combat capability to extend). `api.Terrytories`
  facade is the real public surface. `./gradlew build` green, whole-suite (15 mods) rebuild green.
  **Not yet live-tested** — no in-game way to view a reputation value yet (no HUD/command).
- **Reputation tab added to Nuclyus's shared K menu, 2026-11-08** — a scrollable list of every
  settlement with nonzero reputation (name, value, standing), via a dedicated request/response
  payload pair. First client-only code in this mod (`TerrytoriesModClient`). **Not yet
  live-tested.**
- **Phase E built, 2026-11-08** (see [context/decisions.md](context/decisions.md)): Friendly
  settlements grant a Shop price bonus (Settlemynts' new `bridge.TerrytoriesReputationBridge`,
  composed with the Merchant-skill bonus); Myssions gained a new player-aware
  `quest.QuestAcceptanceGate` for reputation-gated quest acceptance (pure infra, no quest uses it
  yet). Trade-route gating and Kingdom-settlement standing remain deferred (no existing hook /
  unsettled design, respectively).
- **Phase F complete, 2026-11-08**: world-spawn Kingdom generation (a new `terrytories:kingdom`
  Cartographyr layer, nearest 8 settlements to origin convex-hulled and buffered into one Kingdom
  polygon on first boot) and the mayor's founding letter (Arryval's own `starttown
  .FoundingLetterListener`, subscribed to this mod's generic `reputation.ReputationHooks` — no
  code needed here at all, confirming the "Terrytories never depends on Arryval" dependency
  direction held up in practice).
- **Reputation model reworked around a real Faction list, same day** (see
  [context/decisions.md](context/decisions.md)) — explicit user correction: Factions (e.g. "Town of
  Cresview," "Kingdom of Dern") are now first-class, and a Settlement separately carries one data
  point, which Faction currently controls it. New `faction` package
  (`FactionRecord`/`FactionStore`/`SettlementControlStore`, both server-wide). The old Settlement-vs-
  Kingdom ledger split is gone — one unified `FACTION_REPUTATION` ledger, since a settlement already
  doubles as its own implicit Faction. The world-spawn Kingdom is now a real minted Faction
  controlling its 8 member settlements. **This closes out the full original plan, Phases A-F, end
  to end.** See [context/design-document.md](context/design-document.md)'s "Open,
  deliberately-deferred items" section for what's intentionally still not built — trade-route
  gating, and (confirmed still unsettled, explicitly re-stated by the user this same session) a
  Settlement's own political standing with its controlling Kingdom, as distinct from player
  reputation. **Nothing in this whole feature arc has been live-tested yet.**

**World-spawn Kingdom generation gained an EAGER/LAZY timing toggle, 2026-11-09** (see
[context/decisions.md](context/decisions.md)) — found incidentally while boot-testing an unrelated
new mod: `kingdom.KingdomGenerator#generateInitialKingdom`'s `Geometry.Polygon.expandedBy` call was
slow enough on the shared Dev Server's own settlement layout to trip the server watchdog during
boot (not a risk to Production, which already generated and persisted its own Kingdom
successfully before this was noticed). The underlying performance issue in `expandedBy` itself is
still open, not fixed this pass — explicit user choice. New `config.TerrytoriesConfig
#KINGDOM_GENERATION_TIMING` lets `kingdom.KingdomBootstrap` run the one-time generation either
EAGER (default, at `ServerStartingEvent`, original behavior) or LAZY (deferred ~1 real minute into
the first tick after boot, off the startup path). `./gradlew build` green, whole-suite (17 mods)
rebuild + `runServer` boot-smoke-test clean with the Dev Server set to LAZY. **The LAZY path's
actual firing hasn't been directly observed yet** — only its mechanical soundness and that it gets
boot off the critical path are confirmed so far.

See [context/classes/](context/classes/) for per-class reference, once there is one.
