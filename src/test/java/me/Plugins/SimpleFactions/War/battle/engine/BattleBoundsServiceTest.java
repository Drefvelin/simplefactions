package me.Plugins.SimpleFactions.War.battle.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import me.Plugins.SimpleFactions.Map.ProvinceGrid;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.Managers.ProvinceManager;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;
import me.Plugins.SimpleFactions.enums.Terrain;

class BattleBoundsServiceTest {
	private ProvinceManager provinceManager;
	private SimpleFactions plugin;

	@BeforeEach
	void setUp() {
		provinceManager = new ProvinceManager();
		plugin = mock(SimpleFactions.class);
		when(plugin.getProvinceManager()).thenReturn(provinceManager);
	}

	@Test
	void resolve_usesExistingProvinceId() {
		Battle battle = fieldBattle("test");
		battle.setProvinceId(42);

		BattleBoundsService.resolveAllowedProvinces(battle, plugin);

		assertEquals(Set.of(42), battle.getAllowedProvinceIds());
	}

	@Test
	void resolve_fromDefenderSpawnWhenProvinceMissing() throws Exception {
		ProvinceGrid grid = gridWithProvince(10, 10, 5, 5, 77);
		when(plugin.getProvinceGrid()).thenReturn(grid);

		Battle battle = fieldBattle("test");
		BattleSide defender = battle.getSideById(BattleTemplate.DEFENDER_SIDE);
		defender.setSpawn(new Location(mock(World.class), 5, 64, 5));

		BattleBoundsService.resolveAllowedProvinces(battle, plugin);

		assertEquals(Integer.valueOf(77), battle.getProvinceId());
		assertTrue(battle.getAllowedProvinceIds().contains(77));
	}

	@Test
	void resolve_navalAddsAdjacentSeaProvince() throws Exception {
		Province land = province(10, Terrain.PLAINS);
		Province sea = province(11, Terrain.SEA);
		link(land, sea);
		load(land, sea);

		ProvinceGrid grid = gridWithProvince(10, 10, 1, 1, 10);
		when(plugin.getProvinceGrid()).thenReturn(grid);

		Battle battle = fieldBattle("naval");
		battle.setProvinceId(10);
		battle.setNavalVariant(true);

		BattleBoundsService.resolveAllowedProvinces(battle, plugin);

		assertEquals(Set.of(10, 11), battle.getAllowedProvinceIds());
	}

	@Test
	void raid_skippedByApplies() {
		Battle battle = fieldBattle("test");
		battle.setBattleType(BattleType.RAID);
		setStarted(battle, true);
		battle.setAllowedProvinceIds(Set.of(10));

		assertFalse(BattleBoundsService.applies(battle));
	}

	@Test
	void resolve_raidKeepsEmptyAllowedProvinces() {
		Battle battle = fieldBattle("test");
		battle.setBattleType(BattleType.RAID);
		battle.setProvinceId(42);

		BattleBoundsService.resolveAllowedProvinces(battle, plugin);

		assertEquals(Collections.emptySet(), battle.getAllowedProvinceIds());
	}

	@Test
	void isProvinceAllowed_falseForUnset() {
		Battle battle = fieldBattle("test");
		assertFalse(BattleBoundsService.isProvinceAllowed(battle, 10));
	}

	private Battle fieldBattle(String id) {
		BossBar bossBar = mock(BossBar.class);
		try (MockedStatic<org.bukkit.Bukkit> bukkit = Mockito.mockStatic(org.bukkit.Bukkit.class)) {
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
					Mockito.anyString(), Mockito.any(BarColor.class), Mockito.any(BarStyle.class)))
					.thenReturn(bossBar);
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
					Mockito.anyString(), Mockito.any(BarColor.class), Mockito.any(BarStyle.class), Mockito.any()))
					.thenReturn(bossBar);
			return BattleFactory.createBlank(BattleType.FIELD, id);
		}
	}

	private void setStarted(Battle battle, boolean started) {
		try {
			java.lang.reflect.Field field = Battle.class.getDeclaredField("started");
			field.setAccessible(true);
			field.setBoolean(battle, started);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private Province province(int id, Terrain terrain) {
		return new Province(id, terrain.name(), 50);
	}

	private void link(Province a, Province b) {
		a.addNeighbour(b.getId());
		b.addNeighbour(a.getId());
	}

	private void load(Province... provinces) {
		Map<Integer, Province> map = new HashMap<>();
		for (Province province : provinces) {
			map.put(province.getId(), province);
		}
		provinceManager.start(map);
	}

	private ProvinceGrid gridWithProvince(int width, int height, int x, int z, int provinceId) throws Exception {
		short[] ids = new short[width * height];
		ids[z * width + x] = (short) provinceId;
		Constructor<ProvinceGrid> constructor = ProvinceGrid.class.getDeclaredConstructor(int.class, int.class, short[].class);
		constructor.setAccessible(true);
		return constructor.newInstance(width, height, ids);
	}
}
