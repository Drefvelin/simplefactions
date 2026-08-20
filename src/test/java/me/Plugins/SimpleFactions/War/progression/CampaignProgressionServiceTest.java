package me.Plugins.SimpleFactions.War.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Database.WarData;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.WarMapper;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.ObjectiveHolder;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;

class CampaignProgressionServiceTest {
	private Faction attacker;
	private Faction defender;

	@BeforeEach
	void setUp() {
		Cache.warInitiativePerSide = 4;
		Cache.warFirstBattleAtBorder = true;
		Cache.warProvincesBetweenBattles = 1;

		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");
	}

	@Test
	void getOffensiveSide_followsCampaignPhase() {
		War war = baseWar();
		assertEquals(BelligerentRole.ATTACKER, CampaignProgressionService.getOffensiveSide(war));

		war.setCampaignPhase(CampaignPhase.RETAKE);
		assertEquals(BelligerentRole.DEFENDER, CampaignProgressionService.getOffensiveSide(war));

		war.setCampaignPhase(CampaignPhase.COUNTER_PUSH);
		assertEquals(BelligerentRole.DEFENDER, CampaignProgressionService.getOffensiveSide(war));
	}

	@Test
	void applyFoughtBattleOutcome_invasionAttackerWins_movesCursorForwardAndSpendsInitiative() {
		War war = baseWar();
		assertTrue(CampaignProgressionService.applyFoughtBattleOutcome(war, true));
		assertEquals(3, war.getCursorIndex());
		assertEquals(3, war.getInitiativeAttacker());
		assertEquals(1, war.getCampaignBattlesFought());
	}

	@Test
	void applyFoughtBattleOutcome_invasionAttackerLoses_movesCursorBackward() {
		War war = baseWar();
		assertTrue(CampaignProgressionService.applyFoughtBattleOutcome(war, false));
		assertEquals(1, war.getCursorIndex());
		assertEquals(3, war.getInitiativeAttacker());
	}

	@Test
	void applyFoughtBattleOutcome_winAtObjective_entersRetake() {
		War war = baseWar();
		war.setCursorIndex(3);
		assertTrue(CampaignProgressionService.applyFoughtBattleOutcome(war, true));
		assertEquals(CampaignPhase.RETAKE, war.getCampaignPhase());
		assertEquals(ObjectiveHolder.ATTACKER, war.getObjectiveHeldBy());
		assertEquals(3, war.getCursorIndex());
	}

	@Test
	void applyFoughtBattleOutcome_retakeDefenderWins_resetsHolderAndPhase() {
		War war = baseWar();
		war.setCursorIndex(3);
		war.setCampaignPhase(CampaignPhase.RETAKE);
		war.setObjectiveHeldBy(ObjectiveHolder.ATTACKER);
		assertTrue(CampaignProgressionService.applyFoughtBattleOutcome(war, true));
		assertEquals(ObjectiveHolder.DEFENDER, war.getObjectiveHeldBy());
		assertEquals(CampaignPhase.INVASION, war.getCampaignPhase());
		assertEquals(3, war.getCursorIndex());
	}

	@Test
	void applyFoughtBattleOutcome_retakeAttackerWins_movesCursorBack() {
		War war = baseWar();
		war.setCursorIndex(3);
		war.setCampaignPhase(CampaignPhase.RETAKE);
		war.setObjectiveHeldBy(ObjectiveHolder.ATTACKER);
		assertTrue(CampaignProgressionService.applyFoughtBattleOutcome(war, false));
		assertEquals(2, war.getCursorIndex());
		assertEquals(ObjectiveHolder.ATTACKER, war.getObjectiveHeldBy());
		assertEquals(CampaignPhase.RETAKE, war.getCampaignPhase());
	}

	@Test
	void applyFoughtBattleOutcome_counterPushDefenderWins_movesLeft() {
		War war = baseWar();
		war.setCampaignPhase(CampaignPhase.COUNTER_PUSH);
		assertTrue(CampaignProgressionService.applyFoughtBattleOutcome(war, true));
		assertEquals(1, war.getCursorIndex());
		assertEquals(3, war.getInitiativeDefender());
	}

	@Test
	void applyPostponedBattle_doesNotSpendInitiative() {
		War war = baseWar();
		CampaignProgressionService.applyPostponedBattle(war);
		assertEquals(4, war.getInitiativeAttacker());
		assertEquals(4, war.getInitiativeDefender());
		assertEquals(0, war.getCampaignBattlesFought());
	}

	@Test
	void canLaunchOffensive_blocksAttackerAtZeroInitiative() {
		War war = baseWar();
		war.setInitiativeAttacker(0);
		assertTrue(CampaignProgressionService.isAttackerInitiativeExhausted(war));
		assertFalse(CampaignProgressionService.canLaunchOffensive(war, BelligerentRole.ATTACKER));
	}

	@Test
	void resolveNextBattleNodes_firstBattleAtBorder() {
		War war = baseWar();
		assertEquals(List.of(20), CampaignProgressionService.resolveNextBattleNodes(war));
	}

	@Test
	void resolveNextBattleNodes_afterFirstBattle_usesCadenceSpacing() {
		War war = baseWar();
		war.setCampaignBattlesFought(1);
		war.setCursorIndex(2);
		assertEquals(List.of(30), CampaignProgressionService.resolveNextBattleNodes(war));
	}

	@Test
	void resolveNextBattleNodes_attackerExhausted_returnsHoldAndCounterPushChoices() {
		War war = baseWar();
		war.setInitiativeAttacker(0);
		assertEquals(List.of(20, 10), CampaignProgressionService.resolveNextBattleNodes(war));
	}

	@Test
	void applyDefenderCounterPush_setsCounterPushPhase() {
		War war = baseWar();
		war.setInitiativeAttacker(0);
		assertTrue(CampaignProgressionService.applyDefenderCounterPush(war));
		assertEquals(CampaignPhase.COUNTER_PUSH, war.getCampaignPhase());
	}

	@Test
	void applyDefenderHold_keepsInvasionPhase() {
		War war = baseWar();
		war.setInitiativeAttacker(0);
		assertTrue(CampaignProgressionService.applyDefenderHold(war));
		assertEquals(CampaignPhase.INVASION, war.getCampaignPhase());
		assertEquals(2, war.getCursorIndex());
		assertTrue(war.isDefenderChoiceResolved());
	}

	@Test
	void stepsToCapitulationTarget_countsAxisSteps() {
		War war = baseWar();
		assertEquals(1, CampaignProgressionService.stepsToCapitulationTarget(war, BelligerentRole.ATTACKER));
		war.setCampaignPhase(CampaignPhase.COUNTER_PUSH);
		assertEquals(2, CampaignProgressionService.stepsToCapitulationTarget(war, BelligerentRole.DEFENDER));
	}

	@Test
	void warMapper_defaultsMissingCampaignBattlesFought() {
		Faction atk = mock(Faction.class);
		Faction def = mock(Faction.class);
		when(atk.getId()).thenReturn("faction_a");
		when(def.getId()).thenReturn("faction_b");
		FactionManager.factions.clear();
		FactionManager.factions.add(atk);
		FactionManager.factions.add(def);

		WarData data = new WarData();
		data.schemaVersion = 2;
		data.id = 9;
		data.status = "active";
		data.attackers = new me.Plugins.SimpleFactions.Database.SideData();
		data.attackers.leader = "faction_a";
		data.defenders = new me.Plugins.SimpleFactions.Database.SideData();
		data.defenders.leader = "faction_b";

		War war = WarMapper.fromData(data);
		assertEquals(0, war.getCampaignBattlesFought());

		FactionManager.factions.clear();
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
		war.setCampaignPhase(CampaignPhase.INVASION);
		war.setObjectiveHeldBy(ObjectiveHolder.DEFENDER);
		war.setCampaignBattlesFought(0);
		return war;
	}
}
