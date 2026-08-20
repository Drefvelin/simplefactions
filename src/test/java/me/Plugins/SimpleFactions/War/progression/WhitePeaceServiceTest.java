package me.Plugins.SimpleFactions.War.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.ObjectiveHolder;
import me.Plugins.SimpleFactions.War.enums.WarEndReason;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;

class WhitePeaceServiceTest {
	private Faction attacker;
	private Faction defender;

	@BeforeEach
	void setUp() {
		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");
	}

	@Test
	void recalculateProposals_marksAttackerWhenUnreachable() {
		War war = baseWar();
		war.setInitiativeAttacker(1);
		war.setCursorIndex(1);
		assertEquals(Optional.empty(), WhitePeaceService.recalculateProposals(war));
		assertTrue(war.isWhitePeaceProposedByAttacker());
		assertFalse(war.isWhitePeaceProposedByDefender());
	}

	@Test
	void recalculateProposals_clearsAttackerWhenReachable() {
		War war = baseWar();
		war.setInitiativeAttacker(4);
		war.setWhitePeaceProposedByAttacker(true);
		WhitePeaceService.recalculateProposals(war);
		assertFalse(war.isWhitePeaceProposedByAttacker());
	}

	@Test
	void recalculateProposals_marksDefenderUnreachableInCounterPush() {
		War war = baseWar();
		war.setCampaignPhase(CampaignPhase.COUNTER_PUSH);
		war.setInitiativeDefender(1);
		war.setCursorIndex(2);
		WhitePeaceService.recalculateProposals(war);
		assertTrue(war.isWhitePeaceProposedByDefender());
	}

	@Test
	void recalculateProposals_bothInitiativeZeroAutoEnd() {
		War war = baseWar();
		war.setInitiativeAttacker(0);
		war.setInitiativeDefender(0);
		Optional<WarEndReason> reason = WhitePeaceService.recalculateProposals(war);
		assertEquals(WarEndReason.AUTO_WHITE_PEACE, reason.orElse(null));
		assertTrue(war.isWhitePeaceProposedByAttacker());
		assertTrue(war.isWhitePeaceProposedByDefender());
	}

	@Test
	void acceptWhitePeace_requiresEnemyProposal() {
		War war = baseWar();
		war.setWhitePeaceProposedByDefender(true);
		assertTrue(WhitePeaceService.acceptWhitePeace(war, attacker));
		assertFalse(WhitePeaceService.acceptWhitePeace(war, defender));
	}

	@Test
	void shouldAutoEnd_whenBothProposed() {
		War war = baseWar();
		war.setWhitePeaceProposedByAttacker(true);
		war.setWhitePeaceProposedByDefender(true);
		assertTrue(WhitePeaceService.shouldAutoEnd(war));
	}

	private War baseWar() {
		War war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);
		war.setWarType(WarType.SUBJUGATE);
		war.setObjectiveProvinceId(30);
		war.setCampaignStartProvinceId(10);
		war.setCampaignProvinces(List.of(5, 10, 20, 30));
		war.setCursorIndex(1);
		war.setInitiativeAttacker(4);
		war.setInitiativeDefender(4);
		war.setCampaignPhase(CampaignPhase.INVASION);
		war.setObjectiveHeldBy(ObjectiveHolder.DEFENDER);
		return war;
	}
}
