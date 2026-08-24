# SimpleFactions — agent guide

Instructions for AI agents and contributors adding or changing code in **SimpleFactions** (`simplefactions/`).

Planning batches for large work live under `ProvinceSystem/Planning/batches/`. War feature order: `ProvinceSystem/Planning/war-build-order.md`.

---

## How SF is organized (author style)

SimpleFactions favors **domain packages** and **orchestrator managers**, not one-class-per-tiny-helper sprawl.

| Area | Pattern | Examples |
|------|---------|----------|
| Domain state | Few fat types | `Objects/Faction.java`, `War/core/War.java` |
| Player-facing flows | `Managers/*Manager` + `Managers/Inventory/*` | `FactionManager`, `WarView`, `CampaignView` |
| Persistence | `Database/*Data` POJOs + mappers | `Database.java`, `WarMapper` |
| Config / flags | `Cache` static fields | Loaded from `config.yml` |
| Feature domains | Lowercase subfolders | `government/session`, `Guild`, `Map/Provinces` |

**War** was built in many batches; step **75** reorganized war packages (complete 2026-08-24). Follow [step-75 lock](../ProvinceSystem/Planning/batches/step-75/01-planning-lock.md) for the migration map.

---

## Package layout

```text
me.Plugins.SimpleFactions/
├── Objects/              # Core domain POJOs (Faction, Bank, …)
├── Managers/             # Orchestration, commands, GUIs
├── Database/             # Gson/load-save DTOs
├── Map/                  # Provinces, grid
├── government/           # Laws, elections, movement
├── War/
│   ├── core/             # War, Side, WarMapper, declare helpers
│   ├── enums/
│   ├── declare/          # Validation at war declare
│   ├── pathfinder/
│   ├── campaign/
│   │   ├── schedule/     # Campaign battle list build (FB legs, placer)
│   │   ├── zoc/          # Fort/port ZOC indexes
│   │   ├── runtime/      # Hourly battle window, tick, autoresolve
│   │   ├── vote/         # Battle hour voting, quorum
│   │   ├── admin/        # Staff warschedule tools
│   │   ├── ui/           # Schedule copy/formatting/logging
│   │   └── progression/  # Cursor, occupation, peace, post-battle choices
│   └── battle/
│       ├── engine/core|capture|win|raid/
│       ├── campaign/     # Launch scheduled campaign battles; BattleNamingService (75.04)
│       ├── warband/, military/, template/, persistence/, ui/
│       └── …
└── SimpleFactions.java   # Bootstrap, listener registration
```

**Rule:** no more than ~**12 sibling `.java` files** in one directory. If you exceed that, add a subpackage named after the subdomain.

---

## Where new code goes

Use this table before creating a file.

| You are adding… | Put it in… | Avoid… |
|-----------------|------------|--------|
| Campaign slot insertion, trim, validator | `War/campaign/schedule` | `War/schedule`, new `*Helper` at `War/` root |
| Fort/port ZOC lookup | `War/campaign/zoc` | Schedule builder file |
| Hourly battle open/close, autoresolve | `War/campaign/runtime` | Mixing with schedule build |
| Vote tally, quorum, postpone | `War/campaign/vote` | New 5-line `*Result.java` (nest in `VoteResults`) |
| Fort/port ZOC operational DTOs | nested in `FortZocIndex` / `PortSeaZocIndex` | Standalone `OperationalFort.java` files |
| Staff `/warschedule` output | `War/campaign/admin` | Chat formatters in engine |
| GUI lore / schedule debug lines | `War/campaign/ui` | `Managers` unless it is pure inventory layout |
| Path B, axis, dijkstra | `War/pathfinder` | Schedule package |
| Occupation, white peace, push/hold | `War/campaign/progression` | Flat `War/` root |
| Live battle instance, sides, join | `War/battle/engine/core` | Flat `engine/` |
| Capture points, markers | `War/battle/engine/capture` | Template package |
| Field/siege/raid win checks | `War/battle/engine/win` or `raid` | One new `*Service` per if-branch |
| Campaign battle display names | `War/battle/campaign` | New `battle/naming` package |
| Warband signup for campaign battle | `War/battle/campaign` | `engine/core` |
| War domain types (War, Side, WarMapper) | `War/core` | `War/` root |
| Lives, pool, casualties | `War/battle/military` | Battle engine |
| YAML battle templates | `War/battle/template` | Hard-coded in engine |
| Faction ledger / war declare GUI | `Managers/Inventory` | War package UI for non-war commands |
| General file logging | `Managers/LogManager` | War-specific log managers |
| REST/export shape | Keep stable; change mappers + PS docs together | Renaming JSON fields casually |

---

## Naming conventions

| Suffix | Use when |
|--------|----------|
| `*Manager` | Owns lifecycle, maps, listeners (`WarManager`, `BattleManager`, `WarbandManager`) |
| `*Service` | Stateless orchestration over a domain (`WarCampaignService`, `BattleVoteService`) |
| `*Helper` | Pure functions for one workflow (`WarDeclareHelper`) - prefer merging into Manager if &lt;100 lines |
| `*Validator` | Declarative rules, returns result type |
| `*Mapper` | Domain ↔ `Database/*Data` |
| `*View` / `*Creator` | Inventory GUI (`Managers/Inventory`) |
| `*Data` | Gson DTO in `Database/` only |

Do **not** create:

- A new top-level package for one class (`battle/naming`, `battle/util`)
- A separate file for a record used in only one service (use nested record or keep in same file)
- `WarCampaignLogManager`-style duplicates of `LogManager`

---

## War development rules

1. **Schedule changes:** only `CampaignBattlePlacer.placeBattle` (or documented builder phase) mutates leg lists. Read [70d lock](../ProvinceSystem/Planning/batches/step-70d/02-planning-lock-fb.md) before editing insertion.
2. **Tests:** any war change should run `mvn test -Dtest="me.Plugins.SimpleFactions.War.**"`.
3. **Brume acceptance:** invasion `709 FIELD → 713 SIEGE → 705 required`; do not regress when touching schedule/pathfinder.
4. **Persistence:** if you add a field to `ScheduledCampaignBattle` or `War`, update `Database/*Data`, `WarMapper`, and tests in the same change.
5. **Player-facing text:** no em dash (`—`); use `-` or `:`. See workspace rule `no-em-dash.mdc`.
6. **Config:** new toggles go in `config.yml` + `Cache`, not hard-coded in services.

---

## Non-war SimpleFactions rules

1. **Managers** own Bukkit listeners when the feature is cohesive; do not add listener classes for one event unless splitting an existing giant manager.
2. **Objects** hold domain state; avoid putting Bukkit API in `Objects` when possible.
3. **Database** stores POJOs; domain logic stays in Managers or feature packages.
4. **Loaders** (`Loaders/`) are for static YAML registries, one loader per file.
5. Match existing indentation and naming in the file you edit.

---

## Refactor policy for agents

| Task type | Approach |
|-----------|----------|
| New feature | Add to correct subpackage per table above; extend existing service if &lt;50 lines |
| Bug fix | Minimal diff; no drive-by repackage |
| Repackage | One batch per PR; tests green; follow layout rules in this doc |
| Merge small types | Nest in parent type (e.g. `VoteResults`) unless a standalone file is clearly warranted |

When moving classes: IDE refactor/move, update main + test + Database imports, grep old package string.

---

## Verify before finishing

```bash
cd simplefactions && mvn test -Dtest="me.Plugins.SimpleFactions.War.**"   # war changes
cd simplefactions && mvn test                                              # broad changes
```

For schedule/pathfinder work, confirm tests include:

- `CampaignScheduleBuilderTest`
- `ProvincePathfinderTest`
- `CampaignScheduleValidator` / Brume-shaped cases

---

## Related docs

- [Wars.md](Documentation/Wars.md) - gameplay spec
- [step-75 package lock](../ProvinceSystem/Planning/batches/step-75/01-planning-lock.md) - migration map (step 75 repackage complete 2026-08-24)
- [war-build-order.md](../ProvinceSystem/Planning/war-build-order.md) - feature sequence
