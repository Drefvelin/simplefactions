package me.Plugins.SimpleFactions.War.progression;

public enum PostBattleChoicePhase {
	NONE("none"),
	WINNER_PUSH_HOLD("winner_push_hold"),
	LOSER_ATTACK_PEACE("loser_attack_peace");

	private final String jsonId;

	PostBattleChoicePhase(String jsonId) {
		this.jsonId = jsonId;
	}

	public String toJson() {
		return jsonId;
	}

	public static PostBattleChoicePhase fromJson(String value) {
		if (value == null || value.isBlank()) {
			return NONE;
		}
		for (PostBattleChoicePhase phase : values()) {
			if (phase.jsonId.equalsIgnoreCase(value)) {
				return phase;
			}
		}
		return null;
	}
}
