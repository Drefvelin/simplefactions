package me.Plugins.SimpleFactions.War.campaign.progression;

/**
 * Declare-time war coalition on the campaign axis (maps to {@code war.attackers} / {@code war.defenders}).
 * Not the battle-template attacker/defender side ids.
 */
public enum CampaignCoalition {
	AGGRESSOR("aggressor"),
	DEFENDER("defender");

	private final String jsonId;

	CampaignCoalition(String jsonId) {
		this.jsonId = jsonId;
	}

	public String toJson() {
		return jsonId;
	}

	public static CampaignCoalition fromJson(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		for (CampaignCoalition coalition : values()) {
			if (coalition.jsonId.equalsIgnoreCase(value)) {
				return coalition;
			}
		}
		return null;
	}

	public CampaignCoalition opposing() {
		return this == AGGRESSOR ? DEFENDER : AGGRESSOR;
	}
}
