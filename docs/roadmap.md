# Roadmap

## Next

Diplomacy polish is shipped. The chronicle snapshot is shipped; chronicle **events** remain owned elsewhere. Next is war companies (recruitment, dividends, mercenaries), then assassins. Declare codes last.

- **War companies** - planned as a six-phase program: [planning/war-companies/00-index.md](./planning/war-companies/00-index.md) (lock), [01-phases.md](./planning/war-companies/01-phases.md) (batches). Covers the army recruitment rule, guild dividends, mercenary companies, contracts, war participation, wages and reputation. Guild PvP stats land here as `GuildModifier` entries, not as a normal guild upgrade.
- **Assassins** - same hired-violence layer; reuses the war-companies contract object via its `ContractKind` discriminator
- **Map chronicle events** - other member; SF hooks for ProvinceSystem (`war_declared`, `battle_scheduled`, `battle_result`, `province_occupied`, `war_ended`)
- **Declare codes and ticket gate** - Discord ticket → staff code → in-game declare (production gate; last)

## Shipped

- Chronicle snapshot - `chronicle.json` uploaded every 300 s with per-faction wealth, prestige, rank and territory for season graphs ([map-export.md](./map-export.md), [planning/chronicle-export/00-index.md](./planning/chronicle-export/00-index.md)). Includes the prestige idempotency fix and `PrestigeRank` persistence.
- Guild ↔ faction GUI links; Friendly attitude used-cap; rival/hostile/unfriendly relative-prestige diplomatic capacity curve
- Council-forced white peace and surrender (political action on a chosen war; sticky offer or immediate surrender)
- NAP treaty overlay (diplomacy slot right of the nation icon; stacks with tributary; `blocks-war` declare block)
- Automated campaign wars (pathfinder, initiative, occupation bulge, battle scheduling)
- Inter-vassal wars (peer/cousin declare, CTA for all wars, liege transit, internal subjugate via `transferSubject`): [planning/inter-vassal-wars/00-index.md](./planning/inter-vassal-wars/00-index.md)
- War-goal declare and auto-apply through Phase 8 (navy gate, relation/title/law/pillage goals, movement apply gate, civil wars, inter-vassal)
- Civil wars (temp rebels, land split, untangle then apply; no auto reparations on defender win)
- Pillage war type (one-battle settlement; distinct from campaign raids)
- Warbands, military commitment, collective lives, casualty ledger
- Battle runtime (field, siege, raid templates), battle dev mode for staging
- Campaign time dev mode (`/war admin time`, route **Starts in** countdown)
- Strategic retreat during voting (concede slots without initiative cost; **Retreated** route lore)
- Mid-fight battle retreat (`/warband retreat` on started campaign field/siege battles; ledger casualties only)
- Campaign GUI live refresh (1s) and vote-close hour lock
- Campaign battle schedule, fort/port ZOC, naval invasions, dual-leg counter-push
- Installation transfer with province owner; wartime occupation then revert at peace
- War campaign map export: route line, battle pins, `occupied_by_*` on `wars[]`, `occupied_by` on `province_data`
- Installations (fort, port, airport), settlements, province grid
- Vehicle berths at installations, personal slot limits, battle vehicle eligibility
- Campaign installation picks, vehicle in-play, siege fort on schedule slot
- Campaign raids (inter-battle installation assaults)

Canonical war gameplay spec: [wars.md](./wars.md)

Scratch list: [TODO.md](../TODO.md).
