package me.Plugins.SimpleFactions.War.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.War.core.WarDeclareHelper;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;

class WarDeclareHelperTest {

	@Test
	void warTypeForGoal_mapsEachGoal() {
		assertEquals(WarType.DE_JURE, WarDeclareHelper.warTypeForGoal(WarGoalType.DE_JURE_ANNEX));
		assertEquals(WarType.SUBJUGATE, WarDeclareHelper.warTypeForGoal(WarGoalType.SUBJUGATE));
		assertEquals(WarType.TRANSFER_SUBJECT, WarDeclareHelper.warTypeForGoal(WarGoalType.TRANSFER_SUBJECT));
	}

	@Test
	void canAnnexByRank_requiresEqualOrHigherTier() {
		assertEquals(true, WarDeclareHelper.canAnnexByRank(3, 2));
		assertEquals(true, WarDeclareHelper.canAnnexByRank(2, 2));
		assertEquals(false, WarDeclareHelper.canAnnexByRank(1, 2));
	}
}
