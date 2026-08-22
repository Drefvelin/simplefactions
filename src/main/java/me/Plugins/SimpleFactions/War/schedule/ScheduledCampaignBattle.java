package me.Plugins.SimpleFactions.War.schedule;

import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.enums.CampaignBattleKind;

public record ScheduledCampaignBattle(
		int provinceId,
		CampaignBattleKind kind,
		boolean required,
		String fortInstallationId,
		String portInstallationId) {

	public ScheduledCampaignBattle(
			int provinceId,
			CampaignBattleKind kind,
			boolean required,
			String fortInstallationId) {
		this(provinceId, kind, required, fortInstallationId, null);
	}

	public ScheduledCampaignBattle {
		if (kind == null) {
			kind = CampaignBattleKind.FIELD;
		}
		if (fortInstallationId != null && fortInstallationId.isBlank()) {
			fortInstallationId = null;
		}
		if (portInstallationId != null && portInstallationId.isBlank()) {
			portInstallationId = null;
		}
	}

	public BattleType battleType() {
		return kind == CampaignBattleKind.SIEGE ? BattleType.SIEGE : BattleType.FIELD;
	}
}
