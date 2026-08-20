package me.Plugins.SimpleFactions.War.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

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
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.campaign.CampaignBattleLaunchService;
import me.Plugins.SimpleFactions.War.battle.engine.BattleManager;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;

class BattleScheduleTickServiceTest {
	private static final LocalDate BATTLE_DAY = LocalDate.of(2026, 8, 21);

	private Faction attacker;
	private Faction defender;

	@BeforeEach
	void setUp() {
		BattleScheduleTickService.resetHourGateForTests();
		BattleManager.resetForTests();
		WarbandManager.resetForTests();
		Cache.battleCampaignTemplateField = "";
		Cache.warBattleWindowStartHour = 20;
		Cache.warBattleWindowEndHour = 24;
		Cache.warVoteCloseHour = 16;
		Cache.warDefenderChoiceDeadlineHour = 12;
		Cache.warBattleVotingMinPlayers = 4;
		Cache.warBattleVotingRequireSmallestSideFull = true;
		Cache.warBattleVotingPassIfEither = true;
		Cache.warBattleVotingDevMinPlayersEnabled = false;
		Cache.warFirstBattleAtBorder = true;

		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");
		when(attacker.getMembers()).thenReturn(List.of("Alice", "Bob"));
		when(defender.getMembers()).thenReturn(List.of("Carol", "Dave"));
		mockMilitary(attacker);
		mockMilitary(defender);
	}

	@Test
	void shouldRunForHour_onlyOncePerUtcHour() {
		assertTrue(BattleScheduleTickService.shouldRunForHour(Instant.parse("2026-08-21T12:00:00Z")));
		assertFalse(BattleScheduleTickService.shouldRunForHour(Instant.parse("2026-08-21T12:30:00Z")));
		assertTrue(BattleScheduleTickService.shouldRunForHour(Instant.parse("2026-08-21T13:00:00Z")));
	}

	@Test
	void processWar_appliesDefenderDeadlineAtNoon() {
		War war = defenderChoiceWar();

		assertTrue(BattleScheduleTickService.processWar(
				war, Instant.parse("2026-08-21T12:00:00Z")));
		assertTrue(war.isDefenderChoiceResolved());
		assertEquals(BattleSchedulePhase.VOTING, war.getBattleSchedulePhase());
	}

	@Test
	void processWar_closesVoteAtSixteenWhenQuorumMet() {
		War war = votingWar();
		addCrossSideVotes(war, 21);

		withMockBossBar(() -> {
			assertTrue(BattleScheduleTickService.processWar(
					war, Instant.parse("2026-08-21T16:00:00Z")));
			assertEquals(BattleSchedulePhase.SCHEDULED, war.getBattleSchedulePhase());
			assertEquals(
					BATTLE_DAY.atTime(21, 0).atZone(ZoneOffset.UTC).toInstant(),
					war.getScheduledBattleAt());
		});
	}

	@Test
	void processWar_skipsNonVotingPhase() {
		War war = votingWar();
		war.setBattleSchedulePhase(BattleSchedulePhase.SCHEDULED);

		assertFalse(BattleScheduleTickService.processWar(
				war, Instant.parse("2026-08-21T16:00:00Z")));
	}

	@Test
	void processWar_catchUpAtSeventeenRunsDeadlineAndClose() {
		War war = defenderChoiceWar();
		addCrossSideVotes(war, 21);

		withMockBossBar(() -> {
			assertTrue(BattleScheduleTickService.processWar(
					war, Instant.parse("2026-08-21T17:00:00Z")));
			assertTrue(war.isDefenderChoiceResolved());
			assertEquals(BattleSchedulePhase.SCHEDULED, war.getBattleSchedulePhase());
		});
	}

	@Test
	void tick_startsScheduledBattleWhenDue() {
		War war = votingWar();
		war.setBattleSchedulePhase(BattleSchedulePhase.SCHEDULED);
		war.setScheduledBattleHour(21);
		war.setScheduledBattleAt(BATTLE_DAY.atTime(21, 0).atZone(ZoneOffset.UTC).toInstant());
		war.setScheduledBattleProvinceId(20);

		withMockBossBar(() -> {
			WarManager.addWar(war);
			CampaignBattleLaunchService.prepareScheduledBattle(war);
			BattleScheduleTickService.tick(Instant.parse("2026-08-21T21:00:00Z"));
			assertTrue(BattleManager.getByWarId(war.getId()).hasStarted());
		});
	}

	private void withMockBossBar(Runnable action) {
		BossBar bossBar = mock(BossBar.class);
		try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
			bukkit.when(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class)))
					.thenReturn(bossBar);
			bukkit.when(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class), any()))
					.thenReturn(bossBar);
			bukkit.when(() -> Bukkit.getPlayerExact(anyString())).thenReturn(null);
			action.run();
		}
	}

	private War votingWar() {
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
		war.setBattleDay(BATTLE_DAY);
		war.setBattleSchedulePhase(BattleSchedulePhase.VOTING);
		return war;
	}

	private War defenderChoiceWar() {
		War war = votingWar();
		war.setInitiativeAttacker(0);
		return war;
	}

	private void addCrossSideVotes(War war, int hour) {
		war.getBattleVotes().put(BattleScheduleLookups.spoofMemberUuid("Alice"), Set.of(hour));
		war.getBattleVotes().put(BattleScheduleLookups.spoofMemberUuid("Bob"), Set.of(hour));
		war.getBattleVotes().put(BattleScheduleLookups.spoofMemberUuid("Carol"), Set.of(hour));
		war.getBattleVotes().put(BattleScheduleLookups.spoofMemberUuid("Dave"), Set.of(hour));
	}

	private void mockMilitary(Faction faction) {
		Military military = mock(Military.class);
		Regiment levy = mock(Regiment.class);
		when(military.getManpowerNoLevy(anyBoolean())).thenReturn(10);
		when(military.getRegiment("levy")).thenReturn(levy);
		when(levy.getEntries()).thenReturn(List.of());
		when(faction.getMilitary()).thenReturn(military);
		when(faction.getLeader()).thenReturn("leader");
		when(faction.getName()).thenReturn("faction");
	}
}
