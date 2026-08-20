package me.Plugins.SimpleFactions.War.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
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
import me.Plugins.SimpleFactions.War.Side;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.ObjectiveHolder;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;
import me.Plugins.SimpleFactions.enums.Terrain;
import me.Plugins.SimpleFactions.settlement.handler.SettlementHandler;

class WarCampaignServiceTest {
	private ProvinceManager pm;
	private WarCampaignService service;
	private Faction attacker;
	private Faction defender;

	@BeforeEach
	void setUp() {
		Cache.tradeCarry.clear();
		Cache.tradeCarry.put(Terrain.PLAINS, 0.85);
		Cache.warPathfinderNeutralPenalty = 8.0;
		Cache.warPathfinderSeaPassEnabled = true;
		Cache.warPathfinderWaterCost = 0.0;
		Cache.warInitiativePerSide = 4;
		Cache.warFirstBattleDayAfterDeclare = true;

		pm = new ProvinceManager();
		service = new WarCampaignService(pm);

		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");
		when(attacker.getCapital()).thenReturn(5);
		when(defender.getCapital()).thenReturn(30);

		SettlementHandler handler = mock(SettlementHandler.class);
		when(defender.getSettlementHandler()).thenReturn(handler);
		when(handler.getAll()).thenReturn(List.of());
	}

	@Test
	void populateCampaign_buildsFullAxisWithCursorAtBorder() {
		Province atkCapital = province(5, Terrain.PLAINS);
		Province border = province(10, Terrain.PLAINS);
		Province mid = province(20, Terrain.PLAINS);
		Province objective = province(30, Terrain.PLAINS);
		link(atkCapital, border);
		link(border, mid);
		link(mid, objective);
		pm.start(Map.of(5, atkCapital, 10, border, 20, mid, 30, objective));

		War war = new War(
				1,
				new Side(attacker),
				new Side(defender),
				WarGoalType.SUBJUGATE,
				WarType.SUBJUGATE,
				null,
				null,
				Instant.parse("2026-08-19T12:00:00Z"));

		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager, border, mid, objective);

			assertTrue(service.populateCampaign(war));
			assertEquals(30, war.getObjectiveProvinceId());
			assertEquals(20, war.getCampaignStartProvinceId());
			assertEquals(List.of(5, 10, 20, 30), war.getCampaignProvinces());
			assertEquals(2, war.getCursorIndex());
			assertEquals(4, war.getInitiativeAttacker());
			assertEquals(4, war.getInitiativeDefender());
			assertEquals(CampaignPhase.INVASION, war.getCampaignPhase());
			assertEquals(ObjectiveHolder.DEFENDER, war.getObjectiveHeldBy());
			assertTrue(war.getOccupiedByAttacker().isEmpty());
			assertFalse(war.isWhitePeaceProposedByAttacker());
			assertEquals(BattleSchedulePhase.VOTING, war.getBattleSchedulePhase());
			assertEquals(LocalDate.parse("2026-08-20"), war.getBattleDay());
			assertTrue(war.getBattleVotes().isEmpty());
			assertEquals(0, war.getPostponementsThisCycle());
		}
	}

	@Test
	void populateCampaign_usesDefenderCapitalWhenCloserThanRegionalObjective() {
		Province atkCapital = province(5, Terrain.PLAINS);
		Province border = province(10, Terrain.PLAINS);
		Province defMid = province(20, Terrain.PLAINS);
		Province defCapital = province(25, Terrain.PLAINS);
		Province regionalFar = province(30, Terrain.PLAINS);
		link(atkCapital, border);
		link(border, defMid);
		link(defMid, defCapital);
		link(defCapital, regionalFar);
		pm.start(Map.of(5, atkCapital, 10, border, 20, defMid, 25, defCapital, 30, regionalFar));

		when(defender.getCapital()).thenReturn(25);

		War war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);

		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			titleManager.when(() -> TitleManager.getProvinces(defender)).thenReturn(List.of(30));
			titleManager.when(() -> TitleManager.getByProvince(5)).thenReturn(attacker);
			titleManager.when(() -> TitleManager.getByProvince(10)).thenReturn(attacker);
			titleManager.when(() -> TitleManager.getByProvince(20)).thenReturn(defender);
			titleManager.when(() -> TitleManager.getByProvince(25)).thenReturn(defender);
			titleManager.when(() -> TitleManager.getByProvince(30)).thenReturn(defender);

			assertTrue(service.populateCampaign(war));
			assertEquals(25, war.getObjectiveProvinceId());
			assertEquals(List.of(5, 10, 20, 25), war.getCampaignProvinces());
			assertEquals(2, war.getCursorIndex());
		}
	}

	@Test
	void populateCampaign_adjacentCapitals_firstBattleAtDefenderCapital() {
		Province atkCapital = province(5, Terrain.PLAINS);
		Province defCapital = province(30, Terrain.PLAINS);
		link(atkCapital, defCapital);
		pm.start(Map.of(5, atkCapital, 30, defCapital));

		War war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);

		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			titleManager.when(() -> TitleManager.getProvinces(defender)).thenReturn(List.of(30));
			titleManager.when(() -> TitleManager.getByProvince(5)).thenReturn(attacker);
			titleManager.when(() -> TitleManager.getByProvince(30)).thenReturn(defender);

			assertTrue(service.populateCampaign(war));
			assertEquals(30, war.getObjectiveProvinceId());
			assertEquals(30, war.getCampaignStartProvinceId());
			assertEquals(List.of(5, 30), war.getCampaignProvinces());
			assertEquals(1, war.getCursorIndex());
		}
	}

	@Test
	void populateCampaign_fortBeforeCapital_firstBattleAtFort() {
		Province atkCapital = province(5, Terrain.PLAINS);
		Province atkBorder = province(10, Terrain.PLAINS);
		Province fort = province(20, Terrain.PLAINS);
		Province defCapital = province(25, Terrain.PLAINS);
		link(atkCapital, atkBorder);
		link(atkBorder, fort);
		link(fort, defCapital);
		pm.start(Map.of(5, atkCapital, 10, atkBorder, 20, fort, 25, defCapital));

		War war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);
		when(defender.getCapital()).thenReturn(25);

		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			titleManager.when(() -> TitleManager.getProvinces(defender)).thenReturn(List.of(20, 25));
			titleManager.when(() -> TitleManager.getByProvince(5)).thenReturn(attacker);
			titleManager.when(() -> TitleManager.getByProvince(10)).thenReturn(attacker);
			titleManager.when(() -> TitleManager.getByProvince(20)).thenReturn(defender);
			titleManager.when(() -> TitleManager.getByProvince(25)).thenReturn(defender);

			assertTrue(service.populateCampaign(war));
			assertEquals(25, war.getObjectiveProvinceId());
			assertEquals(20, war.getCampaignStartProvinceId());
			assertEquals(List.of(5, 10, 20, 25), war.getCampaignProvinces());
			assertEquals(2, war.getCursorIndex());
		}
	}

	@Test
	void populateCampaign_failsWhenAttackerHasNoCapital() {
		when(attacker.getCapital()).thenReturn(0);
		Province border = province(10, Terrain.PLAINS);
		Province objective = province(30, Terrain.PLAINS);
		link(border, objective);
		pm.start(Map.of(10, border, 30, objective));

		War war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);

		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager, border, objective);
			assertFalse(service.populateCampaign(war));
		}
	}

	@Test
	void mergeAxisPaths_deduplicatesBorderProvince() {
		assertEquals(
				List.of(5, 10, 20, 30),
				WarCampaignService.mergeAxisPaths(List.of(5, 10), List.of(10, 20, 30)));
	}

	private void stubOwnership(MockedStatic<TitleManager> titleManager, Province... provinces) {
		titleManager.when(() -> TitleManager.getProvinces(defender)).thenReturn(List.of(20, 30));
		for (Province province : provinces) {
			if (province.getId() == 5 || province.getId() == 10) {
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
