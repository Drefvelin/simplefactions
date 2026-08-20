package me.Plugins.SimpleFactions.War.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;

class BattleVoteServiceTest {
	private Faction attacker;
	private Faction defender;
	private War war;
	private UUID attackerVoter;
	private UUID defenderVoter;

	@BeforeEach
	void setUp() {
		Cache.warBattleWindowStartHour = 20;
		Cache.warBattleWindowEndHour = 24;

		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");

		war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);
		war.setWarType(WarType.SUBJUGATE);

		attackerVoter = UUID.randomUUID();
		defenderVoter = UUID.randomUUID();
	}

	@Test
	void toggleVote_addsAndRemovesHour() {
		assertEquals(
				BattleVoteToggleResult.ADDED,
				BattleVoteService.toggleVote(war, attackerVoter, 21, attacker, true));
		assertEquals(Set.of(21), BattleVoteService.getPlayerSelections(war, attackerVoter));

		assertEquals(
				BattleVoteToggleResult.REMOVED,
				BattleVoteService.toggleVote(war, attackerVoter, 21, attacker, true));
		assertTrue(BattleVoteService.getPlayerSelections(war, attackerVoter).isEmpty());
		assertFalse(war.getBattleVotes().containsKey(attackerVoter));
	}

	@Test
	void toggleVote_rejectsOfflineNonParticipantAndInvalidHour() {
		assertEquals(
				BattleVoteToggleResult.REJECTED_OFFLINE,
				BattleVoteService.toggleVote(war, attackerVoter, 21, attacker, false));
		assertEquals(
				BattleVoteToggleResult.REJECTED_NOT_PARTICIPANT,
				BattleVoteService.toggleVote(war, attackerVoter, 21, null, true));
		assertEquals(
				BattleVoteToggleResult.REJECTED_INVALID_HOUR,
				BattleVoteService.toggleVote(war, attackerVoter, 19, attacker, true));
	}

	@Test
	void pickHour_usesMinRuleAndEarliestTie() {
		addAttackerVotes(attackerVoter, 22, 23);
		addDefenderVotes(defenderVoter, 21, 22);

		Function<UUID, Faction> uuidToFaction = uuid -> {
			if (uuid.equals(attackerVoter)) {
				return attacker;
			}
			if (uuid.equals(defenderVoter)) {
				return defender;
			}
			return null;
		};

		assertEquals(OptionalInt.of(22), BattleVoteService.pickHour(war, uuidToFaction));
	}

	@Test
	void pickHour_prefersHigherMinScore() {
		UUID extraAttacker = UUID.randomUUID();
		addAttackerVotes(attackerVoter, 21);
		addAttackerVotes(extraAttacker, 22);
		addDefenderVotes(defenderVoter, 21);

		Function<UUID, Faction> uuidToFaction = uuid -> {
			if (uuid.equals(defenderVoter)) {
				return defender;
			}
			return attacker;
		};

		assertEquals(OptionalInt.of(21), BattleVoteService.pickHour(war, uuidToFaction));
	}

	@Test
	void pickHour_ignoresOutOfWindowStoredHours() {
		war.getBattleVotes().put(attackerVoter, Set.of(19, 21));
		war.getBattleVotes().put(defenderVoter, Set.of(19, 21));

		Function<UUID, Faction> uuidToFaction = uuid -> uuid.equals(attackerVoter) ? attacker : defender;

		assertEquals(OptionalInt.of(21), BattleVoteService.pickHour(war, uuidToFaction));
	}

	@Test
	void pickHour_emptyWhenNoCrossSideOverlap() {
		addAttackerVotes(attackerVoter, 21);
		Function<UUID, Faction> uuidToFaction = uuid -> attacker;
		assertTrue(BattleVoteService.pickHour(war, uuidToFaction).isEmpty());
	}

	private void addAttackerVotes(UUID uuid, int... hours) {
		war.getBattleVotes().put(uuid, hoursToSet(hours));
	}

	private void addDefenderVotes(UUID uuid, int... hours) {
		war.getBattleVotes().put(uuid, hoursToSet(hours));
	}

	private static Set<Integer> hoursToSet(int... hours) {
		Set<Integer> set = new HashSet<>();
		Arrays.stream(hours).forEach(set::add);
		return set;
	}
}
