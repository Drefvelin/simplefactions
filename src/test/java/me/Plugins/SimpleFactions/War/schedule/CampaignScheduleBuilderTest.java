package me.Plugins.SimpleFactions.War.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Managers.ProvinceManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.CampaignBattleKind;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.progression.CampaignCoalition;
import me.Plugins.SimpleFactions.enums.Terrain;

class CampaignScheduleBuilderTest {
	private Faction attacker;
	private Faction defender;
	private MockedStatic<SimpleFactions> simpleFactions;
	private MockedStatic<TitleManager> titleManager;
	private ProvinceManager pm;

	@BeforeEach
	void setUp() {
		Cache.warProvincesBetweenBattles = 1;
		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");

		pm = new ProvinceManager();
		SimpleFactions plugin = mock(SimpleFactions.class);
		when(plugin.getProvinceManager()).thenReturn(pm);
		simpleFactions = mockStatic(SimpleFactions.class);
		simpleFactions.when(SimpleFactions::getInstance).thenReturn(plugin);
		titleManager = mockStatic(TitleManager.class);
	}

	@AfterEach
	void tearDown() {
		if (titleManager != null) {
			titleManager.close();
		}
		if (simpleFactions != null) {
			simpleFactions.close();
		}
	}

	@Test
	void build_cadenceOnly_marksObjectiveRequired() {
		List<Integer> axis = List.of(5, 10, 20, 30);
		War war = war();

		List<ScheduledCampaignBattle> schedule = CampaignScheduleBuilder.build(
				war,
				axis,
				2,
				3,
				FortZocIndex.fromForts(List.of()),
				PortSeaZocIndex.fromPorts(List.of()));

		assertEquals(2, schedule.size());
		assertEquals(20, schedule.get(0).provinceId());
		assertEquals(CampaignBattleKind.FIELD, schedule.get(0).kind());
		assertFalse(schedule.get(0).required());
		assertEquals(30, schedule.get(1).provinceId());
		assertTrue(schedule.get(1).required());
	}

	@Test
	void build_fortOnRoute_insertsSiegeBeforeFields() {
		setupMap(List.of(5, 10, 20, 30), 10, 20, 30);
		FortZocIndex index = FortZocIndex.fromForts(List.of(
				new OperationalFort("fort_a", defender, 20, 100L)));
		War war = war();
		war.putFortController("fort_a", CampaignCoalition.DEFENDER);

		List<ScheduledCampaignBattle> schedule = CampaignScheduleBuilder.build(
				war,
				List.of(5, 10, 20, 30),
				2,
				3,
				index,
				PortSeaZocIndex.fromPorts(List.of()));

		assertEquals(CampaignBattleKind.SIEGE, schedule.get(0).kind());
		assertEquals(20, schedule.get(0).provinceId());
		assertEquals("fort_a", schedule.get(0).fortInstallationId());
		assertTrue(schedule.stream().anyMatch(slot -> slot.provinceId() == 30 && slot.required()));
	}

	@Test
	void build_capitalInsideZoc_keepsSiegeAndRequiredObjectiveSeparate() {
		setupMap(List.of(5, 10, 21, 25), 10, 21, 25);
		Province p20 = province(20);
		Province p21 = province(21);
		link(p20, p21);
		pm.start(Map.of(
				5, province(5),
				10, province(10),
				20, p20,
				21, p21,
				25, province(25)));
		stubOwnership(10, attacker);
		stubOwnership(new int[] {20, 21, 25}, defender);

		FortZocIndex index = FortZocIndex.fromForts(List.of(
				new OperationalFort("fort_a", defender, 20, 100L)));
		War war = war();
		war.putFortController("fort_a", CampaignCoalition.DEFENDER);

		List<ScheduledCampaignBattle> schedule = CampaignScheduleBuilder.build(
				war,
				List.of(5, 10, 21, 25),
				2,
				3,
				index,
				PortSeaZocIndex.fromPorts(List.of()));

		assertEquals(CampaignBattleKind.SIEGE, schedule.get(0).kind());
		assertEquals(20, schedule.get(0).provinceId());
		ScheduledCampaignBattle last = schedule.get(schedule.size() - 1);
		assertEquals(25, last.provinceId());
		assertTrue(last.required());
	}

	@Test
	void build_attackerOwnedFort_skipsSiege() {
		setupMap(List.of(5, 10, 20, 30), 10, 20, 30);
		FortZocIndex index = FortZocIndex.fromForts(List.of(
				new OperationalFort("fort_a", attacker, 20, 100L)));
		War war = war();
		war.putFortController("fort_a", CampaignCoalition.AGGRESSOR);

		List<ScheduledCampaignBattle> schedule = CampaignScheduleBuilder.build(
				war,
				List.of(5, 10, 20, 30),
				2,
				3,
				index,
				PortSeaZocIndex.fromPorts(List.of()));

		assertTrue(schedule.stream().noneMatch(slot -> slot.kind() == CampaignBattleKind.SIEGE));
	}

	@Test
	void build_manuallyFlippedController_skipsSiege() {
		setupMap(List.of(5, 10, 20, 30), 10, 20, 30);
		FortZocIndex index = FortZocIndex.fromForts(List.of(
				new OperationalFort("fort_a", defender, 20, 100L)));
		War war = war();
		war.putFortController("fort_a", CampaignCoalition.AGGRESSOR);

		List<ScheduledCampaignBattle> schedule = CampaignScheduleBuilder.build(
				war,
				List.of(5, 10, 20, 30),
				2,
				3,
				index,
				PortSeaZocIndex.fromPorts(List.of()));

		assertTrue(schedule.stream().noneMatch(slot -> slot.kind() == CampaignBattleKind.SIEGE));
	}

	@Test
	void build_enemyPortBlocksSeaRun_insertsNaval() {
		Cache.warPortSeaZocRadius = 2;
		Province atkCoast = province(10);
		Province sea = seaProvince(11);
		Province defCoast = province(20);
		Province objective = province(30);
		link(atkCoast, sea);
		link(sea, defCoast);
		link(defCoast, objective);
		pm.start(Map.of(10, atkCoast, 11, sea, 20, defCoast, 30, objective));
		stubOwnership(10, attacker);
		stubOwnership(new int[] {20, 30}, defender);

		PortSeaZocIndex portIndex = PortSeaZocIndex.fromPorts(List.of(
				new OperationalPort("port_a", defender, 20, 100L)));
		War war = war();

		List<ScheduledCampaignBattle> schedule = CampaignScheduleBuilder.build(
				war,
				List.of(10, 11, 20, 30),
				0,
				3,
				FortZocIndex.fromForts(List.of()),
				portIndex);

		assertTrue(schedule.stream().anyMatch(slot ->
				slot.kind() == CampaignBattleKind.NAVAL
						&& slot.provinceId() == 20
						&& "port_a".equals(slot.portInstallationId())));
	}

	@Test
	void build_friendlyPortOnSeaRun_skipsNaval() {
		Cache.warPortSeaZocRadius = 2;
		Province atkCoast = province(10);
		Province sea = seaProvince(11);
		Province defCoast = province(20);
		Province objective = province(30);
		link(atkCoast, sea);
		link(sea, defCoast);
		link(defCoast, objective);
		pm.start(Map.of(10, atkCoast, 11, sea, 20, defCoast, 30, objective));
		stubOwnership(10, attacker);
		stubOwnership(new int[] {20, 30}, defender);

		PortSeaZocIndex portIndex = PortSeaZocIndex.fromPorts(List.of(
				new OperationalPort("port_a", attacker, 10, 100L)));
		War war = war();

		List<ScheduledCampaignBattle> schedule = CampaignScheduleBuilder.build(
				war,
				List.of(10, 11, 20, 30),
				0,
				3,
				FortZocIndex.fromForts(List.of()),
				portIndex);

		assertTrue(schedule.stream().noneMatch(slot -> slot.kind() == CampaignBattleKind.NAVAL));
	}

	@Test
	void build_seaRunWithoutPort_skipsNaval() {
		Province atkCoast = province(10);
		Province sea = seaProvince(11);
		Province defCoast = province(20);
		Province objective = province(30);
		link(atkCoast, sea);
		link(sea, defCoast);
		link(defCoast, objective);
		pm.start(Map.of(10, atkCoast, 11, sea, 20, defCoast, 30, objective));
		stubOwnership(10, attacker);
		stubOwnership(new int[] {20, 30}, defender);
		War war = war();

		List<ScheduledCampaignBattle> schedule = CampaignScheduleBuilder.build(
				war,
				List.of(10, 11, 20, 30),
				0,
				3,
				FortZocIndex.fromForts(List.of()),
				PortSeaZocIndex.fromPorts(List.of()));

		assertTrue(schedule.stream().noneMatch(slot -> slot.kind() == CampaignBattleKind.NAVAL));
	}

	@Test
	void build_seaCrossing_insertsNavalInvasionOnDefenderCoast() {
		Province atkCoast = province(10);
		Province sea = seaProvince(11);
		Province defCoast = province(20);
		Province objective = province(30);
		link(atkCoast, sea);
		link(sea, defCoast);
		link(defCoast, objective);
		pm.start(Map.of(10, atkCoast, 11, sea, 20, defCoast, 30, objective));
		stubOwnership(10, attacker);
		stubOwnership(new int[] {20, 30}, defender);
		War war = war();

		List<ScheduledCampaignBattle> schedule = CampaignScheduleBuilder.build(
				war,
				List.of(10, 11, 20, 30),
				0,
				3,
				FortZocIndex.fromForts(List.of()),
				PortSeaZocIndex.fromPorts(List.of()));

		assertTrue(schedule.stream().anyMatch(slot ->
				slot.kind() == CampaignBattleKind.NAVAL_INVASION
						&& slot.provinceId() == 20));
	}

	@Test
	void build_seaCrossing_skipsInvasionWhenExitCoastIsAttacker() {
		Province atkCoast = province(10);
		Province sea = seaProvince(11);
		Province atkBeachhead = province(12);
		Province defCoast = province(20);
		link(atkCoast, sea);
		link(sea, atkBeachhead);
		link(atkBeachhead, defCoast);
		pm.start(Map.of(10, atkCoast, 11, sea, 12, atkBeachhead, 20, defCoast));
		stubOwnership(new int[] {10, 12}, attacker);
		stubOwnership(20, defender);
		War war = war();

		List<ScheduledCampaignBattle> schedule = CampaignScheduleBuilder.build(
				war,
				List.of(10, 11, 12, 20),
				0,
				3,
				FortZocIndex.fromForts(List.of()),
				PortSeaZocIndex.fromPorts(List.of()));

		assertTrue(schedule.stream().anyMatch(slot ->
				slot.kind() == CampaignBattleKind.NAVAL_INVASION && slot.provinceId() == 20));
		assertTrue(schedule.stream().noneMatch(slot ->
				slot.kind() == CampaignBattleKind.NAVAL_INVASION && slot.provinceId() == 12));
	}

	@Test
	void build_seaCrossing_mergesInvasionOverFieldCadence() {
		Cache.warProvincesBetweenBattles = 1;
		Province atkCoast = province(10);
		Province sea = seaProvince(11);
		Province defCoast = province(20);
		Province objective = province(30);
		link(atkCoast, sea);
		link(sea, defCoast);
		link(defCoast, objective);
		pm.start(Map.of(10, atkCoast, 11, sea, 20, defCoast, 30, objective));
		stubOwnership(10, attacker);
		stubOwnership(new int[] {20, 30}, defender);
		War war = war();

		List<ScheduledCampaignBattle> schedule = CampaignScheduleBuilder.build(
				war,
				List.of(10, 11, 20, 30),
				2,
				3,
				FortZocIndex.fromForts(List.of()),
				PortSeaZocIndex.fromPorts(List.of()));

		long invasionAt20 = schedule.stream()
				.filter(slot -> slot.provinceId() == 20 && slot.kind() == CampaignBattleKind.NAVAL_INVASION)
				.count();
		long fieldAt20 = schedule.stream()
				.filter(slot -> slot.provinceId() == 20 && slot.kind() == CampaignBattleKind.FIELD)
				.count();
		assertEquals(1, invasionAt20);
		assertEquals(0, fieldAt20);
	}

	@Test
	void build_seaCrossing_landingInEnemyFortZoc_insertsSiegeNotInvasion() {
		Province atkCoast = province(10);
		Province sea = seaProvince(11);
		Province fortProvince = province(20);
		Province defCoast = province(21);
		Province objective = province(30);
		link(atkCoast, sea);
		link(sea, defCoast);
		link(fortProvince, defCoast);
		link(defCoast, objective);
		pm.start(Map.of(10, atkCoast, 11, sea, 20, fortProvince, 21, defCoast, 30, objective));
		stubOwnership(10, attacker);
		stubOwnership(new int[] {20, 21, 30}, defender);

		FortZocIndex fortIndex = FortZocIndex.fromForts(List.of(
				new OperationalFort("fort_a", defender, 20, 100L)));
		War war = war();
		war.putFortController("fort_a", CampaignCoalition.DEFENDER);

		List<ScheduledCampaignBattle> schedule = CampaignScheduleBuilder.build(
				war,
				List.of(10, 11, 21, 30),
				0,
				3,
				fortIndex,
				PortSeaZocIndex.fromPorts(List.of()));

		assertTrue(schedule.stream().anyMatch(slot ->
				slot.kind() == CampaignBattleKind.SIEGE
						&& slot.provinceId() == 20
						&& "fort_a".equals(slot.fortInstallationId())));
		assertTrue(schedule.stream().noneMatch(slot -> slot.kind() == CampaignBattleKind.NAVAL_INVASION));
	}

	@Test
	void build_seaCrossing_landingInFriendlyFortZoc_keepsInvasion() {
		Province atkCoast = province(10);
		Province sea = seaProvince(11);
		Province fortProvince = province(20);
		Province defCoast = province(21);
		Province objective = province(30);
		link(atkCoast, sea);
		link(sea, defCoast);
		link(fortProvince, defCoast);
		link(defCoast, objective);
		pm.start(Map.of(10, atkCoast, 11, sea, 20, fortProvince, 21, defCoast, 30, objective));
		stubOwnership(10, attacker);
		stubOwnership(new int[] {20, 21, 30}, defender);

		FortZocIndex fortIndex = FortZocIndex.fromForts(List.of(
				new OperationalFort("fort_a", defender, 20, 100L)));
		War war = war();
		war.putFortController("fort_a", CampaignCoalition.AGGRESSOR);

		List<ScheduledCampaignBattle> schedule = CampaignScheduleBuilder.build(
				war,
				List.of(10, 11, 21, 30),
				0,
				3,
				fortIndex,
				PortSeaZocIndex.fromPorts(List.of()));

		assertTrue(schedule.stream().anyMatch(slot ->
				slot.kind() == CampaignBattleKind.NAVAL_INVASION
						&& slot.provinceId() == 21));
		assertTrue(schedule.stream().noneMatch(slot -> slot.kind() == CampaignBattleKind.SIEGE));
	}

	@Test
	void build_landOnlyAxis_noNavalInvasion() {
		List<Integer> axis = List.of(5, 10, 20, 30);
		War war = war();

		List<ScheduledCampaignBattle> schedule = CampaignScheduleBuilder.build(
				war,
				axis,
				2,
				3,
				FortZocIndex.fromForts(List.of()),
				PortSeaZocIndex.fromPorts(List.of()));

		assertTrue(schedule.stream().noneMatch(slot -> slot.kind() == CampaignBattleKind.NAVAL_INVASION));
	}

	private War war() {
		War war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);
		return war;
	}

	private void setupMap(List<Integer> axisIds, int attackerProvince, int... defenderProvinces) {
		Map<Integer, Province> map = new java.util.HashMap<>();
		for (int id : axisIds) {
			map.put(id, province(id));
		}
		for (int i = 0; i < axisIds.size() - 1; i++) {
			link(map.get(axisIds.get(i)), map.get(axisIds.get(i + 1)));
		}
		pm.start(map);
		stubOwnership(attackerProvince, attacker);
		stubOwnership(defenderProvinces, defender);
	}

	private void stubOwnership(int provinceId, Faction owner) {
		titleManager.when(() -> TitleManager.getByProvince(provinceId)).thenReturn(owner);
	}

	private void stubOwnership(int[] provinceIds, Faction owner) {
		for (int provinceId : provinceIds) {
			stubOwnership(provinceId, owner);
		}
	}

	private Province province(int id) {
		return new Province(id, Terrain.PLAINS.name(), 50, id * 10, id * 10);
	}

	private Province seaProvince(int id) {
		return new Province(id, Terrain.SEA.name(), 50, id * 10, id * 10);
	}

	private void link(Province a, Province b) {
		a.addNeighbour(b.getId());
		b.addNeighbour(a.getId());
	}
}
