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
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.ObjectiveHolder;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;

class CampaignPushProjectionTest {
	private Faction attacker;
	private Faction defender;

	@BeforeEach
	void setUp() {
		Cache.warFirstBattleAtBorder = true;
		Cache.warProvincesBetweenBattles = 1;
		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");
		when(attacker.getCapital()).thenReturn(5);
	}

	@Test
	void canMountOffensiveAfterPush_falseWhenWinnerLacksFuel() {
		War war = baseWar();
		war.setInitiativeAttacker(0);
		war.setLastBattleOffensiveCoalition(CampaignCoalition.AGGRESSOR);
		assertFalse(CampaignPushProjection.canMountOffensiveAfterPush(war, CampaignCoalition.AGGRESSOR));
	}

	@Test
	void canMountOffensiveAfterPush_projectsCursorAdvanceOnOffensiveWin() {
		War war = baseWar();
		war.setLastBattleOffensiveCoalition(CampaignCoalition.AGGRESSOR);
		var projected = CampaignPushProjection.afterPush(war, CampaignCoalition.AGGRESSOR);
		assertEquals(3, projected.cursorIndex());
	}

	private War baseWar() {
		War war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);
		war.setWarType(WarType.SUBJUGATE);
		war.setObjectiveProvinceId(30);
		war.setCampaignStartProvinceId(20);
		war.setCampaignProvinces(List.of(5, 10, 20, 30));
		war.setCursorIndex(2);
		war.setCampaignBattlesFought(1);
		war.setInitiativeAttacker(4);
		war.setInitiativeDefender(4);
		war.setInitiativeHolderCoalition(CampaignCoalition.AGGRESSOR);
		war.setCampaignPhase(CampaignPhase.INVASION);
		war.setPushTarget(CampaignPushTarget.TOWARD_OBJECTIVE);
		war.setObjectiveHeldBy(ObjectiveHolder.DEFENDER);
		war.setPostBattleChoicePhase(PostBattleChoicePhase.NONE);
		war.setPostBattleChoiceResolved(true);
		return war;
	}
}
