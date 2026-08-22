package me.Plugins.SimpleFactions.War.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.CampaignBattleKind;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.ObjectiveHolder;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;
import me.Plugins.SimpleFactions.War.pathfinder.TitleManagerProvinceOwnerLookup;
import me.Plugins.SimpleFactions.War.schedule.CampaignUiCopy;
import me.Plugins.SimpleFactions.War.schedule.ScheduledCampaignBattle;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

class CampaignRouteRendererTest {
	private Faction attacker;
	private Faction defender;
	private TitleManagerProvinceOwnerLookup owners;

	@BeforeEach
	void setUp() {
		Cache.warFirstBattleAtBorder = true;
		Cache.warProvincesBetweenBattles = 1;
		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");
		when(attacker.getName()).thenReturn("AttackerRealm");
		when(defender.getName()).thenReturn("DefenderRealm");
		when(attacker.getCapital()).thenReturn(5);
		when(defender.getCapital()).thenReturn(30);
		owners = new TitleManagerProvinceOwnerLookup();
	}

	@Test
	void resolveMaterial_viewerContextCases() {
		War ownershipWar = baseWar();
		ownershipWar.setCampaignBattlesFought(1);
		ownershipWar.setCursorIndex(2);

		War routeWar = baseWar();
		War choicePendingWar = baseWar();
		choicePendingWar.setPostBattleChoicePhase(PostBattleChoicePhase.WINNER_PUSH_HOLD);
		choicePendingWar.setPostBattleChoiceResolved(false);

		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager);
			assertEquals(
					Material.BLUE_CONCRETE,
					CampaignRouteRenderer.resolveOwnershipMaterial(ownershipWar, attacker, 5, owners));
			assertEquals(
					Material.RED_CONCRETE,
					CampaignRouteRenderer.resolveMaterial(routeWar, attacker, 30, owners));
			assertEquals(
					Material.GREEN_CONCRETE,
					CampaignRouteRenderer.resolveMaterial(routeWar, attacker, 20, owners));
			assertEquals(
					Material.RED_CONCRETE,
					CampaignRouteRenderer.resolveMaterial(choicePendingWar, attacker, 20, owners));
		}
	}

	@Test
	void buildRouteLore_showsFieldBattleOnScheduledProvince() {
		War war = baseWar();
		war.setCampaignBattleSchedule(List.of(
				new ScheduledCampaignBattle(20, CampaignBattleKind.FIELD, false, null)));
		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager);
			List<String> lore = CampaignRouteRenderer.buildRouteLore(war, 20, owners);
			assertTrue(lore.contains(StringFormatter.formatHex(CampaignUiCopy.BATTLE_KIND + "Field Battle")));
		}
	}

	@Test
	void buildRouteLore_showsSiegeOnScheduledProvince() {
		War war = baseWar();
		war.setCampaignBattleSchedule(List.of(
				new ScheduledCampaignBattle(18, CampaignBattleKind.SIEGE, false, "fort_a")));
		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			titleManager.when(() -> TitleManager.getByProvince(18)).thenReturn(defender);
			List<String> lore = CampaignRouteRenderer.buildRouteLore(war, 18, owners);
			assertTrue(lore.contains(StringFormatter.formatHex(CampaignUiCopy.BATTLE_KIND + "Siege")));
		}
	}

	@Test
	void buildRouteLore_showsNavalBattleOnScheduledProvince() {
		War war = baseWar();
		war.setCampaignBattleSchedule(List.of(
				new ScheduledCampaignBattle(20, CampaignBattleKind.NAVAL, false, null, "port_a")));
		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager);
			List<String> lore = CampaignRouteRenderer.buildRouteLore(war, 20, owners);
			assertTrue(lore.contains(StringFormatter.formatHex(CampaignUiCopy.BATTLE_KIND + "Naval Battle")));
		}
	}

	@Test
	void buildRouteLore_showsNavalInvasionOnScheduledProvince() {
		War war = baseWar();
		war.setCampaignBattleSchedule(List.of(
				new ScheduledCampaignBattle(21, CampaignBattleKind.NAVAL_INVASION, false, null)));
		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			titleManager.when(() -> TitleManager.getByProvince(21)).thenReturn(defender);
			List<String> lore = CampaignRouteRenderer.buildRouteLore(war, 21, owners);
			assertTrue(lore.contains(StringFormatter.formatHex(CampaignUiCopy.BATTLE_KIND + "Naval Invasion")));
		}
	}

	@Test
	void buildRouteLore_omitsBattleKindWithoutSchedule() {
		War war = baseWar();
		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager);
			List<String> lore = CampaignRouteRenderer.buildRouteLore(war, 20, owners);
			assertFalse(lore.stream().anyMatch(line ->
					line.contains("Field Battle")
							|| line.contains("Siege")
							|| line.contains("Naval Battle")
							|| line.contains("Naval Invasion")));
		}
	}

	@Test
	void buildRouteLore_includesObjectiveRealmAndNextBattle() {
		War war = baseWar();
		war.setCursorIndex(3);
		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			stubOwnership(titleManager);
			List<String> lore = CampaignRouteRenderer.buildRouteLore(war, 30, owners);
			assertTrue(lore.contains(StringFormatter.formatHex(CampaignUiCopy.OBJECTIVE + "Defender Capital")));
			assertTrue(lore.stream().anyMatch(line -> line.contains("Part of") && line.contains("DefenderRealm")));
			assertFalse(lore.stream().anyMatch(line -> line.contains("Cursor")));
			assertFalse(lore.stream().anyMatch(line -> line.contains("First battle")));
		}
	}

	@Test
	void buildRouteLore_showsVassalOwnerWhenNotLeader() {
		Faction vassal = mock(Faction.class);
		when(vassal.getId()).thenReturn("vassal");
		when(vassal.getName()).thenReturn("VassalHold");

		War war = baseWar();
		war.getAttackers().getMainParticipants().get(0).getSubjects().add(vassal);
		try (MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			titleManager.when(() -> TitleManager.getByProvince(5)).thenReturn(attacker);
			titleManager.when(() -> TitleManager.getByProvince(10)).thenReturn(vassal);
			titleManager.when(() -> TitleManager.getByProvince(20)).thenReturn(defender);
			titleManager.when(() -> TitleManager.getByProvince(30)).thenReturn(defender);

			List<String> lore = CampaignRouteRenderer.buildRouteLore(war, 10, owners);
			assertTrue(lore.stream().anyMatch(line -> line.contains("(Owned by VassalHold)")));
		}
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
		war.setInitiativeHolder(BelligerentRole.ATTACKER);
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
