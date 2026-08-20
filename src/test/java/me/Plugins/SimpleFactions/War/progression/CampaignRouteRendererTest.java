package me.Plugins.SimpleFactions.War.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.ObjectiveHolder;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;
import me.Plugins.SimpleFactions.War.pathfinder.TitleManagerProvinceOwnerLookup;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

class CampaignRouteRendererTest {
	private Faction attacker;
	private Faction defender;
	private TitleManagerProvinceOwnerLookup owners;

	@BeforeEach
	void setUp() {
		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");
		when(attacker.getCapital()).thenReturn(5);
		when(defender.getCapital()).thenReturn(30);
		owners = new TitleManagerProvinceOwnerLookup();
	}

	@Test
	void resolveMaterial_blueForViewerOwnedProvince() {
		War war = baseWar();
		war.setCampaignBattlesFought(1);
		war.setCursorIndex(2);
		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager);
			assertEquals(
					Material.BLUE_CONCRETE,
					CampaignRouteRenderer.resolveOwnershipMaterial(war, attacker, 5, owners));
		}
	}

	@Test
	void resolveMaterial_redForEnemyOwnedProvince() {
		War war = baseWar();
		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager);
			assertEquals(
					Material.RED_CONCRETE,
					CampaignRouteRenderer.resolveMaterial(war, attacker, 30, owners));
		}
	}

	@Test
	void resolveMaterial_greenForSingleNextBattle() {
		War war = baseWar();
		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager);
			assertEquals(
					Material.GREEN_CONCRETE,
					CampaignRouteRenderer.resolveMaterial(war, attacker, 20, owners));
		}
	}

	@Test
	void resolveMaterial_yellowForDefenderChoice() {
		War war = baseWar();
		war.setInitiativeAttacker(0);
		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager);
			assertEquals(
					Material.YELLOW_CONCRETE,
					CampaignRouteRenderer.resolveMaterial(war, defender, 20, owners));
			assertEquals(
					Material.YELLOW_CONCRETE,
					CampaignRouteRenderer.resolveMaterial(war, defender, 10, owners));
		}
	}

	@Test
	void buildRouteLore_includesCursorAndObjectiveTags() {
		War war = baseWar();
		war.setCursorIndex(3);
		List<String> lore = CampaignRouteRenderer.buildRouteLore(war, 30, 3);
		assertTrue(lore.contains(StringFormatter.formatHex("#e6c84aCursor - current front")));
		assertTrue(lore.contains(StringFormatter.formatHex("#e6c84aObjective")));
	}

	private War baseWar() {
		War war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);
		war.setWarType(WarType.SUBJUGATE);
		war.setObjectiveProvinceId(30);
		war.setCampaignStartProvinceId(20);
		war.setCampaignProvinces(List.of(5, 10, 20, 30));
		war.setCursorIndex(2);
		war.setInitiativeAttacker(4);
		war.setInitiativeDefender(4);
		war.setCampaignPhase(CampaignPhase.INVASION);
		war.setObjectiveHeldBy(ObjectiveHolder.DEFENDER);
		war.setCampaignBattlesFought(0);
		return war;
	}

	private void stubOwnership(MockedStatic<TitleManager> titleManager) {
		titleManager.when(() -> TitleManager.getByProvince(5)).thenReturn(attacker);
		titleManager.when(() -> TitleManager.getByProvince(10)).thenReturn(attacker);
		titleManager.when(() -> TitleManager.getByProvince(20)).thenReturn(defender);
		titleManager.when(() -> TitleManager.getByProvince(30)).thenReturn(defender);
	}
}
