package me.Plugins.SimpleFactions.War.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;
import me.Plugins.SimpleFactions.War.progression.BelligerentRole;

class BattleVoterEligibilityTest {
	private Faction attacker;
	private Faction defender;

	@BeforeEach
	void setUp() {
		Cache.warBattleWindowStartHour = 20;
		Cache.warBattleWindowEndHour = 24;

		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");
	}

	@Test
	void isEligibleVoter_onlyDuringVotingForParticipants() {
		War war = baseWar();
		assertTrue(BattleVoterEligibility.isEligibleVoter(war, attacker));
		assertTrue(BattleVoterEligibility.isEligibleVoter(war, defender));
		assertFalse(BattleVoterEligibility.isEligibleVoter(war, null));

		war.setBattleSchedulePhase(BattleSchedulePhase.SCHEDULED);
		assertFalse(BattleVoterEligibility.isEligibleVoter(war, attacker));
	}

	@Test
	void hourSlotLayout_mapsDefaultWindowHours() {
		List<BattleVoterEligibility.HourSlotEntry> layout = BattleVoterEligibility.hourSlotLayout(
				BattleWindowService.listValidHours());
		assertEquals(5, layout.size());
		assertEquals(28, layout.get(0).slot());
		assertEquals(Integer.valueOf(20), layout.get(0).hour());
		assertEquals(32, layout.get(4).slot());
		assertEquals(Integer.valueOf(24), layout.get(4).hour());
	}

	@Test
	void hourSlotLayout_unusedSlotsHaveNullHour() {
		List<BattleVoterEligibility.HourSlotEntry> layout = BattleVoterEligibility.hourSlotLayout(List.of(20, 21));
		assertEquals(Integer.valueOf(20), layout.get(0).hour());
		assertEquals(Integer.valueOf(21), layout.get(1).hour());
		assertNull(layout.get(2).hour());
	}

	@Test
	void canProposeAutoresolve_onlyDuringVoting() {
		War war = baseWar();
		assertTrue(BattleVoterEligibility.canProposeAutoresolve(war, BelligerentRole.ATTACKER));
		war.setBattleSchedulePhase(BattleSchedulePhase.SCHEDULED);
		assertFalse(BattleVoterEligibility.canProposeAutoresolve(war, BelligerentRole.DEFENDER));
	}

	private War baseWar() {
		War war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);
		war.setWarType(WarType.SUBJUGATE);
		war.setBattleDay(LocalDate.of(2026, 8, 21));
		war.setBattleSchedulePhase(BattleSchedulePhase.VOTING);
		return war;
	}
}
