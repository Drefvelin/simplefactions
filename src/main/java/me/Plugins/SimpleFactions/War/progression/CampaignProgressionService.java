package me.Plugins.SimpleFactions.War.progression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.ObjectiveHolder;

public final class CampaignProgressionService {
	private CampaignProgressionService() {}

	public static BelligerentRole getOffensiveSide(War war) {
		if (!isValidWar(war)) {
			return null;
		}
		CampaignPhase phase = war.getCampaignPhase();
		if (phase == null) {
			return BelligerentRole.ATTACKER;
		}
		return switch (phase) {
			case INVASION -> BelligerentRole.ATTACKER;
			case RETAKE, COUNTER_PUSH -> BelligerentRole.DEFENDER;
		};
	}

	public static boolean canLaunchOffensive(War war, BelligerentRole side) {
		if (!isValidWar(war) || side == null) {
			return false;
		}
		CampaignPhase phase = war.getCampaignPhase();
		if (phase == null) {
			phase = CampaignPhase.INVASION;
		}
		return switch (side) {
			case ATTACKER -> war.getInitiativeAttacker() > 0 && phase == CampaignPhase.INVASION;
			case DEFENDER -> war.getInitiativeDefender() > 0
					&& (phase == CampaignPhase.RETAKE || phase == CampaignPhase.COUNTER_PUSH);
		};
	}

	public static boolean isAttackerInitiativeExhausted(War war) {
		return isValidWar(war) && war.getInitiativeAttacker() <= 0;
	}

	public static List<Integer> resolveNextBattleNodes(War war) {
		if (!isValidWar(war)) {
			return List.of();
		}

		CampaignPhase phase = war.getCampaignPhase();
		if (phase == null) {
			phase = CampaignPhase.INVASION;
		}

		if (phase == CampaignPhase.INVASION && isAttackerInitiativeExhausted(war)) {
			return defenderChoiceNodes(war);
		}

		if (phase == CampaignPhase.RETAKE && canLaunchOffensive(war, BelligerentRole.DEFENDER)) {
			int objectiveIndex = getObjectiveIndex(war);
			if (objectiveIndex >= 0) {
				return List.of(war.getCampaignProvinces().get(objectiveIndex));
			}
			return List.of();
		}

		if (phase == CampaignPhase.COUNTER_PUSH && canLaunchOffensive(war, BelligerentRole.DEFENDER)) {
			int leftIndex = war.getCursorIndex() - 1;
			if (leftIndex >= 0) {
				return List.of(war.getCampaignProvinces().get(leftIndex));
			}
			return List.of();
		}

		if (phase == CampaignPhase.INVASION && canLaunchOffensive(war, BelligerentRole.ATTACKER)) {
			return List.of(resolveNextInvasionTarget(war));
		}

		return List.of();
	}

	public static int stepsToCapitulationTarget(War war, BelligerentRole side) {
		if (!isValidWar(war) || side == null) {
			return 0;
		}
		int cursor = war.getCursorIndex();
		if (side == BelligerentRole.ATTACKER) {
			int objectiveIndex = getObjectiveIndex(war);
			if (objectiveIndex < 0) {
				return 0;
			}
			return stepsAlongAxis(cursor, objectiveIndex);
		}
		return stepsAlongAxis(cursor, 0);
	}

	public static boolean applyFoughtBattleOutcome(War war, boolean offensiveSideWon) {
		if (!isValidWar(war)) {
			return false;
		}

		BelligerentRole offensive = getOffensiveSide(war);
		if (offensive == null || !canLaunchOffensive(war, offensive)) {
			return false;
		}

		CampaignPhase phase = war.getCampaignPhase();
		if (phase == null) {
			phase = CampaignPhase.INVASION;
		}

		spendOffensiveInitiative(war, offensive);

		boolean applied = switch (phase) {
			case INVASION -> applyInvasionOutcome(war, offensiveSideWon);
			case RETAKE -> applyRetakeOutcome(war, offensiveSideWon);
			case COUNTER_PUSH -> applyCounterPushOutcome(war, offensiveSideWon);
		};

		if (applied) {
			war.setCampaignBattlesFought(war.getCampaignBattlesFought() + 1);
		}
		return applied;
	}

	public static void applyPostponedBattle(War war) {
		// Postponed battles spend no initiative and do not move the cursor (step 59).
	}

	public static boolean applyDefenderHold(War war) {
		if (!isValidWar(war) || !isAttackerInitiativeExhausted(war)) {
			return false;
		}
		if (war.getCampaignPhase() != CampaignPhase.INVASION) {
			return false;
		}
		war.setDefenderChoiceResolved(true);
		return true;
	}

	public static boolean applyDefenderCounterPush(War war) {
		if (!isValidWar(war) || !isAttackerInitiativeExhausted(war)) {
			return false;
		}
		if (war.getCampaignPhase() != CampaignPhase.INVASION) {
			return false;
		}
		if (war.getCursorIndex() <= 0) {
			return false;
		}
		war.setCampaignPhase(CampaignPhase.COUNTER_PUSH);
		return true;
	}

	static int getObjectiveIndex(War war) {
		return indexOfObjective(war);
	}

	static int indexOfObjective(War war) {
		if (war == null || war.getObjectiveProvinceId() == null || war.getCampaignProvinces() == null) {
			return -1;
		}
		return war.getCampaignProvinces().indexOf(war.getObjectiveProvinceId());
	}

	static int stepsAlongAxis(int fromIndex, int toIndex) {
		return Math.abs(toIndex - fromIndex);
	}

	static int clampCursorIndex(War war, int index) {
		int max = war.getCampaignProvinces().size() - 1;
		return Math.max(0, Math.min(index, max));
	}

	private static boolean applyInvasionOutcome(War war, boolean offensiveSideWon) {
		int cursor = war.getCursorIndex();
		int objectiveIndex = getObjectiveIndex(war);

		if (offensiveSideWon) {
			if (cursor == objectiveIndex) {
				war.setObjectiveHeldBy(ObjectiveHolder.ATTACKER);
				war.setCampaignPhase(CampaignPhase.RETAKE);
				return true;
			}
			war.setCursorIndex(clampCursorIndex(war, cursor + 1));
			return true;
		}

		war.setCursorIndex(clampCursorIndex(war, cursor - 1));
		return true;
	}

	private static boolean applyRetakeOutcome(War war, boolean offensiveSideWon) {
		int cursor = war.getCursorIndex();

		if (offensiveSideWon) {
			war.setObjectiveHeldBy(ObjectiveHolder.DEFENDER);
			war.setCampaignPhase(CampaignPhase.INVASION);
			return true;
		}

		war.setCursorIndex(clampCursorIndex(war, cursor - 1));
		return true;
	}

	private static boolean applyCounterPushOutcome(War war, boolean offensiveSideWon) {
		int cursor = war.getCursorIndex();
		if (offensiveSideWon) {
			war.setCursorIndex(clampCursorIndex(war, cursor - 1));
		} else {
			war.setCursorIndex(clampCursorIndex(war, cursor + 1));
		}
		return true;
	}

	private static void spendOffensiveInitiative(War war, BelligerentRole offensive) {
		if (offensive == BelligerentRole.ATTACKER) {
			war.setInitiativeAttacker(Math.max(0, war.getInitiativeAttacker() - 1));
		} else {
			war.setInitiativeDefender(Math.max(0, war.getInitiativeDefender() - 1));
		}
	}

	private static int resolveNextInvasionTarget(War war) {
		if (war.getCampaignBattlesFought() == 0 && Cache.warFirstBattleAtBorder) {
			return war.getCampaignProvinces().get(war.getCursorIndex());
		}
		int objectiveIndex = getObjectiveIndex(war);
		int nextIndex = war.getCursorIndex() + Cache.warProvincesBetweenBattles;
		if (objectiveIndex >= 0) {
			nextIndex = Math.min(nextIndex, objectiveIndex);
		}
		nextIndex = clampCursorIndex(war, nextIndex);
		return war.getCampaignProvinces().get(nextIndex);
	}

	private static List<Integer> defenderChoiceNodes(War war) {
		List<Integer> choices = new ArrayList<>();
		choices.add(war.getCampaignProvinces().get(war.getCursorIndex()));
		int leftIndex = war.getCursorIndex() - 1;
		if (leftIndex >= 0) {
			choices.add(war.getCampaignProvinces().get(leftIndex));
		}
		return Collections.unmodifiableList(choices);
	}

	private static boolean isValidWar(War war) {
		return war != null
				&& war.isActive()
				&& war.getCampaignProvinces() != null
				&& !war.getCampaignProvinces().isEmpty()
				&& war.getCursorIndex() >= 0
				&& war.getCursorIndex() < war.getCampaignProvinces().size();
	}
}
