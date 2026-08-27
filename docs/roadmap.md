# Roadmap

## Planned

- **War-goal auto-apply** - [planning/war-goals-apply/00-index.md](./planning/war-goals-apply/00-index.md), sequence [01-phases.md](./planning/war-goals-apply/01-phases.md) (navy done; no parallel diplomacy engine)
- **Pillage war type** - one-battle settlement pillage (distinct from campaign raids)
- **Full war map export** - occupation zones and chronicle event hooks for ProvinceSystem
- **Declare codes and ticket gate** - Discord ticket → staff code → in-game declare (production gate)
- **Website occupation tint** - blocked on SF occupation export; PS side: [ProvinceSystem/docs/map/wars-on-map.md](../../ProvinceSystem/docs/map/wars-on-map.md)

## Shipped

- Automated campaign wars (pathfinder, initiative, occupation, battle scheduling)
- Warbands, military commitment, collective lives, casualty ledger
- Battle runtime (field, siege, raid templates), battle dev mode for staging
- Campaign time dev mode (`/war admin time`, route **Starts in** countdown)
- Strategic retreat during voting (concede slots without initiative cost; **Retreated** route lore)
- Mid-fight battle retreat (`/warband retreat` on started campaign field/siege battles; ledger casualties only)
- Campaign GUI live refresh (1s) and vote-close hour lock
- Campaign battle schedule, fort/port ZOC, naval invasions, dual-leg counter-push
- War campaign map export (route line + battle pins via `wars[]`)
- Installations (fort, port, airport), settlements, province grid
- Vehicle berths at installations, personal slot limits, battle vehicle eligibility
- Campaign installation picks, vehicle in-play, siege fort on schedule slot
- Campaign raids (inter-battle installation assaults)

Canonical war gameplay spec: [wars.md](./wars.md)
