package me.Plugins.SimpleFactions.War.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.engine.BattleManager;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;

class WarScheduleAdminServiceTest {
	private static final LocalDate BATTLE_DAY = LocalDate.of(2026, 8, 21);

	private Faction attacker;
	private Faction defender;

	@BeforeEach
	void setUp() {
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
	void openVote_setsVotingAndClearsSchedule() {
		War war = scheduledWar();
		WarScheduleAdminResult result = WarScheduleAdminService.openVote(war);
		assertTrue(result.success());
		assertEquals(BattleSchedulePhase.VOTING, war.getBattleSchedulePhase());
		assertFalse(war.isDefenderChoiceResolved());
	}

	@Test
	void skipDay_advancesBattleDayOnly() {
		War war = votingWar();
		WarScheduleAdminResult result = WarScheduleAdminService.skipDay(war);
		assertTrue(result.success());
		assertEquals(BATTLE_DAY.plusDays(1), war.getBattleDay());
		assertEquals(0, war.getPostponementsThisCycle());
	}

	@Test
	void forceQuorum_setsFlag() {
		War war = votingWar();
		WarScheduleAdminResult result = WarScheduleAdminService.forceQuorum(war);
		assertTrue(result.success());
		assertTrue(war.isForceQuorumNextClose());
	}

	@Test
	void castVote_addsSpoofSelectionsForAllEligibleMembers() {
		War war = votingWar();
		WarScheduleAdminResult result = WarScheduleAdminService.castVote(war, 21, "both");
		assertTrue(result.success());
		assertEquals(4, BattleQuorumService.countDistinctVoters(war));
		assertTrue(result.message().contains("4 voters total"));
	}

	@Test
	void castVoteThenCloseVote_schedulesWithoutOnlinePlayers() {
		War war = votingWar();
		withMockBossBar(() -> {
			assertTrue(WarScheduleAdminService.castVote(war, 21, "both").success());

			WarScheduleAdminResult result = WarScheduleAdminService.closeVote(
					war,
					Instant.parse("2026-08-21T16:00:00Z"));

			assertTrue(result.success());
			assertEquals(BattleSchedulePhase.SCHEDULED, war.getBattleSchedulePhase());
			assertEquals(21, war.getScheduledBattleHour());
		});
	}

	@Test
	void castVote_rejectsInvalidHour() {
		War war = votingWar();
		WarScheduleAdminResult result = WarScheduleAdminService.castVote(war, 15, "both");
		assertFalse(result.success());
	}

	@Test
	void closeVote_forcedSchedulesWithQuorumBypass() {
		War war = votingWar();
		war.getBattleVotes().put(BattleScheduleLookups.spoofMemberUuid("Alice"), Set.of(21));
		war.getBattleVotes().put(BattleScheduleLookups.spoofMemberUuid("Carol"), Set.of(21));
		war.setForceQuorumNextClose(true);

		withMockBossBar(() -> {
			WarScheduleAdminResult result = WarScheduleAdminService.closeVote(
					war,
					Instant.parse("2026-08-20T10:00:00Z"));

			assertTrue(result.success());
			assertEquals(BattleSchedulePhase.SCHEDULED, war.getBattleSchedulePhase());
			assertFalse(war.isForceQuorumNextClose());
		});
	}

	@Test
	void setScheduled_appliesInstantAndProvince() {
		War war = votingWar();
		withMockBossBar(() -> {
			WarScheduleAdminResult result = WarScheduleAdminService.setScheduled(
					war,
					"2026-08-21T21:00:00Z");
			assertTrue(result.success());
			assertEquals(BattleSchedulePhase.SCHEDULED, war.getBattleSchedulePhase());
			assertEquals(
					BATTLE_DAY.atTime(21, 0).atZone(ZoneOffset.UTC).toInstant(),
					war.getScheduledBattleAt());
			assertEquals(Integer.valueOf(20), war.getScheduledBattleProvinceId());
		});
	}

	@Test
	void setScheduled_rejectsInvalidIso() {
		War war = votingWar();
		WarScheduleAdminResult result = WarScheduleAdminService.setScheduled(war, "not-an-instant");
		assertFalse(result.success());
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

	private War scheduledWar() {
		War war = votingWar();
		war.setBattleSchedulePhase(BattleSchedulePhase.SCHEDULED);
		war.setScheduledBattleAt(Instant.parse("2026-08-21T21:00:00Z"));
		war.setScheduledBattleHour(21);
		war.setScheduledBattleProvinceId(20);
		return war;
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
}
