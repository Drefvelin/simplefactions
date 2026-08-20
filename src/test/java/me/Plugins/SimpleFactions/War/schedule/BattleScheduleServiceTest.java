package me.Plugins.SimpleFactions.War.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;
import me.Plugins.SimpleFactions.War.progression.BelligerentRole;

class BattleScheduleServiceTest {
	private static final LocalDate BATTLE_DAY = LocalDate.of(2026, 8, 21);

	private Faction attacker;
	private Faction defender;
	private UUID attackerVoter1;
	private UUID attackerVoter2;
	private UUID defenderVoter1;
	private UUID defenderVoter2;

	@BeforeEach
	void setUp() {
		Cache.warBattleWindowStartHour = 20;
		Cache.warBattleWindowEndHour = 24;
		Cache.warVoteCloseHour = 16;
		Cache.warDefenderChoiceDeadlineHour = 12;
		Cache.warBattleVotingMinPlayers = 4;
		Cache.warBattleVotingRequireSmallestSideFull = true;
		Cache.warBattleVotingPassIfEither = true;
		Cache.warFirstBattleAtBorder = true;

		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");

		attackerVoter1 = UUID.randomUUID();
		attackerVoter2 = UUID.randomUUID();
		defenderVoter1 = UUID.randomUUID();
		defenderVoter2 = UUID.randomUUID();
	}

	@Test
	void closeVote_schedulesWhenQuorumMet() {
		War war = votingWar();
		addCrossSideVotes(war, 21);

		BattleScheduleCloseResult result = BattleScheduleService.closeVote(
				war,
				Instant.parse("2026-08-21T16:00:00Z"),
				uuidToFaction(),
				name -> null);

		assertEquals(BattleScheduleCloseResult.SCHEDULED, result);
		assertEquals(BattleSchedulePhase.SCHEDULED, war.getBattleSchedulePhase());
		assertEquals(21, war.getScheduledBattleHour());
		assertEquals(Integer.valueOf(20), war.getScheduledBattleProvinceId());
		assertEquals(
				BATTLE_DAY.atTime(21, 0).atZone(ZoneOffset.UTC).toInstant(),
				war.getScheduledBattleAt());
	}

	@Test
	void closeVote_postponesWhenQuorumFails() {
		War war = votingWar();
		war.getBattleVotes().put(attackerVoter1, Set.of(21));

		BattleScheduleCloseResult result = BattleScheduleService.closeVote(
				war,
				Instant.parse("2026-08-21T16:00:00Z"),
				uuidToFaction(),
				name -> null);

		assertEquals(BattleScheduleCloseResult.POSTPONED, result);
		assertEquals(BattleSchedulePhase.VOTING, war.getBattleSchedulePhase());
		assertEquals(BATTLE_DAY.plusDays(1), war.getBattleDay());
		assertEquals(1, war.getPostponementsThisCycle());
		assertTrue(war.getBattleVotes().containsKey(attackerVoter1));
		assertNull(war.getScheduledBattleAt());
		assertFalse(war.isDefenderChoiceResolved());
	}

	@Test
	void closeVote_entersAutoresolvePendingWhenBothLeadersProposed() {
		War war = votingWar();
		war.setAutoresolveProposedByAttacker(true);
		war.setAutoresolveProposedByDefender(true);

		BattleScheduleCloseResult result = BattleScheduleService.closeVote(
				war,
				Instant.parse("2026-08-21T16:00:00Z"),
				uuidToFaction(),
				name -> null);

		assertEquals(BattleScheduleCloseResult.AUTORESOLVE_PENDING, result);
		assertEquals(BattleSchedulePhase.AUTORESOLVE_PENDING, war.getBattleSchedulePhase());
		assertFalse(war.isAutoresolveProposedByAttacker());
		assertFalse(war.isAutoresolveProposedByDefender());
	}

	@Test
	void applyDefenderChoiceDeadline_autoHoldsAtDeadline() {
		War war = defenderChoiceWar();

		assertTrue(BattleScheduleService.applyDefenderChoiceDeadline(
				war, Instant.parse("2026-08-21T12:00:00Z")));
		assertTrue(war.isDefenderChoiceResolved());
	}

	@Test
	void closeVote_blockedWhenDefenderDeadlineAfterVoteClose() {
		Cache.warDefenderChoiceDeadlineHour = 18;
		Cache.warVoteCloseHour = 16;
		War war = defenderChoiceWar();

		BattleScheduleCloseResult result = BattleScheduleService.closeVote(
				war,
				Instant.parse("2026-08-21T16:00:00Z"),
				uuidToFaction(),
				name -> null);

		assertEquals(BattleScheduleCloseResult.BLOCKED_DEFENDER_CHOICE, result);
		assertFalse(war.isDefenderChoiceResolved());
	}

	@Test
	void applyDefenderChoiceDeadline_notDueBeforeConfiguredHour() {
		War war = defenderChoiceWar();
		assertFalse(BattleScheduleService.applyDefenderChoiceDeadline(
				war, Instant.parse("2026-08-21T11:00:00Z")));
		assertFalse(war.isDefenderChoiceResolved());
	}

	@Test
	void closeVote_schedulesAfterAutoHoldAtCloseWhenChoiceNeeded() {
		War war = defenderChoiceWar();
		addCrossSideVotes(war, 21);

		BattleScheduleCloseResult result = BattleScheduleService.closeVote(
				war,
				Instant.parse("2026-08-21T16:00:00Z"),
				uuidToFaction(),
				name -> null);

		assertEquals(BattleScheduleCloseResult.SCHEDULED, result);
		assertTrue(war.isDefenderChoiceResolved());
		assertEquals(Integer.valueOf(20), war.getScheduledBattleProvinceId());
	}

	@Test
	void closeVote_skippedWhenWrongPhaseOrDay() {
		War war = votingWar();
		war.setBattleSchedulePhase(BattleSchedulePhase.SCHEDULED);

		assertEquals(
				BattleScheduleCloseResult.SKIPPED,
				BattleScheduleService.closeVote(
						war,
						Instant.parse("2026-08-21T16:00:00Z"),
						uuidToFaction(),
						name -> null));

		war.setBattleSchedulePhase(BattleSchedulePhase.VOTING);
		assertEquals(
				BattleScheduleCloseResult.SKIPPED,
				BattleScheduleService.closeVote(
						war,
						Instant.parse("2026-08-20T16:00:00Z"),
						uuidToFaction(),
						name -> null));
	}

	@Test
	void proposeAutoresolve_setsSideFlag() {
		War war = votingWar();
		assertTrue(BattleScheduleService.proposeAutoresolve(war, BelligerentRole.ATTACKER));
		assertTrue(war.isAutoresolveProposedByAttacker());
		assertFalse(war.isAutoresolveProposedByDefender());
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
		war.getBattleVotes().put(attackerVoter1, Set.of(hour));
		war.getBattleVotes().put(attackerVoter2, Set.of(hour));
		war.getBattleVotes().put(defenderVoter1, Set.of(hour));
		war.getBattleVotes().put(defenderVoter2, Set.of(hour));
	}

	private Function<UUID, Faction> uuidToFaction() {
		return uuid -> {
			if (uuid.equals(defenderVoter1) || uuid.equals(defenderVoter2)) {
				return defender;
			}
			return attacker;
		};
	}
}
