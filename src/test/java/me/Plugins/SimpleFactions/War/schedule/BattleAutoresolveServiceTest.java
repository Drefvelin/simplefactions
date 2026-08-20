package me.Plugins.SimpleFactions.War.schedule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;

class BattleAutoresolveServiceTest {
	private Faction attacker;
	private Faction defender;

	@BeforeEach
	void setUp() {
		Cache.warVoteCloseHour = 16;

		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");
	}

	@Test
	void canProposeAutoresolveNow_onlyDuringVotingBeforeVoteCloseOnBattleDay() {
		War war = votingWar();

		assertTrue(BattleAutoresolveService.canProposeAutoresolveNow(
				war, Instant.parse("2026-08-21T15:00:00Z")));

		assertFalse(BattleAutoresolveService.canProposeAutoresolveNow(
				war, Instant.parse("2026-08-21T16:00:00Z")));

		war.setBattleSchedulePhase(BattleSchedulePhase.SCHEDULED);
		assertFalse(BattleAutoresolveService.canProposeAutoresolveNow(
				war, Instant.parse("2026-08-21T15:00:00Z")));
	}

	private War votingWar() {
		War war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);
		war.setWarType(WarType.SUBJUGATE);
		war.setBattleDay(LocalDate.of(2026, 8, 21));
		war.setBattleSchedulePhase(BattleSchedulePhase.VOTING);
		return war;
	}
}
