# Dev config and bypasses

Running list of **dev-only config, commented-out checks, and test bypasses** on the SimpleFactions test server. Revert or replace before production.

**War gameplay spec:** [wars.md](./wars.md) · **Map upload:** [map-export.md](./map-export.md)

---

## `config.yml` (dev server template)

Shipped default: `src/main/resources/config.yml`. Live server merges overrides into `plugins/SimpleFactions/config.yml`.

Campaign keys (`war.*`) are in **`war.yml`** (`src/main/resources/war.yml` → `plugins/SimpleFactions/war.yml`).

| Key | Dev value | Production target | Notes |
|-----|-----------|-------------------|-------|
| Header comment | `dev server template` | Remove or relabel | Documents intent only |
| `map-reference` | `main` on prod file; use `dev` on test | `main` (or live map id) | Drives TFMCWeb upload/regen paths |
| `enable-chronicle` | `true` | `true` | Chronicle snapshot upload. Set `false` to stop the chronicle without taking the map down; `enable-map` still gates it |
| `war.require_declare_code` (`war.yml`) | `false` | `true` | See RelationView bypass below |
| `war.battle_cadence.provinces_between_battles` (`war.yml`) | `3` | `3` (or higher after playtest) | Field battle cadence |
| `installations.*.construction-time` | `10` | `432000` fort / `259200` port+airport | Seconds |

**Tick model:** faction `tick()` runs **once per real second** (`FactionManager` timer every 20 ticks). Construction and regiment expansion `timeLeft` decrement **once per second**, so `10` = **10 seconds**, not 10 days.

---

## `regiments.yml`

| Regiment | Key | Dev value | Production target (comment in file) |
|----------|-----|-----------|-------------------------------------|
| professional | `expansion-time` | `10` | Loader fallback `21600` (6 h) if omitted |
| militia | `expansion-time` | `10` | `#43200` (12 h) in comment |
| levy | `expansion-time` | `0` | `#43200` in comment |

Queue ticks once per faction second (same as construction).

---

## `Guilds/upgrades.yml`

All three realm upgrades use `expansion-time: 10` (10 seconds). Production values not commented in file; treat as **dev-only** until locked.

| Upgrade id | Dev `expansion-time` |
|------------|----------------------|
| `max_admin_power` | 10 |
| `max_diplomatic_capacity` | 10 |
| `admin_power_gain` | 10 |

---

## Code bypasses (must revert)

| Location | What | Production action |
|----------|------|-------------------|
| `RelationView.java` ~L133 | **Commented block** skips declare pre-checks (code gate, opinion, duplicate war, target online) | Uncomment block; rely on `war.require_declare_code` + validators |
| `RestServer.java` L24 | `REGEN_HASH` hardcoded | Move to config / env |

---

## Admin / debug commands (intentional staff tools)

| Command | Purpose |
|---------|---------|
| `/war list` | Open war list GUI |
| `/war admin status <id>` | JSON war state (campaign, initiative, proposals) |
| `/war admin path <id>` | Regenerate campaign route |
| `/war admin end <id>` | Force-end a war (no goal apply, no reparations) |
| `/war admin win <id> attacker\|defender` | End war with victory outcome (goal apply or reparations) |
| `/war admin devmode on\|off\|status` | Volatile war devmode (roster fill + unrestricted raid launch) |
| `/war admin raid resetquota <id> [aggressor\|defender\|both]` | Clear daily raid quota for current battle day |
| `/faction fullregen <map>` | Trigger map regen via TFMCWeb |
| `/faction regen` | Queue nation upload + regen |

---

## Timing quirks (verify before prod)

| System | Actual tick | UI / docs say | Action |
|--------|-------------|---------------|--------|
| Administrative power gain | Every **10 s** (`FactionManager.timer % 10`) | Lore shows `+N/hour` | Confirm intended hourly rate |
| Faction daily cycle | `timer >= 86400` (~24 h real time) | Daily guild income, loans | Real-time days on test server |
| Diplomacy opinion drift | `RelationManager.tick` every **3600 s** | - | Real-time hour |
| Map partial upload | `MapSystem` full update every **3600 s** | - | Real-time hour |

---

## Wars - dev-friendly settings

| Item | Dev setting | Prod |
|------|-------------|------|
| Declare without Discord ticket | `war.require_declare_code: false` | Declare codes + ticket gate |
| Declare GUI pre-checks | Disabled in code (see above) | Re-enable with codes |

### Battle scheduling timeline (Europe/Paris)

Defaults under `war.battle_schedule`:

- **Vote open:** when next battle is pending (at declare or after battle end).
- **`defender_choice_deadline_hour` (12):** auto Push (winner) or Attack (loser after Hold) if unresolved.
- **`vote_close_hour` (16):** tally, schedule or postpone (`battleDay` +1, votes persist).
- **First battle day:** calendar day after declare.

**Dev surface (remove before prod):**

| Mechanism | Detail |
|-----------|--------|
| `/war admin schedule <id>` | Admin subcommands: `opencvote`, `closevote`, `skipday`, `castvote <hour> [attacker\|defender\|both]`, `forcequorum`, `setscheduled <iso>`, `battlecreate`, `battledelete`, `battlestart`, `winbattle attacker\|defender`, `choice push\|hold\|attack\|accept` (aliases: `battlechoice`, `defenderchoice`, …) |
| `war.battle_voting.dev_min_players: 1` | Test-server quorum override (default quorum uses `min_players: 4`) |
| Shortened hour keys | Test server may use e.g. 10/11/12/13 if order constraint holds |

---

## War dev mode

| Mechanism | Detail |
|-----------|--------|
| `/war admin devmode on\|off\|status` | Volatile in-memory toggle; resets on restart. Admin only. |
| `war.devmode.phantom_count` | Default 10 phantom UUIDs on manual `/warband create` or campaign `battlecreate` when devmode on |
| Raid QA | With devmode on, raid launch ignores battle day, raid hour window, and daily quota (mutex still applies). |
| `battle.capture_min_players: 1` | Min players at capture zone |
| Campaign join rules | Side membership check; roster cap = preview collective lives; bypass faction `WarbandSlot` for campaign battles |

**Solo staging workflow:**

1. `/war admin devmode on`
2. Declare war; open/close vote with `dev_min_players: 1`
3. `/warband create` or war GUI - phantoms in lore when devmode on
4. `/battle join <id> attacker` - wrong side rejected; roster cap enforced
5. Fight solo (`capture_min_players: 1`), verify lives/casualties/commitments
6. `/war admin devmode off`; restart clears devmode

---

## Campaign time dev mode

| Mechanism | Detail |
|-----------|--------|
| `/war admin time add 1h 31m` | Advance spoofed Paris schedule clock (compound tokens: `1h31m`, `1d`, `90s`) |
| `/war admin time reset` | Restore real time; resets hour gate and cancels raid Bukkit tasks |
| `/war admin time status` | Show offset, Paris date/hour, spoofed flag, active war count |
| `/war admin time skip-to-battle-day <warId>` | Jump to 00:00 Paris on war's `battleDay` (fixes `first_battle_day_after_declare` tomorrow default) |
| Route / schedule GUI | Gray **Starts in X** under **Next battle** on active route slot; schedule panel when `SCHEDULED` |

**Notes:**

- Admin only (`Permissions.isAdmin`); offset is volatile (lost on restart).
- Each mutating command runs `BattleScheduleTickService.onClockOffsetChanged()` then `tick(CampaignClock.now())` for immediate effect.
- Does **not** affect faction daily timer, guild income, or vehicle upkeep.
- `war admin schedule` subcommands remain for low-level overrides; prefer `war admin time` for schedule E2E.

**Typical workflow:**

1. Declare war (two factions).
2. `war admin time skip-to-battle-day <warId>` (or `add 1d` if battle day is tomorrow).
3. `add` into vote window; cast votes in GUI.
4. `add` past `vote_close_hour` - vote closes, phase `SCHEDULED`.
5. Confirm route shows **Starts in X**; `add` until fight starts.
6. Post-battle: `add` to `defender_choice_deadline_hour` for hold/push auto-resolve.
7. Raid window: `add` into raid hours; muster/fight via tick (no long real wait while spoofed).
8. `war admin time reset` when done.

---

## Campaign UX (test server E2E)

Prerequisites: `war.battle_voting.dev_min_players: 1`, optional `/war admin devmode on`.

**War setup (preferred):** declare → `war admin time skip-to-battle-day <id>` → vote in GUI → `war admin time add` past vote close → confirm **Starts in X** on route → `add` to fight time.

**War setup (manual override):** `war admin schedule <id> opencvote` → `castvote` → `forcequorum` → `closevote` (or `setscheduled` / `battlecreate`) when you need to bypass clock logic.

**Battle prep:** campaign warbands start empty until signup; staff `/battle edit` for spawns/jails/points; **siege:** Contest Area → click Min/Max at fort corners; `/warband join`. Optional: shorten `battle.signup_reminder_seconds_before` (e.g. `60`, `30`) to verify chat reminders before fight time.

**Fight loop:** `war admin time add` to fight time (tick runs on each add) or `war admin schedule <id> battlestart`; if siege contest is missing, countdown shows **Cannot start** and belligerents get a chat warning; casualties apply; war returns to **VOTING**.

**Cleanup:** `war admin time reset`; `/war admin devmode off`; battles/warbands persist on disk (`plugins/SimpleFactions/Battles/`, `Warbands/`).

### Strategic retreat (test server E2E)

Prerequisites: same as **Campaign UX** (`dev_min_players: 1`, optional `/war admin devmode on`).

1. Declare war; `war admin time skip-to-battle-day <id>`.
2. Open campaign GUI as **pushed** war leader during `VOTING` (defender on invasion push).
3. Confirm **Retreat** (slot 46) visible; pusher war leader does not see it.
4. Confirm retreat: route slot shows **Retreated**; schedule index and cursor advance; initiative fuel unchanged.
5. Hour votes still present; phase stays `VOTING`.
6. Second retreat in same window (if next slot exists): next slot **Retreated**; still `VOTING`.
7. Siege slot retreat: fort controller flips without a fought battle.
8. `war admin time add` past `vote_close_hour`: retreat button and hour toggles lock (GUI refreshes within ~1s).
9. Counter-push (`toward_aggressor_capital`): **attacker** (pushed) may retreat; counter schedule index advances.
10. `retake_objective` phase: retreat hidden / rejected.

See [planning/campaign-retreat/05-docs-verify.md](./planning/campaign-retreat/05-docs-verify.md) for the full manual matrix.

### Battle retreat (test server E2E)

Prerequisites: campaign battle running; set `battle.retreat_min_elapsed_seconds: 0` in config for fast testing (restore `1200` after).

1. Declare war; schedule and start a campaign field or siege battle (`war admin time` / `war admin schedule <id> battlestart`).
2. As **warband leader**: `/warband retreat` - confirm GUI with warning lore.
3. Cancel - battle continues.
4. Confirm - opponent wins; success message; campaign advances as a normal battle loss.
5. Take some deaths before retreat - verify partial casualties in `war admin status` / commitment rows.
6. Restore cooldown to `1200`; retry within 20 min - rejected with remaining-time message (no GUI).
7. Non-leader or non-warband player - rejected on command.
8. Campaign raid battle - rejected.

See [planning/battle-retreat/04-docs-verify.md](./planning/battle-retreat/04-docs-verify.md) for the full manual matrix.

---

## Related docs

- [roadmap.md](./roadmap.md) - what is still planned for production
- [wars.md](./wars.md) - full war spec
- [map-export.md](./map-export.md) - `map-reference` and regen hash
