package me.Plugins.SimpleFactions.Managers.Inventory;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;

class CampaignScheduleInfoTest {
	private Faction attacker;
	private Faction defender;
	private UUID attackerVoter;

	@BeforeEach
	void setUp() {
		Cache.warBattleWindowStartHour = 20;
		Cache.warBattleWindowEndHour = 24;
		Cache.warVoteCloseHour = 16;

		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");
		attackerVoter = UUID.randomUUID();
	}

	@Test
	void buildScheduleInfoLines_includesBattleDayVoteCloseAndSelections() {
		War war = votingWar();
		war.getBattleVotes().put(attackerVoter, Set.of(21, 22));

		List<String> lines = CampaignCreator.buildScheduleInfoLines(
				war,
				attackerVoter,
				uuid -> attacker);

		assertTrue(lines.stream().anyMatch(line -> line.contains("2026-08-21")));
		assertTrue(lines.stream().anyMatch(line -> line.contains("16:00 UTC")));
		assertTrue(lines.stream().anyMatch(line -> line.contains("21:00")));
		assertTrue(lines.stream().anyMatch(line -> line.contains("Total voters")));
		assertTrue(lines.stream().anyMatch(line -> line.contains("Hour votes")));
	}

	@Test
	void buildScheduleInfoLines_includesScheduledFightWhenScheduled() {
		War war = votingWar();
		war.setBattleSchedulePhase(BattleSchedulePhase.SCHEDULED);
		war.setScheduledBattleAt(Instant.parse("2026-08-21T21:00:00Z"));
		war.setScheduledBattleProvinceId(20);

		List<String> lines = CampaignCreator.buildScheduleInfoLines(war, attackerVoter, uuid -> attacker);

		assertTrue(lines.stream().anyMatch(line -> line.contains("2026-08-21T21:00:00Z")));
		assertTrue(lines.stream().anyMatch(line -> line.contains("Battle province")));
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
