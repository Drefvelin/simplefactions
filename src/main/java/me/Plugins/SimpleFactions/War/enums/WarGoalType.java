package me.Plugins.SimpleFactions.War.enums;

public enum WarGoalType {
	DE_JURE_ANNEX("de_jure_annex"),
	SUBJUGATE("subjugate"),
	TRANSFER_SUBJECT("transfer_subject");

	private final String jsonId;

	WarGoalType(String jsonId) {
		this.jsonId = jsonId;
	}

	public String toJson() {
		return jsonId;
	}

	public String getDisplayName() {
		return switch (this) {
			case DE_JURE_ANNEX -> "De Jure Annex";
			case SUBJUGATE -> "Subjugate";
			case TRANSFER_SUBJECT -> "Transfer Subject";
		};
	}

	public static WarGoalType fromJson(String value) {
		if (value == null || value.isBlank()) return null;
		for (WarGoalType type : values()) {
			if (type.jsonId.equalsIgnoreCase(value)) return type;
		}
		return null;
	}
}
