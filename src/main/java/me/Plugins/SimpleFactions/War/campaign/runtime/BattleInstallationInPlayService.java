package me.Plugins.SimpleFactions.War.campaign.runtime;

import me.Plugins.SimpleFactions.War.core.War;

public final class BattleInstallationInPlayService {
	private BattleInstallationInPlayService() {}

	public static boolean isInPlay(War war, String factionId, String installationId) {
		if (war == null || factionId == null || factionId.isBlank()
				|| installationId == null || installationId.isBlank()) {
			return false;
		}
		return BattleInstallationPickService.getPicks(war, factionId).contains(installationId)
				|| BattleSiegeFortService.isSiegeFortInPlayForFaction(war, factionId, installationId);
	}
}
