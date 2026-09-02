# Fertility crop growth - in-game verification

**Reference:** [fertility.md](./fertility.md) (formula, config, architecture)

Manual QA for province fertility crop growth on a staging server. None of this is covered by `mvn test`; the automated backstop is at the bottom.

---

## Prerequisites

- Staging world with `Input/province_id_grid.bin.gz` and `provinces.txt` loaded (see [province-grid.md](./province-grid.md)).
- `config.yml`: `enable-map: true`, `enable-provinces: true`.
- `fertility-crops.yml`: `enabled: true` (shipped default).
- **CustomCrops** installed; at least one test crop has `province-fertility` in every `grow-conditions.*.conditions` block (see [fertility.md](./fertility.md#customcrops-path)).
- Web map **Fertility** mode (or `provinces.txt`) to pick test plots at known fertility values: **0**, **~40**, and **100**.
- Use **random tick speed** default or slightly raised; growth checks need many random ticks, so allow **10+ minutes** per comparative test.

Suggested crops (weights from shipped yaml):

| Crop | Path | Weight | Use |
|------|------|--------|-----|
| Wheat | Vanilla | 0.90 | High sensitivity |
| Potato | Vanilla | 0.30 | Low sensitivity |
| Nether wart | Vanilla | 0.15 | Very tolerant |
| Tomato | CustomCrops | 0.75 | CustomCrops gate |
| Olive | CustomCrops | 0.30 | Low sensitivity custom |
| Rice | CustomCrops | 0.85 | High sensitivity custom |

Plant identical setups (hydrated farmland, light, same tick exposure) in the same province for fair comparison.

---

## Core growth curve

| # | Step | Expected |
|---|------|----------|
| 1 | Plant wheat, tomato, and nether wart in a **fertility 100** province. Wait for several vanilla random ticks / CustomCrops grow ticks. | All advance at roughly normal speed (fertility 100 always passes the roll). |
| 2 | Repeat in a **fertility 0** province. | **None** of the governed crops advance a stage over an extended wait. |
| 3 | In a **fertility ~40** province, plant wheat and potato side by side (same conditions). Observe over ~10 minutes. | Potato visibly outpaces wheat (lower weight = higher grow chance at the same fertility). |
| 4 | In the same ~40 province, plant rice/tomato (CustomCrops) vs olive (CustomCrops). | Olive outpaces rice/tomato when all have `province-fertility` in grow-conditions. |

---

## Edge cases and gates

| # | Step | Expected |
|---|------|----------|
| 5 | Plant governed crops on an **off-map / unmapped** block (grid returns province id 0). | Behaves as fertility **0** (no growth on governed crops). |
| 6 | Set `enable-provinces: false` in `config.yml`, reload plugin. Plant wheat in a previously fertility-0 province. | Governed crops grow normally (province lookup inactive). Restore `enable-provinces: true` after. |
| 7 | Set `fertility-crops.yml` `enabled: false`, reload configs. Plant wheat in fertility-0 province. | Grows normally. Restore `enabled: true` after. |
| 8 | Plant **cactus**, **bamboo**, or a **mushroom** in fertility-0 province. | Unaffected (excluded from yaml; not governed). |
| 9 | Plant a crop **not** listed in `fertility-crops.yml` (e.g. a flower) in fertility-0 province. | Unaffected. |

---

## CustomCrops integration

| # | Step | Expected |
|---|------|----------|
| 10 | Confirm test CustomCrops crop has `type: province-fertility` (or alias `simplefactions-fertility`) in grow-conditions. Plant in fertility-0 vs fertility-100 provinces. | Same pass/fail pattern as vanilla (0 blocks, 100 allows). |
| 11 | On one crop, add `value: 0.10` under the fertility condition while registry weight is higher (e.g. tomato 0.75). Test in fertility ~50 province vs a crop using registry weight only. | Override crop grows **more often** than registry-only crop at the same fertility. |
| 12 | Run `/customcrops reload`. Check server log for `[SimpleFactions] Registered CustomCrops requirement: province-fertility`. Re-test tomato in fertility-0 province. | Requirement still active; growth still blocked at fertility 0. |

---

## Out of scope for SimpleFactions

| # | Step | Expected |
|---|------|----------|
| 13 | **Bone meal** on governed crops in fertility-0 land. | SimpleFactions does **not** handle bone meal. If bone meal bypass exists, it comes from **tfmccore**; verify there separately. SF `BlockGrowEvent` cancel does not apply to bone meal-driven growth unless tfmccore routes it through the same path. |

---

## Regression sweep

1. `cd simplefactions && mvn test -Dtest="me.Plugins.SimpleFactions.Map.fertility.**,me.Plugins.SimpleFactions.Loaders.FertilityCropsLoaderTest"`
2. Confirm server starts with **CustomCrops absent**: no errors; vanilla fertility still works; no requirement registration log.
3. Confirm server starts with **CustomCrops present**: registration log on enable; reload still registers.

---

## Automated backstop

| Area | Test |
|------|------|
| Growth formula boundaries | `FertilityGrowthChanceTest` |
| Registry weights | `FertilityCropRegistryTest` |
| Province resolver (active flag, unmapped) | `FertilityProvinceResolverTest` |
| Vanilla cancel/allow | `FertilityVanillaGrowthTest` |
| Custom crop id + weight override | `FertilityCustomGrowthTest` |
| Yaml loader | `FertilityCropsLoaderTest` |
