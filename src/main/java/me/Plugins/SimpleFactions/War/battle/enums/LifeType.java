package me.Plugins.SimpleFactions.War.battle.enums;

public enum LifeType {
	COLLECTIVE,
	PER_PLAYER;

	public String toJson() {
		return name();
	}

	public static LifeType fromJson(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		for (LifeType type : values()) {
			if (type.name().equalsIgnoreCase(value)) {
				return type;
			}
		}
		return null;
	}
}