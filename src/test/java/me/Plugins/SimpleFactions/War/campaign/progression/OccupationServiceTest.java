package me.Plugins.SimpleFactions.War.campaign.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Managers.ProvinceManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.ObjectiveHolder;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;
import me.Plugins.SimpleFactions.War.pathfinder.BelligerentTerritory;
import me.Plugins.SimpleFactions.War.pathfinder.TitleManagerProvinceOwnerLookup;
import me.Plugins.SimpleFactions.enums.Terrain;

class OccupationServiceTest {
	private ProvinceManager pm;
	private OccupationService service;
	private Faction attacker;
	private Faction defender;

	@BeforeEach
	void setUp() {
		Cache.warOccupationIncludeEnemyNeighbors = true;
		pm = new ProvinceManager();
		service = new OccupationService(pm, new TitleManagerProvinceOwnerLookup());
		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");
	}

	@Test
	void computeOccupationZone_isolatedBattleProvince() {
		Province battle = province(10, Terrain.PLAINS);
		pm.start(Map.of(10, battle));

		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager, battle);
			War war = baseWar(List.of(10, 20, 30));
			assertEquals(List.of(10), service.computeOccupationZone(war, 10, BelligerentRole.ATTACKER).provinceIds());
		}
	}

	@Test
	void computeOccupationZone_includesCampaignLineNeighbor() {
		Province battle = province(10, Terrain.PLAINS);
		Province onLine = province(20, Terrain.PLAINS);
		Province offLine = province(99, Terrain.PLAINS);
		link(battle, onLine);
		link(battle, offLine);
		pm.start(Map.of(10, battle, 20, onLine, 99, offLine));

		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			titleManager.when(() -> TitleManager.getByProvince(10)).thenReturn(attacker);
			titleManager.when(() -> TitleManager.getByProvince(20)).thenReturn(defender);
			titleManager.when(() -> TitleManager.getByProvince(99)).thenReturn(attacker);
			War war = baseWar(List.of(5, 10, 20, 30));
			assertEquals(List.of(10, 20), service.computeOccupationZone(war, 10, BelligerentRole.ATTACKER).provinceIds());
		}
	}

	@Test
	void computeOccupationZone_includesExistingOccupationNeighbor() {
		Province battle = province(10, Terrain.PLAINS);
		Province occupiedNeighbor = province(99, Terrain.PLAINS);
		link(battle, occupiedNeighbor);
		pm.start(Map.of(10, battle, 99, occupiedNeighbor));

		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager, battle, occupiedNeighbor);
			War war = baseWar(List.of(5, 10, 30));
			war.setOccupiedByDefender(new ArrayList<>(List.of(99)));
			assertEquals(List.of(10, 99), service.computeOccupationZone(war, 10, BelligerentRole.ATTACKER).provinceIds());
		}
	}

	@Test
	void computeOccupationZone_includesEnemyNeighborWhenConfigEnabled() {
		Province battle = province(10, Terrain.PLAINS);
		Province enemy = province(99, Terrain.PLAINS);
		link(battle, enemy);
		pm.start(Map.of(10, battle, 99, enemy));
		Cache.warOccupationIncludeEnemyNeighbors = true;

		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			titleManager.when(() -> TitleManager.getByProvince(10)).thenReturn(attacker);
			titleManager.when(() -> TitleManager.getByProvince(99)).thenReturn(defender);
			War war = baseWar(List.of(5, 10, 30));
			assertEquals(List.of(10, 99), service.computeOccupationZone(war, 10, BelligerentRole.ATTACKER).provinceIds());
		}
	}

	@Test
	void computeOccupationZone_excludesEnemyNeighborWhenConfigDisabled() {
		Province battle = province(10, Terrain.PLAINS);
		Province enemy = province(99, Terrain.PLAINS);
		link(battle, enemy);
		pm.start(Map.of(10, battle, 99, enemy));
		Cache.warOccupationIncludeEnemyNeighbors = false;

		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			titleManager.when(() -> TitleManager.getByProvince(10)).thenReturn(attacker);
			titleManager.when(() -> TitleManager.getByProvince(99)).thenReturn(defender);
			War war = baseWar(List.of(5, 10, 30));
			assertEquals(List.of(10), service.computeOccupationZone(war, 10, BelligerentRole.ATTACKER).provinceIds());
		}
	}

	@Test
	void applyBattleWin_attacker_mergesIntoAttackerOccupation() {
		Province battle = province(10, Terrain.PLAINS);
		Province onLine = province(20, Terrain.PLAINS);
		link(battle, onLine);
		pm.start(Map.of(10, battle, 20, onLine));

		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager, battle, onLine);
			War war = baseWar(List.of(5, 10, 20, 30));
			assertTrue(service.applyBattleWin(war, 10, BelligerentRole.ATTACKER));
			assertEquals(List.of(10, 20), war.getOccupiedByAttacker());
			assertEquals(List.of(10, 20), war.getLastBattleOccupied());
			assertTrue(war.getOccupiedByDefender().isEmpty());
		}
	}

	@Test
	void applyBattleWin_defender_mergesIntoDefenderOccupation() {
		Province battle = province(20, Terrain.PLAINS);
		pm.start(Map.of(20, battle));

		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager, battle);
			War war = baseWar(List.of(5, 10, 20, 30));
			assertTrue(service.applyBattleWin(war, 20, BelligerentRole.DEFENDER));
			assertEquals(List.of(20), war.getOccupiedByDefender());
			assertEquals(List.of(20), war.getLastBattleOccupied());
			assertTrue(war.getOccupiedByAttacker().isEmpty());
		}
	}

	@Test
	void applyBattleWin_deduplicatesExistingOccupation() {
		Province battle = province(10, Terrain.PLAINS);
		pm.start(Map.of(10, battle));

		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager, battle);
			War war = baseWar(List.of(5, 10, 30));
			war.setOccupiedByAttacker(new ArrayList<>(List.of(10)));
			assertTrue(service.applyBattleWin(war, 10, BelligerentRole.ATTACKER));
			assertEquals(List.of(10), war.getOccupiedByAttacker());
			assertTrue(war.getLastBattleOccupied().isEmpty());
		}
	}

	@Test
	void applyBattleWin_recapture_stripsOtherSide() {
		Province battle = province(10, Terrain.PLAINS);
		pm.start(Map.of(10, battle));

		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager, battle);
			War war = baseWar(List.of(5, 10, 30));
			war.setOccupiedByDefender(new ArrayList<>(List.of(10)));
			assertTrue(service.applyBattleWin(war, 10, BelligerentRole.ATTACKER));
			assertEquals(List.of(10), war.getOccupiedByAttacker());
			assertTrue(war.getOccupiedByDefender().isEmpty());
			assertEquals(List.of(10), war.getLastBattleOccupied());
		}
	}

	@Test
	void mergeOccupation_returnsOnlyNewProvinces() {
		List<Integer> existing = new ArrayList<>(List.of(10));
		List<Integer> added = OccupationService.mergeOccupation(existing, OccupationZone.of(List.of(10, 20)));
		assertEquals(List.of(10, 20), existing);
		assertEquals(List.of(20), added);
	}

	@Test
	void qualifiesNeighbor_usesBelligerentTerritoryForEnemyOwnership() {
		Province battle = province(10, Terrain.PLAINS);
		Province enemy = province(99, Terrain.PLAINS);
		link(battle, enemy);
		pm.start(Map.of(10, battle, 99, enemy));

		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			titleManager.when(() -> TitleManager.getByProvince(10)).thenReturn(attacker);
			titleManager.when(() -> TitleManager.getByProvince(99)).thenReturn(defender);
			War war = baseWar(List.of(5, 10, 30));
			BelligerentTerritory territory = BelligerentTerritory.fromWar(war, new TitleManagerProvinceOwnerLookup());
			assertTrue(OccupationService.qualifiesNeighbor(war, 10, 99, BelligerentRole.ATTACKER, territory));
			assertFalse(OccupationService.qualifiesNeighbor(war, 10, 99, BelligerentRole.DEFENDER, territory));
		}
	}

	private War baseWar(List<Integer> axis) {
		War war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);
		war.setWarType(WarType.SUBJUGATE);
		war.setObjectiveProvinceId(30);
		war.setCampaignStartProvinceId(10);
		war.setCampaignProvinces(axis);
		war.setCursorIndex(1);
		war.setCampaignPhase(CampaignPhase.INVASION);
		war.setObjectiveHeldBy(ObjectiveHolder.DEFENDER);
		war.setOccupiedByAttacker(new ArrayList<>());
		war.setOccupiedByDefender(new ArrayList<>());
		war.setLastBattleOccupied(new ArrayList<>());
		return war;
	}

	private void stubOwnership(MockedStatic<TitleManager> titleManager, Province... provinces) {
		for (Province province : provinces) {
			if (province.getId() == 10 || province.getId() == 5) {
				titleManager.when(() -> TitleManager.getByProvince(province.getId())).thenReturn(attacker);
			} else {
				titleManager.when(() -> TitleManager.getByProvince(province.getId())).thenReturn(defender);
			}
		}
	}

	private Province province(int id, Terrain terrain) {
		return new Province(id, terrain.name(), 50, id * 10, id * 10);
	}

	private void link(Province a, Province b) {
		a.addNeighbour(b.getId());
		b.addNeighbour(a.getId());
	}
}
