# Wars — automated campaign system (planning lock)

> **Implementation status:** **Step 56 foundation shipped** (2026-08-19). **Step 57 pathfinder & campaign shipped** (2026-08-20). **Step 58 initiative & occupation shipped** (2026-08-20). **Step 59 battle scheduling shipped** (2026-08-20). **Step 60.09 campaign battle launch shipped** (2026-08-20). **Step 61 military & casualties shipped** (2026-08-21): war commitment snapshot, battle pool, collective lives, casualty ledger/apply. **Step 61b battle dev mode shipped** (2026-08-21): capture min 1, devmode phantoms, campaign join cap. **Step 61c campaign UX shipped** (2026-08-21): rules-only templates, formatted `warschedule` output, explicit warband signup, JSON battle/warband persistence (61c.09). **Step 63 war end closure shipped** (2026-08-22). **Step 64 campaign battle schedule & fort sieges shipped** (2026-08-23): schedule, fort control, trim/initiative, resolver/launch, GUI battle kinds, battle zone removal (lock: [64.01](../../ProvinceSystem/Planning/batches/step-64/01-planning-lock.md)). **Step 65 naval & invasions shipped** (2026-08-23): port sea ZOC naval slots, naval invasion, siege override on amphibious landing, campaign GUI naval kinds, `navalVariant` launch, war-aware fort ZOC export (lock: [65.01](../../ProvinceSystem/Planning/batches/step-65/01-planning-lock.md)). Still planned: inter-battle raids (**66**), raid war type (**66**), map export (**67**), declare codes (**68**).
>
> **ProvinceSystem:** [step-44](../../ProvinceSystem/Planning/batches/step-44/00-index.md) (map occupation overlay) · [map-export-schema.json](../../ProvinceSystem/Planning/assets/map-export-schema.json)

---

## Why this exists

Previous-season wars were informal: players arranged fights in Discord, staff sometimes ruled outcomes, and the in-game war GUI was barely used. That caused drama and unfairness.

**v1 automated wars** are fully system-driven after staff approve a declaration ticket. Staff set battle **rule presets** in templates (lives, friendly fire, keep inventory, durations). Staff place **battle geometry** (spawns, jails, capture points) per scheduled fight via `/battle edit`. Players vote on battle times, sign up via warband join, and the campaign advances on the map without manual organisation.

---

## Design principles

| Principle | Rule |
|-----------|------|
| **Automated** | Campaign route, schedule, progression, goal enforcement, and occupation are system-driven. |
| **Staff-light** | Staff maintain **rule presets** and place battle geometry per scheduled fight; ticket/code gate only in production (step 68). No mid-battle rulings. |
| **Transparent** | Campaign line, occupation zones, next battle, votes, and initiative visible in-game and on the web map. |
| **Wars encouraged** | Reparations are rare and attacker-only. White peace and initiative exhaustion avoid punishing failed wars too harshly. |
| **One goal** | One war = one war goal. No per-participant goal picking. |

---

## Declaration flow

### Production (step 68 — last)

1. Players open a **Discord ticket** (war type, target, goal, belligerents).
2. Staff review → approve → issue a **one-time declare code**.
3. In-game: diplomacy → **Declare War** → enter code.

**Deferred to step 68** so the war system can be built and tested without ticket/code friction first.

### Development / testing (steps 56–67)

- Declare war **directly in-game** from diplomacy GUI (same entry point as today).
- Config `war.require_declare_code: false` (default until step 68).
- All goal validation and FSM rules apply; no code check.

**Code properties (step 68):** one-time use, expiry, bound to attacker/defender/goal, audit log.

---

## War goals (locked)

Generic **conquest** is **not** a goal. The goal defines the political outcome.

| Goal | When available | On attacker win |
|------|----------------|-----------------|
| **`de_jure_annex`** | Target de jure region has **no settlements** (protects player builds) | Title/region annexed |
| **`subjugate`** | Target has settlements, or vassalization is intended | Target becomes subject |
| **`transfer_subject`** | Ticket specifies subject transfer between overlords | Subject transferred |

**De jure annex blocked** when any settlement exists in the target region → use **subjugate** or **transfer_subject**.

**One war = one goal**, chosen at declare (validated at declare time; code validation in step 68).

### De jure wars

- Attacker must already **partially control** the de jure title/region.
- **Rank gate:** may only target titles **at or below** own rank (kingdom → kingdom; county → county only).
- Victory is at a single **objective province** (see [Objective province](#objective-province)), not 100% province occupation.

---

## War types (campaign shape)

| Type | Campaign | Battles | End |
|------|----------|---------|-----|
| **De jure / subjugate / transfer** | Border → objective province (and capital push if counter-invasion) | Campaign battles on schedule | Goal applied or reparations / white peace |
| **Raid (war type)** | Shortest path border → **one settlement** within **X** provinces of border | **One** battle | Pillage + war ends (no return battle) |

**Raid distance:** settlements only within **X** provinces of attacker border (config). Deep raids require future airborne raids (out of v1 scope).

### Inter-battle raids (not a war type)

Between scheduled **campaign** battles, belligerents may run **tactical raids**:

| Kind | Target | Effect |
|------|--------|--------|
| Naval raid | Enemy **port** | Quick battle; damage gear / navy |
| Air raid | **Airfield** | Bombing; weaken installations |
| Fort raid | **Fort** | Soften before siege |

- Do **not** hold ground; not separate war types.
- Raid **war type** does **not** add permanent occupation tint (optional chronicle marker only).

---

## Participants

Keep from legacy system (repurpose):

- **Attacker / defender** sides with **main participants**
- **Subjects** auto-included on participant side
- **Allies** via call-to-arms (`/faction accept`, 60s timeout)
- **No switch sides in war GUI.** Subject independence / rebellion uses the **movement system**, not a war-view button (legacy switch removed 2026-08-20).

**Declined call to arms:** **-30% stability** (config), decays over time.

**Multiple wars** per faction are allowed in design; implement **one war FSM first**, tag all military commits with **`war_id`** from day one.

**War leaders** (main attacker / main defender faction leaders) decide surrender and white peace. Council-forced peace is **future** (proposal / movement / anti-war rebellion).

### Militia (locked)

| Rule | Detail |
|------|--------|
| Militia fights only on **that faction's direct land** | Province owner = faction |
| **Overlord militia** does **not** deploy in **vassal** territory | Overlord sends army + levies |
| Battle in **vassal land** | Vassal's **full** military including militia joins; overlord sends non-militia + levies |

---

## Campaign route generation

Uses a new **`ProvincePathfinder`** module (not embedded in `ProvinceManager` trade pipeline). Graph: province adjacency from `province_neighbors.json` + terrain from `provinces.txt`. Edge costs: terrain-weighted (plains preferred over mountains), same philosophy as trade `terrain-modifiers`.

**Planning lock:** [step-57/01-planning-lock.md](../../ProvinceSystem/Planning/batches/step-57/01-planning-lock.md)

### WATER vs SEA (locked)

| Terrain | Pathfinder rule |
|---------|-----------------|
| **WATER** (rivers, lakes) | Always crossable on **land passes** at normal terrain cost (~`0.75`). Routes may cross water so paths do not zigzag around rivers. |
| **SEA** (ocean) | **Impassable** on land passes 1 and 3. Only traversable in **pass 2** (naval/amphibious). |

**Note:** `Province.isSea()` returns true for both WATER and SEA. Pathfinder code must use `terrain == Terrain.SEA` for ocean logic, not `isSea()`.

### Neutral provinces (locked)

**Neutral** = province owner not in the war belligerent set (attacker side + defender side, including subjects and called allies).

| Pass | Neutral handling |
|------|------------------|
| **1** Land, no neutral | Blocked |
| **2** Sea, no neutral | Blocked |
| **3** Land + neutral | Allowed with heavy penalty (`war.pathfinder.neutral_penalty`, default `8.0`) |

### Route priority (locked)

Run passes **in order**; first pass that finds a route wins:

1. **Land campaign** - land + WATER; no SEA; no neutral  
2. **Sea campaign** - SEA hops between coastal belligerent provinces; no neutral  
3. **Land + neutral fallback** - same traversability as pass 1; neutrals penalized  

No sea-first routing; land is always preferred when possible.

**Future (not v1):** generate 2–3 alternative campaigns (e.g. long land vs risky naval); attacker chooses after declare.

### Step A — border start (conquest / de jure / subjugate)

**Intent:** shortest invasion corridor from **attacker–defender border** toward **objective province**, not capital-to-capital through messy borders.

1. For each province **B** on the **defender** side of the border (defender-owned adjacent to attacker-owned), pathfind **B → objective** using passes 1→2→3.
2. Pick **B** with minimum total cost → **`campaignStartProvinceId`** (first battle / invasion entry).
3. If no land border: use defender provinces adjacent to **sea** as **B** candidates; pathfind **B → objective** (sea landing on enemy soil).

### Step B — full campaign axis (locked step 58)

**Planning lock:** [step-58/01-planning-lock.md](../../ProvinceSystem/Planning/batches/step-58/01-planning-lock.md)

Step 57 shipped **`B → objective`** only. Step **58** replaces this with a **full axis** at declare / `warpath` regen:

```text
← ATTACKER   [ attacker capital … … atk border … B … … objective ]   DEFENDER →
                                              ↑
                                    cursor_index (first battle)
```

**B** is the first **defender-owned** province on the invasion route (first battle on enemy soil). The attacker-owned border province remains on the axis but is not **B**.

1. **Step A:** pick invasion entry **B** (defender-side; see above).
2. **Capital-closer rule:** if defender/subject **faction capital** is closer from **B** than the regional objective (path cost), **capital replaces** `objectiveProvinceId`; rebuild right segment.
3. **Left segment:** pathfind **attacker faction capital → B** (full path, always at declare).
4. **Right segment:** pathfind **B → objective** (inclusive).
5. **`campaign_provinces[]`** = merged left + right (**B** once).
6. **`cursor_index`** = index of **B** in the array (**middle**, not `0`).
7. **`campaignStartProvinceId`** = **B**.

**Counter-push:** defender fights **leftward** on the **existing** line toward attacker capital — no polyline append at choice time.

**Raid war type:** unchanged — step **66**.

### Raid war type route

Shortest path: attacker border → settlement (within X border distance). One battle at settlement. Implemented in step **66**, not step 57.

---

## Objective province (locked)

One province represents the de jure / vassal / regional target for capture and recake battles:

| Condition | Pick |
|-----------|------|
| Title/region **capital** in set | Capital province |
| Else **largest settlement** | Province with largest settlement; **capital settlement beats non-capital**; population/size tiebreaker |
| No settlements | **Geometric center** of title/region provinces |

All capture/recapture battles occur **at this province**. No multi-province occupation requirement to win.

When **capital itself** is the war target, capital province is the objective.

**Capital vs regional target (locked):** for subjugate / transfer / de jure wars, if the defender (or subject) **faction capital** is strictly **closer** from the campaign border start than the regional objective picked above, **faction capital replaces** the regional objective for both **`objectiveProvinceId`** and capture/recapture battles. De jure title capital in-set already wins via the table above; this rule covers cases where settlement/centroid pick would otherwise skip a nearer faction capital.

---

## Campaign progression

### Battle cadence

- **One campaign battle per day** (config) inside **battle window** (default **20:00–24:00 CET**, hourly slots; see step 59).
- Exact hour chosen by [voting](#battle-scheduling--voting).
- **First campaign battle** is always at **`campaign_provinces[cursor_index]`** (border **B** at declare), regardless of schedule slot order when indices align.
- **Full battle list** is built at declare (see [Campaign battle schedule](#campaign-battle-schedule-locked-step-64)), trimmed to per-goal `max_battles`, then fought in order via `campaignScheduleIndex`.
- Fort ZOC on line → **siege** when the line passes through the ZOC and the fort is enemy-controlled. Capital in fort ZOC → siege then objective field battle.

**Target feel:** default **4** battles per goal after trim; starting initiative fuel `ceil(4 × 1.5) = 6` with default `initiative_factor`. Back-and-forth spends fuel until exhaustion or victory.

### Campaign battle schedule (locked step 64)

**Planning lock:** [step-64/01-planning-lock.md](../../ProvinceSystem/Planning/batches/step-64/01-planning-lock.md) · [step-65/01-planning-lock.md](../../ProvinceSystem/Planning/batches/step-65/01-planning-lock.md) (naval)

At declare, after the campaign axis is set:

```text
natural  = CampaignScheduleBuilder.build(axis, forts, cadence)
trimmed  = CampaignScheduleTrimmer.trim(natural, max_battles[goal])
war.campaignBattleSchedule = trimmed
war.campaignScheduleIndex = 0
fuel     = ceil(trimmed.size() * initiative_factor)  // both coalitions
```

#### Two layers: template vs display

| Layer | Purpose | Values |
|-------|---------|--------|
| **`BattleType`** | Win rules (capture vs contest) | `FIELD`, `SIEGE` (no `BattleType.NAVAL`) |
| **`CampaignBattleKind`** | GUI label / staff setup expectations | `Field Battle`, `Siege`, `Naval Battle`, `Naval Invasion` |

Objective battles use **field** template and **Field Battle** display (required slot, never trimmed). Naval kinds use **`FIELD`** template plus **`navalVariant`** on the battle (staff sea spawn layout).

#### Natural slots (before trim)

| Slot | When | `BattleType` | Display |
|------|------|--------------|---------|
| **Siege** | Axis passes through province in operational fort ZOC; fort controller is enemy | `SIEGE` | Siege |
| **Naval** | Axis sea run blocked by enemy port sea ZOC | `FIELD` + `navalVariant` | Naval Battle |
| **Naval invasion** | First defender-owned land after sea run (unless fort ZOC override) | `FIELD` + `navalVariant` | Naval Invasion |
| **Field** | Every `provinces_between_battles` along axis | `FIELD` | Field Battle |
| **Objective** | Always at `objectiveProvinceId` | `FIELD` | Field Battle (`required`) |

Siege fires **once per fort** on the line (battle at fort province). Overlapping ZOC: **oldest** operational fort wins per province (`completedAt`, then id).

#### Port sea ZOC (shipped step 65)

Sea zones use **`Terrain.SEA`** only (not `Province.isSea()` / rivers). At declare, scan the axis for contiguous **SEA** runs (pathfinder pass 2).

| Rule | Detail |
|------|--------|
| **Port coverage** | BFS from **SEA neighbours of the port land province**; expand only across ocean tiles; radius **`war.port_sea_zoc_radius`** (default **2**) |
| **Blocking port** | Operational port whose owner coalition is **enemy of aggressor** at declare and whose coverage intersects any sea province in the run |
| **Naval slot** | One **`NAVAL`** per blocking port at the **port installation province** (`portInstallationId` on slot); friendly port covering the run → no naval slot |
| **No enemy port** | Sea on axis alone does **not** insert a naval slot |
| **Naval invasion** | First **defender-owned land** after the sea run → **`NAVAL_INVASION`**; landing in **enemy-controlled** fort ZOC → **`SIEGE`** at fort instead (no invasion slot) |
| **Overlap** | Oldest operational port wins per sea province (`completedAt`, then `id`) |

#### Trim priority (lowest cut first)

Objective (required) → siege → naval → naval invasion → field cadence. Within a tier, drop slots **farthest from objective** first (`CampaignScheduleTrimmer` ships naval kinds).

#### War-time fort control

Installation DB ownership does **not** change. `fortControllers` on the war tracks who **controls the ZOC**. Siege winner becomes controller. Counter-push through enemy-held ZOC **inserts a siege slot** at `campaignScheduleIndex` before the next battle resolves.

#### Progression

- `nextBattleProvince(war)` → `campaignBattleSchedule[campaignScheduleIndex].provinceId` (after re-siege insert check).
- Each fought campaign battle increments `campaignScheduleIndex` and `campaignBattlesFought`.
- `cursorIndex` still advances on winner **Push** per existing rules.

#### Persistence (new war JSON fields)

| Field | Role |
|-------|------|
| `campaignBattleSchedule` | Ordered trimmed slots |
| `campaignScheduleIndex` | Next slot index |
| `fortControllers` | installation id → coalition key |

Slot shape: `provinceId`, `kind`, `required`, optional `fortInstallationId` (siege), optional `portInstallationId` (naval).

### Cursor movement (after each fought battle)

Cursor moves only when the **battle winner chooses Push** after the battle. **Hold** keeps the cursor in place and auto-proposes white peace.

| Winner choice | Cursor |
|---------------|--------|
| **Push** | Advances along the current `pushTarget` (toward objective, toward aggressor capital, or retake objective) |
| **Hold** | Unchanged; white peace proposed to the loser |

### Initiative (locked; updated step 64)

| Rule | Default |
|------|---------|
| Starting fuel (both coalitions) | `ceil(trimmed_schedule.size() × initiative_factor)` |
| `initiative_factor` | **1.5** (config) |
| Per-goal `max_battles` | **4** each (`DE_JURE_ANNEX`, `SUBJUGATE`, `TRANSFER_SUBJECT`) |
| **`initiativeHolderCoalition`** | Which coalition may schedule the next campaign battle and is battle-offensive (starts **aggressor** at declare) |
| Each **fought** battle | **Battle offensive coalition** (holder at battle start) loses **1** fuel when the battle ends |
| **Winner** | Becomes initiative holder after post-battle choices resolve (unless Hold assigns attack to the loser) |
| **Postponed** battle (low votes) | **No** fuel spent; holder unchanged |
| Coalition at **0 fuel** while holding initiative | Cannot schedule until they win initiative back |

**Removed:** `war.initiative_per_side` fixed fuel at declare. Fuel is derived from **final** trimmed battle count only (re-siege inserts do not recompute fuel).

### Post-battle choice (every battle)

After **every** campaign battle, the **winner's war leader** chooses on the campaign view (or admin `battlechoice`):

| Winner choice | Result |
|---------------|--------|
| **Push** | Continue the offensive; cursor moves per `pushTarget`; voting reopens |
| **Hold** | Front held; winner auto-proposes white peace; **loser** chooses **Attack** or **Accept peace** |

**Loser response after Hold:**

| Loser choice | Result |
|--------------|--------|
| **Attack** | Loser gets initiative at the held front; voting reopens |
| **Accept peace** | White peace; war ends with no goal |

**Defaults at deadline:** winner **Push**; loser **Attack** after Hold.

**Mandatory Hold (moment C):** if the battle winner cannot field an offensive army at the **next** battle province after a Push (troops must be ready immediately), the winner is treated as having chosen **Hold** - the loser gets **Attack** / **Accept peace** without a Push/Hold prompt.

While `postBattleChoicePhase` is not `NONE`, **no** new battle may be scheduled (vote close blocked until choice or deadline).

### Declare gate (locked)

War declare is blocked unless the **declaring attacker faction** has at least **1 offensive manpower** from live military (`Military.getManpower(true)`). Regiment types count when marked `offense: true` in `regiments.yml` (levy or professional). No first-battle province or `canAttack()` check at declare.

### Battle offensive forfeit (locked)

At **scheduled battle time**, if the **battle offensive coalition** (initiative holder) cannot `canAttack()` at that province, they **forfeit** the battle: the opponent wins with no casualties, then normal post-battle choice rules apply. The same forfeit applies in the military walkover chain when the initiative holder cannot attack.

### White peace proposals (locked)

Symmetric reach checks per **coalition** via `CampaignCapabilityService.canReachTarget`:

| Coalition | Capitulation target (axis steps from cursor) |
|-----------|-----------------------------------------------|
| Aggressor | Objective province index |
| Defender | Aggressor capital index |

| Situation | Result |
|-----------|--------|
| One coalition cannot reach its target | That coalition **auto-proposes white peace** |
| Other war leader **accepts** | **White peace** - war ends, no goal, no reparations |
| **Both** coalitions auto-propose (includes **neither can attack**) | **Automatic white peace** |
| Neither coalition can mount next offensive (VOTING/SCHEDULED) | **Automatic white peace** (offensive stalemate) |
| Hold peace proposal active | Winner's coalition stays flagged until next battle ends |

Persist `whitePeaceProposedByAttacker` / `whitePeaceProposedByDefender` (coalition flags); recalc after each choice resolution and walkover chain.

### Both sides initiative = 0

**Automatic white peace** via mutual auto-proposal (see above) — no goal, no reparations.

### Regional retake loop

1. Attacker wins at **objective** → objective held (attacker occupation).
2. Next battle: defenders **retake** at objective (defender offensive).
3. **Defenders win** → objective stays defender; cursor stays at objective; attackers must attack objective again.
4. **Attackers win retake attempt** → **attacker victory** (war ends; no cursor rollback).

Capital as objective: capital battle won → **auto victory** (no retake loop). Symmetric rule: aggressor wins at **defender capital** → attacker victory; defender wins at **attacker capital** → defender victory. Failed retake at objective (attackers win while `retake_objective` is active) → attacker victory.

---

## Campaign GUI (locked)

**Planning lock:** [step-58/01-planning-lock.md](../../ProvinceSystem/Planning/batches/step-58/01-planning-lock.md)

Primary player surface for campaign line, post-battle Push/Hold choice, white peace accept, and battle hour voting. **GUI-first** for campaign choices.

### Navigation

War list → War view → **Campaign** button → **Campaign view**.

### Route row

- Horizontal row: **aggressor direction left**, **defender direction right**.
- One slot per province on **`campaign_provinces[]`** (paginate if long).
- **Cursor marker** on **`cursor_index`** (separate from block color).

### Concrete legend (viewer-relative)

| Material | Meaning |
|----------|---------|
| **Blue concrete** | Province owned by **your** belligerent coalition |
| **Red concrete** | Province owned by **enemy** belligerent coalition |
| **Green concrete** | **Next battle** when only one valid option |

**Not de jure.** Use **belligerent territorial ownership**. Neutral provinces on the line: **red** for both sides (v1).

### Leader interactions

| Situation | Campaign view |
|-----------|----------------|
| Winner choice pending | **Push** / **Hold** buttons (slots 40-41) |
| Loser response after Hold | **Attack** / **Accept white peace** buttons (slots 42-43) |
| War leader (no choice pending) | **Surrender** (slot 47) |
| Enemy white peace proposed | **Accept peace** (slot 48) when eligible |
| White peace proposed | Other war leader **Accept white peace** button |
| Both auto-propose | Automatic white peace |

Fort / objective / capital: lore tags; scheduled battle kind (**Field Battle** / **Siege**) on route provinces; siege provinces show enchant glint.

Voting hour toggles and schedule info: Campaign view slots **28-32** (hour multi-select), info book slot **4**, autoresolve propose slots **49-51** (step **59**, shipped 2026-08-20).

---

## Occupation map (locked)

Each **won campaign battle** adds explicit province(s) to the occupier's zone (**not** a single snake — creates a natural **bulge / front**).

| Field | Meaning |
|-------|---------|
| `occupied_by_attacker[]` | Province ids tinted attacker-held |
| `occupied_by_defender[]` | Province ids tinted defender-held |
| `objective_province_id` | Capture/recapture pin |
| `campaign_provinces[]` | Campaign polyline |
| `cursor_index` | Index into campaign line |
| `last_battle_occupied[]` | Provinces added by last battle (for chronicle / UI) |
| `whitePeaceProposedByAttacker` | Attacker auto-proposed white peace (unreachable capitulation) |
| `whitePeaceProposedByDefender` | Defender auto-proposed white peace |

**Website:** ProvinceSystem step-44 renders occupation tint from export. **Do not infer** occupation from territory diffs alone.

**Campaign GUI (in-game):** route block colors use **belligerent territorial ownership**, not de jure title claims and not `occupied_by_*` bulge lists. Bulge lists remain for web map export and chronicle.

**Per-battle rule (locked):** winning battle **occupies** the battle province and adjacent contested set per config (bulge front), exported after each battle.

---

## Battle scheduling & voting

> **Shipped:** steps [59.02-59.06](../../ProvinceSystem/Planning/batches/step-59/00-index.md) (2026-08-20). Locked in [59.01 planning lock](../../ProvinceSystem/Planning/batches/step-59/01-planning-lock.md).

All clock times **Zulu (UTC)**, configurable under `war.battle_schedule`:

| Key | Default | Role |
|-----|---------|------|
| `window_start_hour` / `window_end_hour` | 20 / 24 | Fightable hours on battle day |
| `vote_close_hour` | 16 | Hour vote tally on battle day |
| `defender_choice_deadline_hour` | 12 | Hold / counter-push / white peace deadline on battle day; no choice → auto **Hold** |

- **Vote open:** when a next battle is pending (declare or after prior battle end); battle province not required.
- **Vote close:** `vote_close_hour` on battle day → pick hour, postpone, or autoresolve.
- **First battle day:** calendar day **after** declare (voting may start at declare).
- Valid slots: one per full hour in the window (e.g. 20, 21, 22, 23, 24).
- **Eligible voters:** **online** members of participating factions (main + subjects + called allies on that side).
- Each player selects **all hours they can attend** (multi-select).

**Pick hour:** maximize `min(attacker_votes(H), defender_votes(H))`; tie → **earliest** hour.

### Quorum

Config under `war.battle_voting`:

| Key | Default | Role |
|-----|---------|------|
| `min_players` | 4 | Minimum distinct voters (any hour) |
| `require_smallest_side_full` | true | Smaller side must have 100% of **eligible members** represented |
| `pass_if_either` | true | Pass if **either** threshold met |
| `dev_min_players` | (optional) | Test-server override when key explicitly set; lower than `min_players` lowers quorum threshold. Remove before prod ([DEV-SHORTCUTS.md](../../ProvinceSystem/Planning/DEV-SHORTCUTS.md)). |

### Low turnout

| Situation | Resolution |
|-----------|------------|
| Quorum not met at `vote_close_hour` | **Postpone 1 battle day** (no initiative spent) |
| On postpone | `battleDay` +1; **votes persist**; stay in `VOTING` until next close |
| **Autoresolve** | Only if **both war leaders** agree (separate from white peace) |

### Persistence (59.02+)

| Field | Role |
|-------|------|
| `battleSchedulePhase` | `IDLE`, `VOTING`, `SCHEDULED`, `AUTORESOLVE_PENDING` |
| `battleDay` | UTC calendar day of current slot |
| `scheduledBattleAt` / `scheduledBattleHour` | Chosen fight time |
| `scheduledBattleProvinceId` | From `resolveNextBattleNodes` at vote close |
| `battleVotes` | UUID → selected hours |
| `autoresolveProposedByAttacker/Defender` | Dual-leader autoresolve flags |
| `postponementsThisCycle` | Debug counter |
| `postBattleChoicePhase` | `NONE`, `WINNER_PUSH_HOLD`, `LOSER_ATTACK_PEACE` |
| `postBattleChoiceResolved` | Choice locked (deadline defaults applied) |
| `initiativeHolderCoalition` | `aggressor` or `defender` coalition key |
| `pushTarget` | `toward_objective`, `toward_aggressor_capital`, `retake_objective` |
| `defenderChoiceResolved` | Legacy alias of `postBattleChoiceResolved` (v2 saves) |
| `forceQuorumNextClose` | Dev-only: next admin/tick close bypasses quorum ([DEV-SHORTCUTS.md](../../ProvinceSystem/Planning/DEV-SHORTCUTS.md)) |

### Runtime (59.06)

- **UTC scheduler:** `BattleScheduleTickService` polls every minute; at `defender_choice_deadline_hour` applies post-battle choice defaults; at `vote_close_hour` runs tally. Persists on change.
- **Campaign battle launch:** On `SCHEDULED`, `CampaignBattleLaunchService` creates campaign battle, enrolls warbands. On `BattleEndedEvent`, casualties apply, then `CampaignBattleOutcomeService` spends fuel, begins winner Push/Hold choice, and may chain military walkovers after choice resolves.
- **Admin dev commands:** `/faction warschedule <warId> battlechoice push|hold|attack|accept` (aliases: `defenderchoice`, `pushchoice`, `holdchoice`). Permission `simplefactions.admin`.

---

## Battles & Warbands

### Province presence (central tracker)

SimpleFactions runs **one** province location poll for all online players every **1 second**. It fires **`PlayerProvinceEnterEvent`** / **`PlayerProvinceLeaveEvent`** when a player's province changes.

Battles and future systems (ZOC, raids) **subscribe to these events** instead of running separate location scans. Province-leave battle penalty **removed** in step **64.08**.

Lookup: [`RestServer.getProvince`](./ProvinceGrid.md) → local `ProvinceGrid`.

### Merge Warbands into SimpleFactions

Same pattern as professions → RPCharacters. SF owns campaign battles, join flow, lives, and campaign linkage. Warbands battle engine (capture points, deaths, respawn) becomes an SF submodule.

### Battle modes (locked step 60; zones removed step 64.08)

| Mode | Win | Region | Respawns |
|------|-----|--------|----------|
| **Field** | Side eliminated when **lives = 0** and all online fighters are in **jail** (capture points gate spawn teleports only) | **No province fence** (64.08) | Campaign: collective lives from committed regiments (61.04). Staff manual: per-side lives in side edit GUI |
| **Siege** | Hold **contest area** until timer hits **0** (ETW-style bidirectional timer); defender elimination also ends battle | **No province fence** (64.08) | Same as field |
| **Raid** | Capture **target** to 100%; defender eliminated when `LIVES` mode exhausted | **No fence** - map-wide movement | Attackers: **none** (elimination on death/disconnect). Defenders: **infinite** or **set lives** (template) |

**Naval variant** (field + siege only): template flag for staff layout (**attacker spawn** on naval point). Campaign schedule inserts **`NAVAL`** / **`NAVAL_INVASION`** slots; launch sets **`navalVariant`** on field battles (step 65 shipped). Does not enforce province bounds after 64.08.

**Province-leave penalty:** **removed** step 64.08 (was: leave allowed set → 10s → death). Staff place spawns, jails, capture points, and contest areas anywhere on the map.

### Automatic vs manual battles

| Mode | Use |
|------|-----|
| **Campaign battle** | System-created from schedule; join via command; mode = field or siege from campaign context |
| **Raid battle** | Target settlement/province; raid war (66) or inter-battle raid (65) |
| **Manual battle** | Lore / staff events — **remains** for non-campaign fights. **One manual battle at a time** (61c.09); persisted to `plugins/SimpleFactions/Battles/`; delete via battle edit GUI (slot 22) or admin flow |

### Staff template battles (61c)

**YAML templates** (`battle-templates.yml`) apply **battle rules only**: lives, friendly fire, keep inventory, siege/raid durations, defender respawn mode, naval variant flag. They do **not** seed spawns, jails, or capture point coordinates.

For each **campaign battle**, staff use `/battle edit` (or battle GUI) to place spawns, jails, capture points (field), contest area + duration (siege), and naval spawn when applicable. Manual (non-campaign) battles from the war GUI reuse the same edit flow. **Fast edit:** open Sides, click a side, use Set spawn / Set jail / Add point at your feet (61c.10).

### Capture points enabled (`capture_points_enabled`)

Template YAML key (default `true` on `field_default`, `false` on siege/raid). When enabled:

- Battle edit slot 23 opens the points list
- Side Edit shows **Add point** (auto-names A, B, C per side)
- Capture point tick runs during the fight

When disabled, siege/raid use contest area or raid target UI on slot 23 instead.

### Battle & warband persistence (61c.09)

| Path | Content |
|------|---------|
| `plugins/SimpleFactions/Battles/battle_{id}.json` | Battle layout, rules, started state, side warband id references |
| `plugins/SimpleFactions/Warbands/warband_{id}.json` | Roster, leader, campaign shell fields (devmode dummy members not saved) |

- **Autosave:** every 60s and on plugin disable.
- **Resume:** `started=true` battles restore lives, capture progress, and contest timer; tick loop continues without re-teleport/title.
- **Manual limit:** only one battle with `warId == null`; `/battle create` blocked until deleted.
- **Orphan cleanup:** manual warbands not attached to any persisted battle are removed on save/disable.
- **Campaign end:** `CampaignBattleOutcomeService` deletes battle + campaign warband JSON when the war outcome resolves the fight.

### In-battle rules

- **No province-leave penalty** (removed 64.08). Movement is unrestricted during field/siege battles.
- Friendly fire / keep inventory per template config.
- **Raid attackers** do not respawn; fight until eliminated.
- **Raid defenders:** infinite respawns or finite lives per template.

### Battle dev mode and capture (61b + 61c, test server)

Solo staging on the test server: [DEV-SHORTCUTS.md](../../ProvinceSystem/Planning/DEV-SHORTCUTS.md). Full campaign E2E: [step-61c/05-docs-verify.md](../../ProvinceSystem/Planning/batches/step-61c/05-docs-verify.md).

| Topic | Detail |
|-------|--------|
| Capture threshold | `battle.capture_min_players` (default **1**). The side with strictly more players in the zone ticks capture. |
| Devmode | `/battle devmode on\|off\|status` (admin, volatile). **On:** fills every active campaign battle side warband with up to `phantom_count` dummy roster members (re-applies after restart). **Off:** strips dummy members from all warbands. Dummies are not persisted to disk. Manual `/warband create` also seeds dummies when devmode on. Dummies count toward roster display, lives subtraction, and join cap preview. Capture markers and capture presence still use online real players only. |
| Campaign join | When `battle.warId != null`: joining faction must be on the battle side; side roster capped by **pool lives** (`livesPerRegiment × committedRegiments`, pre-battle) or side lives (mid-battle join costs 1 life). `/battle join` redirects to `/warband list` signup. One auto warband per battle side. Player-facing errors: wrong side, roster full, no lives left, blocked rejoin after mid-battle leave. |

Config under `battle:`:

| Key | Default | Purpose |
|-----|---------|---------|
| `capture_min_players` | `1` | Min players at a capture zone |
| `devmode.phantom_count` | `10` | Dummy roster fill on manual warband create or campaign seed when devmode on |

### Manpower pool per battle (locked step 61.01)

Planning lock: [step-61/01-planning-lock.md](../../ProvinceSystem/Planning/batches/step-61/01-planning-lock.md) · levy detail: [step-61/01b-levy-vassal-lock.md](../../ProvinceSystem/Planning/batches/step-61/01b-levy-vassal-lock.md). Implementation batches **61.02–61.07 shipped** (2026-08-21).

Offense/defense regiment pools depend on **where the battle is fought** and **campaign phase**, not who declared war. Use `CampaignProgressionService.getOffensiveSide(war)` to determine which belligerent role is offensive; that side's factions use offensive regiments, the other side uses defensive regiments.

| Location (simplified) | Attacker-side factions | Defender-side factions |
|----------|------------------------|-------------------------|
| Inside **defender** territory (invasion push) | Offensive regiments | Defensive regiments |
| Inside **attacker** territory (counter-push) | Defensive regiments | Offensive regiments |

**Militia** (`militia` regiment): deploys only on **that faction's direct land** (`TitleManager.getByProvince(battleProvinceId) == faction`). Overlord militia does **not** deploy in vassal territory; vassal gets full military including militia on vassal land; overlord sends professional army + levies only.

### War commitment (`WarCommitment`, step 61.02+)

Minimal rules (locked 61.01 + 61.01b):

1. **Fighter OR levy-only, never both** on a war side. Fighters = main leaders, their **direct subjects**, and **called allies** (`BattleSideMembers.collectParticipatingFactions`). Nested vassals are levy-only.
2. **Fighter own regiments:** live slot count at each battle (mid-war buildup counts).
3. **Levy:** frozen rows `holder → source → count`. Snapshotted at **declare** and when an **ally joins**. Nearest **fighter** on the overlord chain is the holder (not the top overlord when a subject also fights).
4. Casualties always debit the **source** faction for levy rows.

**Levy mid-war:**

| Event | Effect |
|-------|--------|
| Ally joins | New levy rows for joiner only |
| Subject buildup / levy % change | No change |
| **New vassal** (of main, subject fighter, or ally) | No new rows |
| **Vassal bond breaks** | Remove rows for broken subject **and its subject subtree**; if a fighting subject leaves, remove all rows it held |

Today `WarManager.getCommitmentsForWar` returns snapshot rows via `WarCommitmentService` (61.02). Re-commit is forbidden per own-regiment row. Levy rows use `sourceFactionId`. Commitments persist on war JSON (`WarData.commitments`, 61.06) and reload on server start.

**Admin debug:** `/faction warstatus <warId>` prints one JSON line per war. Use `commitmentRows` to inspect per-faction rows (`factionId`, optional `sourceFactionId`, `regimentId`, `count`). Example own row: `{"factionId":"atk","regimentId":"militia","count":4}`. Example levy row: `{"factionId":"atk","sourceFactionId":"sub","regimentId":"levy","count":6}`. The `commitments` field is the row count (same as `commitmentRows.length`). After a campaign battle ends, re-run `warstatus` to confirm rows and counts decreased (61.06).

Militia eligibility is filtered at **battle pool** time, not at commit.

### Lives (collective, campaign field + siege)

Applies when `battle.warId != null` and type is **FIELD** or **SIEGE** at `battle.start()` (61.04). Campaign lives are **computed per side** from war commitment and roster size; the battle editor shows a read-only preview before start. `/battle setlives` is rejected on campaign battles. Staff manual battles (`warId == null`) configure **per-side lives** in the side edit GUI (FIELD/SIEGE); **raids** keep template defender lives.

**Formula (per side):**

```text
sideLives = max(minSideLives, livesPerRegiment × committedRegiments − rosterFighters)
```

- `committedRegiments`: eligible pool total from battle pool resolver (61.03)
- `rosterFighters`: unique warband roster members on that side (includes devmode dummies; offline real players included)
- Capture markers and capture presence still count **online real players only**
- Pre-battle join cap uses **pool lives** (committed regiments × lives per regiment); mid-battle join costs 1 life from the started side pool

Config under `war.battle_military`:

| Key | Default |
|-----|---------|
| `lives_per_regiment` | `5` |
| `min_side_lives` | `1` |

### Casualties (locked step 61.01)

**Ledger (61.05):** During campaign field/siege battles, track per-side casualties from deaths and disconnects after start. Province-leave penalty deaths **removed** with 64.08. No ledger for staff manual or raid battles.

**Apply (61.06):** After battle via `CampaignBattleOutcomeService` (before `openVote`). Implemented in `BattleCasualtyService`: militia first (when eligible at battle province), then army + levies split **proportionally** across contributors. Debits `WarCommitment.count` and faction `Regiment.currentSlots` (permanent until rebuilt). Applies even when **no winner**. Side casualties are snapshotted on `BattleEndedEvent` before the ledger clears.

**Out of scope for 61:** staff battles, goal apply (**62**), campaign battle schedule / fort sieges (**64**), raid war type (**66**).

### Levies (war-scoped)

- Frozen integer pool per `(holder, source)` row; snapshotted at **declare** and on **ally join** only.
- **Holder** = nearest participating fighter walking up from source (avoids double count when main and subject both fight).
- **Source** = levy-only faction whose troops and casualties are tracked; can be any depth in the vassal chain.
- **No** mid-war add from subject troop buildup, levy % changes, or **becoming** a new vassal (of main, subject fighter, or ally).
- **Yes** mid-war **remove** when a subject stops being a vassal: drop that source and **all its subject descendants**; drop holder rows if a fighting subject leaves the side.
- **transferSubject** mid-war: remove old subtree only; no snapshot for new overlord.
- Fighter on the war side never also appears as levy from the same side (no double count).
- All commits tagged **`war_id`**. Losses decrement committed levy and source faction sent counts.

---

## Naval & installations

**Planning lock:** [step-65/01-planning-lock.md](../../ProvinceSystem/Planning/batches/step-65/01-planning-lock.md)

### Campaign naval segments (shipped step 65)

Full rules: [Port sea ZOC](#port-sea-zoc-shipped-step-65) under campaign battle schedule.

- Enemy **port** sea ZOC blocking an axis sea run → **`NAVAL`** schedule slot at the port province (`navalVariant` field battle).
- First **defender-owned land** after a sea run → **`NAVAL_INVASION`** (or **siege** if landing province is in enemy-controlled fort ZOC).
- Sea on axis without an enemy blocking port → no naval slot.

### Port protection

`war.port_sea_zoc_radius` (default **2**): sea-hop BFS from the port's adjacent ocean tiles. Distinct from `port-sea-proximity-blocks` (construction validation only). See [Installations.md](./Installations.md#config).

### Installation pick per battle

**Deferred** (not in step 65 scope). Schedule slots carry `portInstallationId` for the **blocking** port only. Full attacker/defender port / airport / fort pick UI may ship later or in step 66 prep. Airports used in attack can be bombed in inter-battle air raids (step 66).

Fort ZOC on campaign line → **siege** when line passes through ZOC and fort is enemy-controlled. War-time fort controller may differ from installation owner; see [Campaign battle schedule](#campaign-battle-schedule-locked-step-64). Capital inside fort ZOC → siege then objective field battle.

See [Installations.md](./Installations.md) for fort/port/airport pipeline. War-aware ZOC on map export (`ZocRealm` controller filter) **shipped** step 65; see [Installations.md](./Installations.md#fort-zoc-export-forts).

---

## War end conditions

| Outcome | Trigger | Goal | Reparations |
|---------|---------|------|-------------|
| **Attacker victory** | Aggressor wins battle at defender capital, failed objective retake, or defender leader **surrenders** (slot 47) | Goal apply (future) | No |
| **Defender victory** | Defender wins battle at attacker capital, or attacker leader **surrenders** (slot 47) | — | **Attacker pays** (future) |
| **White peace** | Leader accept of auto-proposal, voluntary mutual agreement, mutual exhaustion auto-proposal, or offensive stalemate | None | **No** |
| **Raid success** | Raid battle won at settlement | Pillage | No (unless attacker loses — N/A for one-shot raid) |

### `WarEndReason` values (shipped step 63)

| Value | Meaning |
|-------|---------|
| `white_peace` | No winner; no goal |
| `attacker_victory` | Attacker coalition wins |
| `defender_victory` | Defender coalition wins |
| `admin_end` | Staff / command end |

Opening the campaign view **recalculates** white peace proposal flags only; it does **not** auto-end the war.

### War reparations (attacker-only)

**Only when attacker loses badly:**

- Attacker **surrenders**, or
- Attacker **loses capital** (defender counter-push and wins there)

**Not when:** defender loses (land/subject loss is enough), any **white peace** (including accepted auto-proposal), initiative exhaustion white peace.

**Mechanic:** flat **% of main guild ledger income** for **X days** paid to winner (e.g. 25% tax). Source: **main faction guild ledger only** — not subsidiary guilds. Applied like other taxes via ledger pipeline.

---

## Map export contract

Exported in `map_markers.json` (or sidecar) per [`map-export-schema.json`](../../ProvinceSystem/Planning/assets/map-export-schema.json).

Emit on declare, after each battle, and on war end. Chronicle events: `war_declared`, `battle_scheduled`, `battle_result`, `province_occupied`, `war_ended`.

---

## Build steps (locked)

Full step list and dependencies: [ProvinceSystem/Planning/war-build-order.md](../../ProvinceSystem/Planning/war-build-order.md).

| Step | Scope | Repos |
|------|--------|-------|
| **56** — Foundation **done** | War v2, goals, FSM, persistence, test declare (no code), participants, `war_id` stubs | SF |
| **57** — Pathfinder & campaign **done** | `ProvincePathfinder`, border start, route, objective province | SF |
| **58** — Initiative & occupation **done** | Cursor, initiative, occupation bulge, counter-push | SF |
| **59** — Scheduling **done** | Battle window, voting, postpone, scheduler, `warschedule` dev | SF |
| **60** — Warbands & battles **done** | Province tracker, field/siege/raid, templates, schedule hook, auto-join | SF |
| **61** — Military & casualties **done** | Commit snapshot, battle pool, collective lives, casualty ledger/apply (61.02–61.07) | SF |
| **61b** — Battle dev mode **done** | Solo staging: devmode, capture min, campaign join cap | SF |
| **61c** — Campaign UX **done** | Rules-only templates, formatted `warschedule`, warband signup, battle/warband JSON persistence (61c.09), side fast edit GUI (61c.10) | SF |
| **62** — Campaign capability **done** | Reach checks, walkovers, declare gate | SF |
| **63** — War end closure **done** | `WarResolutionService`, surrender GUI, capital auto-victory, stalemate peace | SF |
| **64** — Campaign schedule & fort sieges **done** | Battle list at declare, fort ZOC sieges, war-time fort control, trim, initiative from schedule, GUI battle kinds, remove battle zones | SF |
| **65** — Naval & invasions **done** | Port ZOC naval battles, naval invasion slots, siege override, GUI naval kinds, war ZOC export | SF |
| **66** — Inter-battle raids **planned** | Naval/air/fort between battles | SF |
| **67** — Map export **planned** | `wars[]` occupation payload | SF |
| **44** — Map layer | Occupation tint on website | PS |
| **68** — Declare codes | Ticket → code production gate | SF |

---

## Legacy system (to replace)

Current `main` branch war code is a **partial MVP** — GUI, sides, goals display, Warbands muster, JSON save on disable only. **Remove or repurpose:**

| Keep / repurpose | Remove / replace |
|------------------|------------------|
| `War`, `Side`, `Participant`, ally calls | Per-participant war goals UI |
| War list / view GUI shell | Manual declare without code |
| `wargoals.yml` IDs + `canTarget()` | Goal resolution (new engine) |
| `WarManager` registry pattern | Save-only-on-disable persistence |

See git history and `War/` package for legacy files.

---

## Related documentation

| Doc | Topic |
|-----|--------|
| [Installations.md](./Installations.md) | Forts, ports, airports, ZOC |
| [Settlements.md](./Settlements.md) | Settlement provinces (de jure annex block) |
| [ProvinceGrid.md](./ProvinceGrid.md) | Province ids and neighbours |
| [war-build-order.md](../../ProvinceSystem/Planning/war-build-order.md) | Locked step list 56–68 |
| [16-map-platform.md](../../ProvinceSystem/Planning/16-map-platform.md) | Map platform requirement #10 |

---

## Open items for implement batches

- Exact **X** provinces for raid war border distance  
- **Occupation bulge** adjacency rule (which extra provinces per battle win)  
- Reparations **%** and **days** defaults  
- When to **recalculate** white peace auto-proposal flags after cursor / phase change  

`provinces_between_battles`, `max_battles`, and `initiative_factor` are locked in step **64** (see config schema in [64.01](../../ProvinceSystem/Planning/batches/step-64/01-planning-lock.md)).
