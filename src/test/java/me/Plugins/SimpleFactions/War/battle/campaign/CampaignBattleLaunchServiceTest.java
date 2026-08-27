package me.Plugins.SimpleFactions.War.battle.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import me.Plugins.SimpleFactions.Army.Military;
import me.Plugins.SimpleFactions.Army.Regiment;
import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.War.battle.engine.core.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleManager;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.CampaignBattleKind;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;
import me.Plugins.SimpleFactions.War.campaign.progression.CampaignCoalition;
import me.Plugins.SimpleFactions.War.campaign.progression.CampaignOffensiveForfeitService;
import me.Plugins.SimpleFactions.War.campaign.progression.CampaignPushTarget;
import me.Plugins.SimpleFactions.War.campaign.progression.PostBattleChoicePhase;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.campaign.runtime.BattleWindowService;
import me.Plugins.SimpleFactions.War.campaign.schedule.ScheduledCampaignBattle;

class CampaignBattleLaunchServiceTest {
	private Faction attacker;
	private Faction defender;

	@BeforeEach
	void setUp() {
		BattleManager.resetForTests();
		WarbandManager.resetForTests();
		Cache.warFirstBattleAtBorder = true;
		Cache.battleCampaignTemplateField = "";
		Cache.battleCampaignTemplateSiege = "";
		Cache.warBattleWindowStartHour = 20;
		Cache.warBattleWindowEndHour = 24;

		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");
		when(attacker.getLeader()).thenReturn("Alice");
		when(defender.getLeader()).thenReturn("Bob");
		mockMilitary(attacker);
		mockMilitary(defender);
	}

	private void mockMilitary(Faction faction) {
		Military military = mock(Military.class);
		Regiment levy = mock(Regiment.class);
		Regiment professional = mock(Regiment.class);
		when(professional.getId()).thenReturn("professional");
		when(professional.isLevy()).thenReturn(false);
		when(professional.isOffensive()).thenReturn(true);
		when(professional.getCurrentSlots()).thenReturn(10);
		when(military.getManpowerNoLevy(anyBoolean())).thenReturn(10);
		when(military.getRegiment("levy")).thenReturn(levy);
		when(military.getRegiments()).thenReturn(java.util.List.of(professional));
		when(levy.getEntries()).thenReturn(java.util.List.of());
		when(faction.getMilitary()).thenReturn(military);
		when(faction.getMembers()).thenReturn(java.util.List.of());
		when(faction.getName()).thenReturn("faction");
	}

	@Test
	void resolve_returnsSiegeForSiegeSlot() {
		War war = baseWar();
		ScheduledCampaignBattle siege = new ScheduledCampaignBattle(20, CampaignBattleKind.SIEGE, false, "fort_a");
		war.setCampaignBattleSchedule(List.of(siege));
		war.setCampaignScheduleIndex(0);

		assertEquals(BattleType.SIEGE, CampaignBattleTypeResolver.resolve(war, siege));
		assertEquals(BattleType.SIEGE, CampaignBattleTypeResolver.resolve(war, 20));
	}

	@Test
	void resolve_returnsFieldForCampaignProvince() {
		War war = baseWar();
		assertEquals(BattleType.FIELD, CampaignBattleTypeResolver.resolve(war, 20));
	}

	@Test
	void resolve_returnsFieldForNavalSlot() {
		War war = baseWar();
		ScheduledCampaignBattle naval = new ScheduledCampaignBattle(
				20, CampaignBattleKind.NAVAL, false, null, "port_a");
		war.setCampaignBattleSchedule(List.of(naval));
		war.setCampaignScheduleIndex(0);

		assertEquals(BattleType.FIELD, CampaignBattleTypeResolver.resolve(war, naval));
	}

	@Test
	void prepareScheduledBattle_navalSlot_setsNavalVariant() {
		War war = scheduledWar();
		war.setCampaignBattleSchedule(List.of(
				new ScheduledCampaignBattle(20, CampaignBattleKind.NAVAL, false, null, "port_a")));
		war.setCampaignScheduleIndex(0);

		withMockBossBar(() -> {
			Battle battle = CampaignBattleLaunchService.prepareScheduledBattle(war);

			assertNotNull(battle);
			assertEquals(BattleType.FIELD, battle.getBattleType());
			assertTrue(battle.isNavalVariant());
		});
	}

	@Test
	void prepareScheduledBattle_navalInvasionSlot_setsNavalVariant() {
		War war = scheduledWar();
		war.setCampaignBattleSchedule(List.of(
				new ScheduledCampaignBattle(20, CampaignBattleKind.NAVAL_INVASION, false, null)));
		war.setCampaignScheduleIndex(0);

		withMockBossBar(() -> {
			Battle battle = CampaignBattleLaunchService.prepareScheduledBattle(war);

			assertNotNull(battle);
			assertEquals(BattleType.FIELD, battle.getBattleType());
			assertTrue(battle.isNavalVariant());
		});
	}

	@Test
	void prepareScheduledBattle_siegeSlot_doesNotSetNavalVariant() {
		War war = scheduledWar();
		war.setCampaignBattleSchedule(List.of(
				new ScheduledCampaignBattle(20, CampaignBattleKind.SIEGE, false, "fort_a")));
		war.setCampaignScheduleIndex(0);

		withMockBossBar(() -> {
			Battle battle = CampaignBattleLaunchService.prepareScheduledBattle(war);

			assertNotNull(battle);
			assertEquals(BattleType.SIEGE, battle.getBattleType());
			assertFalse(battle.isNavalVariant());
		});
	}

	@Test
	void prepareScheduledBattle_createsSiegeBattle() {
		War war = scheduledWar();
		war.setCampaignBattleSchedule(List.of(
				new ScheduledCampaignBattle(20, CampaignBattleKind.SIEGE, false, "fort_a")));
		war.setCampaignScheduleIndex(0);

		withMockBossBar(() -> {
			Battle battle = CampaignBattleLaunchService.prepareScheduledBattle(war);

			assertNotNull(battle);
			assertEquals(BattleType.SIEGE, battle.getBattleType());
		});
	}

	@Test
	void prepareScheduledBattle_setsWarIdProvinceAndType() {
		War war = scheduledWar();

		withMockBossBar(() -> {
			Battle battle = CampaignBattleLaunchService.prepareScheduledBattle(war);

			assertNotNull(battle);
			assertEquals("campaign_w1_p20", battle.getId());
			assertEquals(Integer.valueOf(1), battle.getWarId());
			assertEquals(Integer.valueOf(20), battle.getProvinceId());
			assertEquals(BattleType.FIELD, battle.getBattleType());
			assertFalse(battle.isLocked());
			assertTrue(battle.hasTeleport());
		});
	}

	@Test
	void prepareScheduledBattle_isIdempotent() {
		War war = scheduledWar();

		withMockBossBar(() -> {
			Battle first = CampaignBattleLaunchService.prepareScheduledBattle(war);
			Battle second = CampaignBattleLaunchService.prepareScheduledBattle(war);
			assertEquals(first.getId(), second.getId());
			assertEquals(1, BattleManager.get().size());
		});
	}

	@Test
	void launchAutoresolveBattle_startsImmediately() {
		War war = baseWar();
		war.setBattleSchedulePhase(BattleSchedulePhase.AUTORESOLVE_PENDING);

		withMockBossBar(() -> {
			Battle battle = CampaignBattleLaunchService.launchAutoresolveBattle(war);

			assertNotNull(battle);
			assertTrue(battle.hasStarted());
			assertEquals("campaign_w1_p20", battle.getId());
		});
	}

	@Test
	void tryStartScheduledBattle_failsWhenSiegeContestMissing() {
		War war = scheduledWar();
		war.setCampaignBattleSchedule(List.of(
				new ScheduledCampaignBattle(20, CampaignBattleKind.SIEGE, false, "fort_a")));
		war.setCampaignScheduleIndex(0);
		Instant startAt = war.getScheduledBattleAt();
		SimpleFactions plugin = mock(SimpleFactions.class);
		when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));

		withMockBossBar(() -> {
			try (MockedStatic<CampaignOffensiveForfeitService> forfeit =
					mockStatic(CampaignOffensiveForfeitService.class)) {
				forfeit.when(() -> CampaignOffensiveForfeitService.applyIfBattleOffensiveCannotAttack(
						any(), anyInt())).thenReturn(false);

				SimpleFactions.plugin = plugin;
				CampaignBattleLaunchService.prepareScheduledBattle(war);
				Battle battle = BattleManager.getByWarId(war.getId());
				assertNotNull(battle);
				assertEquals(BattleType.SIEGE, battle.getBattleType());

				assertFalse(CampaignBattleLaunchService.tryStartScheduledBattle(war, startAt));
				assertFalse(battle.hasStarted());
			}
		});
	}

	@Test
	void tryStartScheduledBattle_startsWhenDue() {
		War war = scheduledWar();
		Instant startAt = war.getScheduledBattleAt();
		SimpleFactions plugin = mock(SimpleFactions.class);
		when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));

		withMockBossBar(() -> {
			try (MockedStatic<CampaignOffensiveForfeitService> forfeit =
					mockStatic(CampaignOffensiveForfeitService.class)) {
				forfeit.when(() -> CampaignOffensiveForfeitService.applyIfBattleOffensiveCannotAttack(
						any(), anyInt())).thenReturn(false);

				SimpleFactions.plugin = plugin;
				CampaignBattleLaunchService.prepareScheduledBattle(war);
				assertFalse(BattleManager.getByWarId(war.getId()).hasStarted());

				assertTrue(CampaignBattleLaunchService.tryStartScheduledBattle(war, startAt));
				assertTrue(BattleManager.getByWarId(war.getId()).hasStarted());
			}
		});
	}

	private War baseWar() {
		War war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);
		war.setWarType(WarType.SUBJUGATE);
		war.setObjectiveProvinceId(30);
		war.setCampaignStartProvinceId(20);
		war.setCampaignProvinces(java.util.List.of(5, 10, 20, 30));
		war.setCursorIndex(2);
		war.setInitiativeAttacker(4);
		war.setInitiativeDefender(4);
		war.setInitiativeHolderCoalition(CampaignCoalition.AGGRESSOR);
		war.setPushTarget(CampaignPushTarget.TOWARD_OBJECTIVE);
		war.setPostBattleChoicePhase(PostBattleChoicePhase.NONE);
		war.setPostBattleChoiceResolved(true);
		war.setCampaignPhase(CampaignPhase.INVASION);
		return war;
	}

	private War scheduledWar() {
		War war = baseWar();
		LocalDate battleDay = LocalDate.of(2026, 8, 21);
		war.setBattleDay(battleDay);
		war.setScheduledBattleHour(21);
		war.setScheduledBattleAt(BattleWindowService.computeScheduledBattleAt(battleDay, 21));
		war.setScheduledBattleProvinceId(20);
		war.setBattleSchedulePhase(BattleSchedulePhase.SCHEDULED);
		return war;
	}

	private void withMockBossBar(Runnable action) {
		BossBar bossBar = mock(BossBar.class);
		try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
			bukkit.when(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class)))
					.thenReturn(bossBar);
			bukkit.when(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class), any()))
					.thenReturn(bossBar);
			action.run();
		}
	}
}
