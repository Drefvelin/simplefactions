package me.Plugins.SimpleFactions.War.declare;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;

public final class WarDeclareRequest {
	private final Faction attacker;
	private final Faction defender;
	private final WarGoalType goal;
	private final String targetTitleId;
	private final String subjectFactionId;

	public WarDeclareRequest(
			Faction attacker,
			Faction defender,
			WarGoalType goal,
			String targetTitleId,
			String subjectFactionId) {
		if (attacker == null) {
			throw new IllegalArgumentException("attacker is required");
		}
		if (defender == null) {
			throw new IllegalArgumentException("defender is required");
		}
		if (goal == null) {
			throw new IllegalArgumentException("goal is required");
		}
		this.attacker = attacker;
		this.defender = defender;
		this.goal = goal;
		this.targetTitleId = targetTitleId;
		this.subjectFactionId = subjectFactionId;
	}

	public static WarDeclareRequest of(Faction attacker, Faction defender, WarGoalType goal) {
		return new WarDeclareRequest(attacker, defender, goal, null, null);
	}

	public Faction getAttacker() {
		return attacker;
	}

	public Faction getDefender() {
		return defender;
	}

	public WarGoalType getGoal() {
		return goal;
	}

	public String getTargetTitleId() {
		return targetTitleId;
	}

	public String getSubjectFactionId() {
		return subjectFactionId;
	}
}
