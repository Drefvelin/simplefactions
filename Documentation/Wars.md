# Wars — automated campaign system (planning lock)

> **Implementation status:** **Step 56 foundation shipped** (2026-08-19). **Step 57 pathfinder & campaign shipped** (2026-08-20). **Step 58 initiative & occupation shipped** (2026-08-20). **Step 59 battle scheduling shipped** (2026-08-20). **Step 60.09 campaign battle launch shipped** (2026-08-20). **Step 61.01 military & casualties planning lock** (2026-08-20); code batches **61.02–61.07** pending. Still planned: raid routes (**66**), map export (**67**), declare codes (**68**).
>
> **ProvinceSystem:** [step-44](../../ProvinceSystem/Planning/batches/step-44/00-index.md) (map occupation overlay) · [map-export-schema.json](../../ProvinceSystem/Planning/assets/map-export-schema.json)

---

## Why this exists

Previous-season wars were informal: players arranged fights in Discord, staff sometimes ruled outcomes, and the in-game war GUI was barely used. That caused drama and unfairness.

**v1 automated wars** are fully system-driven after staff approve a declaration ticket. Staff set battle **templates** (spawns, jails, capture points) only. Players vote on battle times, join auto-created battles, and the campaign advances on the map without manual organisation.

---

## Design principles

| Principle | Rule |
|-----------|------|
| **Automated** | Campaign route, schedule, progression, goal enforcement, and occupation are system-driven. |
| **Staff-light** | Staff maintain battle templates; ticket/code gate only in production (step 68). No mid-battle rulings. |
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

- **One campaign battle per day** (config) inside **battle window** (default **20:00–24:00 Zulu**, hourly slots).
- Exact hour chosen by [voting](#battle-scheduling--voting).
- **First campaign battle** is always at **`campaign_provinces[cursor_index]`** (border **B** at declare), regardless of **N**.
- **Later field battles:** every **N** provinces along the campaign line (config), plus mandatory battles at special nodes (objective capture/recapture, fort ZOC **siege** before advance, capital-as-target final battle). Battle **scheduling** and site pick → steps **59–63**; step **58** only applies outcomes after a battle is fought.
- Fort ZOC on line → **siege** before advance. Capital in fort ZOC → siege then final battle.

**Target feel:** ~**3 campaign battles** between major factions if attacker never loses; back-and-forth can use up to **~8** with initiative caps.

### Cursor movement (after each fought battle)

| Outcome | Cursor |
|---------|--------|
| Side currently **pushing toward objective** wins | **Forward** (+1 along line toward objective) |
| Pushing side **loses** | **Backward** (−1) |

Direction follows **who won last battle**, not static attacker/defender role.

### Initiative (locked)

| Rule | Default |
|------|---------|
| Each side starts with **N initiative** | **4** (config) |
| Each **fought** campaign battle consumes **1** from the **offensive** side (who is pushing toward objective that battle) |
| **Postponed** battle (low votes) | **No** initiative spent |
| Offensive side at **0 initiative** | Cannot launch a **new offensive** (cannot spend initiative to attack) |

### Attacker initiative exhausted — defender choice (not mandatory counter-attack)

When **attacker initiative = 0**, the **defender war leader** chooses (not auto-forced into a counter-offensive):

| Defender choice | Result |
|-----------------|--------|
| **White peace** | War ends. **No** reparations. |
| **Counter-push** | Optional offensive: fight **leftward** on the campaign axis toward **attacker capital** (line already includes capital direction). |
| **Hold** | **No** counter-push. War stays active on the current front. Attacker still cannot attack (0 initiative). Attacker must **accept** a pending white peace proposal or wait while the front sits (e.g. after a successful **siege defense**, defender is not forced into a field battle they would lose). If the attacker later regains no initiative (none in v1), stalemate resolves via [white peace proposals](#white-peace-proposals-locked). |

**Counter-push is never mandatory.** A defender who just won a costly siege may **hold** rather than counter-attack into unfavorable odds.

If the attacker still has initiative remaining, they may spend **1** to schedule another attack on the current front (e.g. **attack the fort again** after a failed siege). That is normal offensive spend, not the "attacker exhausted" branch above.

### White peace proposals (locked)

Replace blunt auto-end on unreachable counter-push with **proposal + accept**:

Each side tracks whether it has **auto-proposed white peace** when it **cannot reach its current capitulation target** with **remaining initiative**:

| Side | Capitulation target (for reach check) |
|------|-------------------------------------|
| Attacker (invasion / retake) | **Objective** province (or capital when capital is the war terminus) |
| Defender (counter-push phase) | **Attacker capital** |

**Reach check:** `steps_needed` = provinces along **`campaign_provinces[]`** from **`cursor_index`** toward that side's capitulation target; compare to that side's **remaining initiative**.

| Situation | Result |
|-----------|--------|
| One side cannot reach its target | That side **auto-proposes white peace** (flag on war record; visible in GUI / `warstatus`) |
| Other war leader **accepts** the proposal | **White peace** — war ends, **no** goal, **no** reparations |
| **Both** sides auto-propose (includes **both initiative = 0**) | **Automatic white peace** — no acceptance step |
| Voluntary mutual agreement | Leaders agree white peace anytime (step **62** UI/command) |

**Examples:**

- Defender chose counter-push but `steps_needed > initiative_defender` → defender auto-proposes; attacker may accept or continue if they still have initiative and a reachable target.
- Attacker at 0 initiative, defender **holds** (no counter-push) → if neither side can reach capitulation with remaining initiative, both flags → **automatic white peace**.
- Attacker needs **3** steps to objective, has **2** initiative → attacker auto-proposes; defender may accept (end) or **hold** and wait (defender is not forced to accept).

Persist proposal flags on the war record (e.g. `whitePeaceProposedByAttacker`, `whitePeaceProposedByDefender`); cleared if the strategic picture changes (implement batch defines when recalc runs — typically after each fought battle and phase change).

### Both sides initiative = 0

**Automatic white peace** via mutual auto-proposal (see above) — no goal, no reparations.

### Regional retake loop

1. Attacker wins at **objective** → objective held (attacker occupation).
2. Next battle: defenders **retake** at objective (defender offensive).
3. **Defenders win** → objective stays defender; cursor stays at objective; attackers must attack objective again.
4. **Attackers win retake attempt** → defenders push back: cursor **−1** on campaign line.

Capital as objective: capital battle won → **auto surrender** (no retake loop).

---

## Campaign GUI (locked)

**Planning lock:** [step-58/01-planning-lock.md](../../ProvinceSystem/Planning/batches/step-58/01-planning-lock.md)

Primary player surface for campaign line, next battle choice, white peace accept, and (step 59) battle hour voting. **GUI-first** — no player commands for hold, counter-push, or accept peace.

### Navigation

War list → War view → **Campaign** button → **Campaign view**.

### Route row

- Horizontal row: **attacker direction left**, **defender direction right**.
- One slot per province on **`campaign_provinces[]`** (paginate if long).
- **Cursor marker** on **`cursor_index`** (separate from block color).

### Concrete legend (viewer-relative)

| Material | Meaning |
|----------|---------|
| **Blue concrete** | Province owned by **your** belligerent coalition (main, subjects, called allies) |
| **Red concrete** | Province owned by **enemy** belligerent coalition |
| **Green concrete** | **Next battle** when only one valid option |
| **Yellow concrete** | **Choice** between two valid next battles (war leader click) |

**Not de jure.** **Not occupation bulge** for these colors — use **belligerent territorial ownership** (`ProvinceOwnerLookup` + war side membership). Neutral/unowned provinces on the line: **red** for both sides (v1).

**No gray.** **No white.** Every battle has a winner.

Green/yellow mark **action** slots; ownership remains visible in item lore.

### Leader interactions

| Situation | Campaign view |
|-----------|----------------|
| Attacker has initiative | Attacker leader sees **green** on next attack node |
| Attacker initiative = 0 | Defender leader sees **yellow** on current front (**hold**) and next node **left** (**counter-push**); confirm dialog |
| White peace proposed | Other war leader **Accept white peace** button |
| Both auto-propose | Automatic white peace |

Fort / objective / capital: lore tags; siege rules in step **63**.

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
| `defenderChoiceResolved` | Defender hold/counter choice locked (auto-Hold at deadline) |
| `forceQuorumNextClose` | Dev-only: next admin/tick close bypasses quorum ([DEV-SHORTCUTS.md](../../ProvinceSystem/Planning/DEV-SHORTCUTS.md)) |

### Runtime (59.06)

- **UTC scheduler:** `BattleScheduleTickService` polls every minute; processes each active `VOTING` war once per UTC hour change. At `defender_choice_deadline_hour` applies auto-Hold when needed; at `vote_close_hour` runs tally (schedule, postpone, or autoresolve). Persists on change.
- **Campaign battle launch (60.09):** On `SCHEDULED`, `CampaignBattleLaunchService` creates `campaign_w{id}_p{prov}` from campaign template, auto-rosters warbands, broadcasts `/battle join`. Tick starts fight at `scheduledBattleAt`. Autoresolve accept starts immediately. On `BattleEndedEvent`, progression + occupation apply and phase returns to `VOTING`. Regiment casualties apply in **61.06** (locked in **61.01**, not yet implemented).
- **Declare hook:** `WarManager.declareWar` -> `populateCampaign` -> `initScheduleState` sets `VOTING`, `battleDay`, empty votes.
- **Admin dev commands:** `/faction warschedule <warId> <subcommand>` (`opencvote`, `closevote`, `skipday`, `castvote`, `forcequorum`, `setscheduled`). Permission `simplefactions.admin`. Remove before prod.

---

## Battles & Warbands

### Province presence (central tracker)

SimpleFactions runs **one** province location poll for all online players every **1 second**. It fires **`PlayerProvinceEnterEvent`** / **`PlayerProvinceLeaveEvent`** when a player's province changes.

Battles (province-leave penalty), and future systems (ZOC, raids), **subscribe to these events** instead of running separate location scans.

Lookup: [`RestServer.getProvince`](./ProvinceGrid.md) → local `ProvinceGrid`.

### Merge Warbands into SimpleFactions

Same pattern as professions → RPCharacters. SF owns campaign battles, join flow, lives, and campaign linkage. Warbands battle engine (capture points, deaths, respawn) becomes an SF submodule.

### Battle modes (locked step 60)

| Mode | Win | Region | Respawns |
|------|-----|--------|----------|
| **Field** | Side eliminated when **lives = 0** and all online fighters are in **jail** (capture points gate spawn teleports only) | Battle **province** (+ naval add-on) | Campaign: collective lives from committed regiments (61.04). Staff manual: template lives |
| **Siege** | Hold **contest area** until timer hits **0** (ETW-style bidirectional timer); defender elimination also ends battle | Same as field | Same as field |
| **Raid** | Capture **target** to 100%; defender eliminated when `LIVES` mode exhausted | **No fence** - map-wide movement | Attackers: **none** (elimination on death/disconnect). Defenders: **infinite** or **set lives** (template) |

**Naval variant** (field + siege only): template flag adds the **adjacent sea province** to allowed set; **attacker spawn** on naval point. Campaign sea routing stays step **64**.

**Province-leave penalty** (field + siege): leave allowed province set → **10s countdown** → death → respawn at **side spawn**. Uses central province events. **Raids:** no leave penalty.

### Automatic vs manual battles

| Mode | Use |
|------|-----|
| **Campaign battle** | System-created from schedule; join via command; mode = field or siege from campaign context |
| **Raid battle** | Target settlement/province; raid war (66) or inter-battle raid (65) |
| **Manual battle** | Lore / staff events — **remains** for non-campaign fights |

### Staff template battles

From war GUI (staff): **template** per province — **spawns**, **jails**, **capture points** (field), **contest area** + duration (siege), **naval variant**, **defender respawn mode** (raid). Reused for campaign battles at that province.

### In-battle rules

- Leave allowed **provinces** (field/siege) → **10s countdown** → death → respawn at **side spawn** (central province tracker).
- Friendly fire / keep inventory per template config.
- **Raid attackers** do not respawn; fight until eliminated.
- **Raid defenders:** infinite respawns or finite lives per template.

### Manpower pool per battle (locked step 61.01)

Planning lock: [step-61/01-planning-lock.md](../../ProvinceSystem/Planning/batches/step-61/01-planning-lock.md) · levy detail: [step-61/01b-levy-vassal-lock.md](../../ProvinceSystem/Planning/batches/step-61/01b-levy-vassal-lock.md). Implementation batches **61.02–61.07**.

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

Today `WarManager.getCommitmentsForWar` returns real snapshot rows via `WarCommitmentService` (61.02). Re-commit is forbidden per own-regiment row. Levy rows use `sourceFactionId`. Commitments are in-memory until 61.06 persistence.

Militia eligibility is filtered at **battle pool** time, not at commit.

### Lives (collective, campaign field + siege)

Applies when `battle.warId != null` and type is **FIELD** or **SIEGE** at `battle.start()` (61.04). Staff manual battles (`warId == null`) and **raids** keep 60 template lives.

**Formula (per side):**

```text
sideLives = max(minSideLives, livesPerRegiment × committedRegiments − playersAtStart)
```

- `committedRegiments`: eligible pool total from battle pool resolver (61.03)
- `playersAtStart`: unique online fighters on that side at start
- `max_players ≤ lives`

Config under `war.battle_military`:

| Key | Default |
|-----|---------|
| `lives_per_regiment` | `5` |
| `min_side_lives` | `1` |

### Casualties (locked step 61.01)

**Ledger (61.05):** During campaign field/siege battles, track per-side casualties from deaths, disconnects after start, and province-leave penalty deaths. No ledger for staff manual or raid battles.

**Apply (61.06):** After battle via `CampaignBattleOutcomeService` (before `openVote`). Order: militia first (when eligible at battle province), then army + levies split **proportionally** across contributors. Debits `WarCommitment.count` and faction `Regiment.currentSlots` (permanent until rebuilt). Applies even when **no winner**.

**Out of scope for 61:** staff battles, goal apply (**62**), fort ZOC siege pick (**63**), raid war type (**66**).

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

### Campaign naval segments

Sea zones from province sea neighbours. Hostile **port** covering a sea zone blocks passage → **naval battle** required before land continues.

### Port protection

Port covers **up to 2 sea zones** away (config). Coastal forts (~20 blocks from coast, like ports) align with installation battle rules.

### Installation pick per battle

Attacker and defender each choose which **port / airport / fort** they use for that battle (cannot use all every time). **Blocking port** is mandatory for defender in naval gate battles. Airports used in attack can be bombed in inter-battle air raids.

Fort ZOC on campaign line → **siege battle** before advance. Capital inside fort ZOC → siege then final battle.

See [Installations.md](./Installations.md) for fort/port/airport pipeline. War-based ZOC suppression (`ZocRealm` TODO) ships with war rework.

---

## War end conditions

| Outcome | Trigger | Goal | Reparations |
|---------|---------|------|-------------|
| **Attacker win** | Defender surrenders, loses objective, loses capital when capital is target | Goal applied | No |
| **Defender win** | Attacker surrenders, loses capital | — | **Attacker pays** |
| **White peace** | Leader accept of auto-proposal, voluntary mutual agreement, or **both** sides auto-propose (unreachable capitulation / both initiative 0) | None | **No** |
| **Raid success** | Raid battle won at settlement | Pillage | No (unless attacker loses — N/A for one-shot raid) |

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
| **61** — Military & casualties | Lives, militia, levies, regiment losses (**61.01–61.02 done**; code 61.03+) | SF |
| **62** — End & goals | Surrender, white peace, goal apply, reparations | SF |
| **63** — Forts & sieges | ZOC gates on campaign line | SF |
| **64** — Naval & installations | Sea zones, port pick per battle | SF |
| **65** — Inter-battle raids | Naval/air/fort between battles | SF |
| **66** — Raid war type | One-battle border settlement raid | SF |
| **67** — Map export | `wars[]` occupation payload | SF |
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
- **N** provinces between campaign battles (after mandatory first battle at border)  
- **Occupation bulge** adjacency rule (which extra provinces per battle win)  
- Reparations **%** and **days** defaults  
- When to **recalculate** white peace auto-proposal flags after cursor / phase change  

These are config or implement-batch detail, not open design questions.
