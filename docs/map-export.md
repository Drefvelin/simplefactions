# Map export

SimpleFactions exports political map data to **ProvinceSystem** via the **TFMCWeb** gateway. The website reads uploaded JSON from the map data store; incremental regen redraws only changed provinces.

**ProvinceSystem side:** [integrations/simplefactions.md](../../ProvinceSystem/docs/integrations/simplefactions.md) · **Wars overlay:** [map/wars-on-map.md](../../ProvinceSystem/docs/map/wars-on-map.md) · **Schema:** [map-export-schema.json](../../ProvinceSystem/docs/assets/map-export-schema.json)

---

## Config

| Key | Role |
|-----|------|
| `enable-map` | Master switch; when false, uploads and regen are skipped |
| `map-reference` | Map id in upload/regen URLs (e.g. `main`, `dev`) |
| `api.base-url` | TFMCWeb gateway (softdepend config, not in `config.yml`) |

`Cache.mapRef` mirrors `map-reference` at load time. All upload paths use `/{mapRef}/data/upload/{mode}`.

---

## Upload modes

`RestServer.upload(mode, file)` POSTs JSON to TFMCWeb. `MapSystem` exports files under `plugins/SimpleFactions/MapAPI/` (and title JSON under `Input/`), then uploads:

| Mode | File | Payload |
|------|------|---------|
| `nation` | `MapAPI/nation.json` | Faction colours / borders |
| `province_data` | `MapAPI/province_data.json` | Per-province trade/prosperity |
| `guilds` | `MapAPI/guilds.json` | Guild markers |
| `map_markers` | `MapAPI/map_markers.json` | Settlements, installations, forts, wars |
| `county` / `duchy` / `kingdom` / `empire` | `Input/*.json` | De jure title trees |
| `queue` | `MapAPI/queue.json` | Incremental province/border change list |

`map_markers` validation (server-side before upload):

- Root must be a JSON object with a `settlements` array.
- If present, `installations` and `forts` must be arrays.

---

## Regen

| Trigger | Mechanism |
|---------|-----------|
| Incremental | `MapSystem.updateMap()` uploads `queue` + full payloads, then `RestServer.commenceRegen("queued")` |
| Full | `/faction fullregen <map>` or `MapSystem.fullRegen()` → `commenceRegen("full")` |
| Nation-only queue | `/faction regen` enqueues all nations |

Regen URL: `GET /{mapRef}/{REGEN_HASH}/api/regenerate/{queued|full}`. `REGEN_HASH` is currently hardcoded in `RestServer` (move to config before production; see [dev-config.md](./dev-config.md)).

---

## Tick cadence

`MapSystem.tick()`:

- Every **300 s** (if queue non-empty): incremental update.
- Every **3600 s**: queue all nations for a full nation upload cycle.

Faction state is saved before each upload/regen.

---

## `map_markers.json` shape

Built by `Markers.export()`:

| Top-level key | Source |
|---------------|--------|
| `map_id` | `Cache.mapRef` |
| `exported_at` | ISO-8601 instant |
| `settlement_large_population_threshold` | Config |
| `settlements[]` | Faction capitals + named settlements |
| `installations[]` | Operational forts, ports, airports |
| `forts[]` | Fort ZOC province lists (`zoc_provinces`, war-aware via `ZocRealm`) |
| `wars[]` | Active campaign wars only (`WarMapExporter`) |

Under-construction installations are **not** exported.

---

## `wars[]` route slice (shipped)

`WarMapExporter.exportWars()` includes active wars that have a non-empty `campaign_provinces` axis. **Raid wars** and wars with no axis are excluded.

Per-war object (snake_case):

| Field | Meaning |
|-------|---------|
| `id`, `name`, `war_type`, `goal`, `status` | War identity |
| `attacker_leader_id`, `defender_leader_id` | Leader faction ids |
| `belligerents[]` | All participating faction ids |
| `campaign_provinces[]` | Route polyline (province ids) |
| `cursor_index` | Campaign cursor on the line |
| `objective_province_id` | War goal province (optional) |
| `push_target` | Push/hold/counter state |
| `campaign_schedule_index`, `campaign_counter_schedule_index` | Active slot indices |
| `campaign_battle_schedule[]` | Invasion leg slots |
| `campaign_counter_schedule[]` | Counter-push leg (optional) |
| `attacker_capital`, `defender_capital` | `{ province_id, center_x?, center_z? }` |

Each schedule slot:

| Field | Meaning |
|-------|---------|
| `schedule_index`, `leg` | `invasion` or `counter` |
| `province_id`, `kind`, `kind_label`, `battle_type` | Battle placement |
| `required`, `status` | `fought` / `next` / `upcoming` |
| `display_name` | UI label for map pin hover |
| `fort_installation_id`, `port_installation_id` | Siege/naval anchor (nullable) |

**Not shipped yet:** `occupied_by_attacker[]` / `occupied_by_defender[]` occupation tint lists. See [roadmap.md](./roadmap.md).

ProvinceSystem enriches coordinates and renders the campaign line + battle pins on the web map. Details: [wars.md](./wars.md#web-map-campaign-visualization).

---

## Admin commands

| Command | Action |
|---------|--------|
| `/faction regen` | Queue all nations + upload |
| `/faction fullregen <map>` | Full map regen for map id |

After war schedule or installation changes, wait for the next `map_markers` upload or trigger regen manually so the website picks up new pins.

---

## Related docs

- [wars.md](./wars.md) - campaign system and map visualization rules
- [installations.md](./installations.md) - installation and fort ZOC export
- [province-grid.md](./province-grid.md) - local grid (not uploaded)
- [dev-config.md](./dev-config.md) - `map-reference: dev`, regen hash
