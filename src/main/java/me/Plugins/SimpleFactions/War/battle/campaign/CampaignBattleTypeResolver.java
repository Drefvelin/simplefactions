package me.Plugins.SimpleFactions.War.battle.campaign;

import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;

public final class CampaignBattleTypeResolver {
	private CampaignBattleTypeResolver() {
	}

	/**
	 * Step 60.09: all campaign battles use field mode. Fort ZOC siege selection is step 63.
	 */
	public static BattleType resolve(War war, int scheduledBattleProvinceId) {
		if (war == null) {
			return BattleType.FIELD;
		}
		return BattleType.FIELD;
	}
}
