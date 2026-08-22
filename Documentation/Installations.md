# Installations

> **Implementation status:** Steps 54–55 **and step 43 ZOC** **complete** (2026-08-19) — placement, map markers, construction queue, daily upkeep, faction GUI, fort zone-of-control hatch overlay on hover.

Military **installations** are named structures a faction can build on owned land: forts, ports, and airports. Each **operational** installation appears on the political map via `installations[]` in `map_markers.json`. Under-construction installations are **not** exported. Settlements and installations are independent — a province may have both a city and a fort.

---

## Concepts

| Term | Meaning |
|------|---------|
| **Installation** | Named fort, port, or airport on a single province (operational) |
| **Construction** | In-progress build; max **one** per faction; not on map until complete |
| **Kind** | `fort`, `port`, or `airport` |
| **Province** | Block coords at construct time; one installation of each kind per province per faction |

**Invariants:**

- At most **one of each kind** per province per faction (many forts across different provinces OK).
- **Direct faction ownership only** — `provinceHandler.hasProvince(P)`; vassal/subject land does not qualify.
- Independent of settlements — settlement + fort + port + airport on the same province is allowed.

---

## Data model

### `installation.Installation`

| Field | Type | Notes |
|-------|------|--------|
| `id` | `String` | From `Formatter.formatId` |
| `name` | `String` | Display name (colour codes via hex formatter) |
| `kind` | `InstallationKind` | `FORT`, `PORT`, `AIRPORT` |
| `province` | `int` | Province id |
| `centerX` / `centerZ` | `int` | Player block coords at construct |
| `completedAt` | `long` | Epoch ms when construction finished (0 on legacy saves) |

### `installation.InstallationConstruction` (in-progress)

| Field | Type | Notes |
|-------|------|--------|
| Same as `Installation` | | id, name, kind, province, centerX, centerZ |
| `timeLeft` | `int` | Seconds until operational |
| `startedAt` | `long` | Epoch ms at enqueue |

Persisted on faction JSON as `installation queue` (single object or null).

### `installation.handler.InstallationHandler` (per `Faction`)

| Responsibility | Notes |
|----------------|--------|
| `byId` | Lookup by operational installation id |
| `byProvinceKind` | One operational per kind per province |
| `pendingConstruction` | At most one in-progress build |
| `construct` | Validate + enqueue construction |
| `tick` | Decrement `timeLeft` each second; complete at 0 |
| `deconstruct` | Remove operational installation or cancel pending build |
| `payDailyUpkeep` | Daily pass in `Faction.newDay()`: withdraw upkeep or dissolve (cheapest first) |
| `onProvinceLost` | Cancel pending + dissolve operational on province |
| `validate()` | Cancel pending / dissolve if province no longer owned |
| `serialize()` / `load()` | Operational installations |
| `serializeConstruction()` / `loadConstruction()` | Pending build |

---

## Command

Player stands in the target province. Block coords taken from player location.

### `/faction construct <fort|port|airport> <name...>`

Leader only (`FactionManager.getByLeader`).

| Check | Behaviour |
|-------|-----------|
| Kind valid | `fort`, `port`, or `airport` |
| Name | Non-blank after trim |
| Ownership | Faction directly owns province |
| Duplicate | No existing installation of same kind on province |
| Land | Province valid and not sea/water |
| Port only | Within `port-sea-proximity-blocks` of sea or river |
| Unique id | No duplicate `id` within faction |
| Queue | Faction not already building another installation |

On success: enqueue construction (province+kind reserved). Leader sees time remaining. Installation registers and map updates when `timeLeft` reaches 0.

### `/faction deconstruct [id]`

Leader only.

| Form | Behaviour |
|------|-----------|
| No args | Opens **Installations** GUI (`INSTALLATIONS_VIEW`) |
| `<id>` | Opens confirm GUI for that installation or pending build |

Deconstruct always requires GUI confirm (green/red). Tab-complete includes pending ids.

### Faction GUI

Implemented in `Managers/Inventory/InstallationView.java` and `InstallationCreator.java`.

| Entry | Detail |
|-------|--------|
| Hub slot **32** | Installations tab (`MenuItemType.INSTALLATIONS`, march icon) — summary counts + total upkeep |
| `SFGUI.INSTALLATIONS_VIEW` | List view — operational installations (green concrete), active build in slot 39 |
| `SFGUI.INSTALLATION_DETAIL_VIEW` | Detail lore + deconstruct button (leader only) |
| Confirm | `InventoryManager.confirmView` with key `installation` + installation id |

---

## Territory loss

When the faction **loses** a province → cancel any pending construction on that province and dissolve **all** operational installations on that province. Leader notified if online.

---

## Daily upkeep

Each **operational** installation costs `daily-upkeep` denars per faction day (from `InstallationConfigLoader`). Pending constructions are excluded until complete.

**Payment** runs in `Faction.newDay()` **after** army upkeep, **before** `guild.newDay()`:

1. Collect operational installations; sort by `dailyUpkeep` ascending, then `completedAt` ascending.
2. For each: if faction bank has enough → `withdraw(upkeep)`; else destroy installation (cheapest first when broke).

**Ledger** (`Cashflow.INSTALLATIONS`) on the base guild shows the daily expense total in the guild book GUI. Payment is **not** applied again at `settleIncome()` — same model as army upkeep.

Leader message on non-payment destroy: `… has been destroyed §7(unable to pay upkeep)`.

---

## Dissolve

1. Remove from `byId` and `byProvinceKind`.
2. Enqueue map update.
3. Notify faction leader if online.

---

## Map export

See `Map/export/Markers.java` — `map_markers.json` per installation:

| Field | Source |
|-------|--------|
| `id` | installation id |
| `name` | display name |
| `kind` | `fort`, `port`, or `airport` (lowercase) |
| `faction_id` | owning faction |
| `province_id` | province |
| `center_x` / `center_z` | construct coords |

ProvinceSystem enriches `map_x` / `map_y` from `center_x` / `center_z` (1:1). Frontend renders fort/port/airport pins on political map modes.

### Fort ZOC export (`forts[]`)

Operational **forts only** (same set as `installations[]` where `kind == fort`). Map pins remain on `installations[]`; `forts[]` drives zone-of-control data and hatch overlays (step 43).

| Field | Notes |
|-------|--------|
| `id`, `name`, `faction_id`, `province_id` | Same as installation |
| `center_x` / `center_z` | Pin position |
| `zoc_provinces` | Sorted unique province ids — computed by `ZocRealm` |

**`zoc_provinces` rules:** fort province + one-ring **land** neighbors whose owner shares the **same top realm** (`RelationManager.getTopLiege` or faction id). Sea and unclaimed neighbors excluded.

**Active wars:** during a campaign war, who **controls** a fort's ZOC for siege gating may differ from installation owner (`fortControllers` on the war JSON). Siege winner becomes controller; installation DB ownership unchanged. **Map export is war-aware (shipped step 65.06):** when a fort is referenced on an active war and has a `fortControllers` entry, `zoc_provinces` is computed from that coalition's war leader realm; otherwise the installation owner is used. See [Wars.md](./Wars.md#campaign-battle-schedule-locked-step-64).

#### PS + frontend (step 43)

| Stage | Behaviour |
|-------|-----------|
| **PS `zocgen`** | Unions `zoc_provinces` pixel mask; tiles diagonal hatch → `output/{map}/zoc/{id}.png` + `defines/{map}/zoc_overlays.json` |
| **Triggers** | `map_markers` upload and `fullregen` |
| **API** | `GET /{map}/data/markers` → `forts[].overlay`, `zoc_url`, `map_x`/`map_y` |
| **Static** | `GET /{map}/zoc/{id}.png` |
| **Frontend** | Hover fort pin on political map modes → `hoveredFortZoc` hatch layer (separate from nation `hoveredOverlay`) |

Port and airport pins have no ZOC. Pending construction forts are excluded from `forts[]` (step 55).

---

## Config

```yaml
port-sea-proximity-blocks: 20   # construct validation: port must be within N blocks of sea/river

war:
  port_sea_zoc_radius: 2        # shipped step 65 — campaign naval blocking range (sea-hop BFS)

installations:
  fort:
    daily-upkeep: 50
    construction-time: 10  # 432000 (5 days)
    slots:
      static_emplacement: 8
  port:
    daily-upkeep: 20
    construction-time: 10  # 259200 (3 days)
    slots:
      ship: 10
  airport:
    daily-upkeep: 35
    construction-time: 10  # 259200 (3 days)
    slots:
      aircraft: 10
```

| Field | Kind | Dev value | Production (comment) |
|-------|------|-----------|----------------------|
| `daily-upkeep` | fort / port / airport | 50 / 20 / 35 denars per day | — |
| `construction-time` | fort | 10 seconds | 432000 (5 days) |
| `construction-time` | port / airport | 10 seconds | 259200 (3 days) |

Loaded at enable by `InstallationConfigLoader` (fail loud if missing). Access: `InstallationConfigLoader.getDailyUpkeep(kind)`, `getConstructionTimeSeconds(kind)`.

**Live servers:** merge `installations.*.daily-upkeep` and `construction-time` into `plugins/SimpleFactions/config.yml`, and add `icons: installations(black_dye.24)` for the hub tab icon.

`port-sea-proximity-blocks` is active (default 20). `installations.*.slots` are reserved for future VehicleFramework integration — unused in step 54/55.

---

## Package layout

```text
installation/
  Installation.java
  InstallationKind.java
  InstallationKindConfig.java
  InstallationConstruction.java
  handler/
    InstallationHandler.java
    ConstructResult.java
Loaders/
  InstallationConfigLoader.java
Managers/Inventory/
  InstallationView.java
  InstallationCreator.java
Database/
  InstallationData.java
  InstallationConstructionData.java
```

---

## Planning

ProvinceSystem step 54: [Planning/batches/step-54](../../ProvinceSystem/Planning/batches/step-54/00-index.md)

Step 55 (upkeep, construction queue, GUI): [Planning/batches/step-55](../../ProvinceSystem/Planning/batches/step-55/00-index.md) — **done**.

Step 43 (fort ZOC hatch overlay): [Planning/batches/step-43](../../ProvinceSystem/Planning/batches/step-43/00-index.md) — **done**.

Automated wars (planning lock): [Wars.md](./Wars.md) · PS [step-44](../../ProvinceSystem/Planning/batches/step-44/00-index.md).
