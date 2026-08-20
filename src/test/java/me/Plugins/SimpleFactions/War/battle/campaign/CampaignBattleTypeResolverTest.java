package me.Plugins.SimpleFactions.War.battle.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;

class CampaignBattleTypeResolverTest {
	@Test
	void resolve_returnsFieldForCampaignProvince() {
		Faction attacker = org.mockito.Mockito.mock(Faction.class);
		Faction defender = org.mockito.Mockito.mock(Faction.class);
		War war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);
		war.setWarType(WarType.SUBJUGATE);

		assertEquals(BattleType.FIELD, CampaignBattleTypeResolver.resolve(war, 20));
	}
}
