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
- Only the bare mod skeleton exists so far (`TerrytoriesMod`, logs "Terrytories common setup" on
  boot). `./gradlew build` green standalone. **Nothing else built yet** — see
  [context/design-document.md](context/design-document.md) for the full phased plan (reputation
  data model/API, combat/targeting hooks, economy/quest-gating hooks, world-spawn Kingdom
  generation, the mayor's letter).

See [context/classes/](context/classes/) for per-class reference, once there is one.
