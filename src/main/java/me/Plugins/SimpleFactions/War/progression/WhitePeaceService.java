package me.Plugins.SimpleFactions.War.progression;

import java.util.Optional;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.WarEndReason;

public final class WhitePeaceService {
	private WhitePeaceService() {}

	public static Optional<WarEndReason> recalculateProposals(War war) {
		if (!isValidWar(war)) {
			return Optional.empty();
		}

		war.setWhitePeaceProposedByAttacker(shouldProposeAttacker(war));
		war.setWhitePeaceProposedByDefender(shouldProposeDefender(war));

		if (shouldAutoEnd(war)) {
			return Optional.of(WarEndReason.AUTO_WHITE_PEACE);
		}
		return Optional.empty();
	}

	public static boolean shouldAutoEnd(War war) {
		return war != null
				&& war.isWhitePeaceProposedByAttacker()
				&& war.isWhitePeaceProposedByDefender();
	}

	public static boolean acceptWhitePeace(War war, Faction acceptingLeader) {
		if (!isValidWar(war) || acceptingLeader == null) {
			return false;
		}

		String leaderId = acceptingLeader.getId();
		boolean isAttackerLeader = leaderId.equalsIgnoreCase(war.getAttackerLeaderId());
		boolean isDefenderLeader = leaderId.equalsIgnoreCase(war.getDefenderLeaderId());
		if (!isAttackerLeader && !isDefenderLeader) {
			return false;
		}

		if (isAttackerLeader) {
			return war.isWhitePeaceProposedByDefender();
		}
		return war.isWhitePeaceProposedByAttacker();
	}

	static boolean shouldProposeAttacker(War war) {
		if (!isValidWar(war)) {
			return false;
		}
		if (war.getInitiativeAttacker() == 0 && war.getInitiativeDefender() == 0) {
			return true;
		}
		int steps = CampaignProgressionService.stepsToCapitulationTarget(war, BelligerentRole.ATTACKER);
		return steps > war.getInitiativeAttacker();
	}

	static boolean shouldProposeDefender(War war) {
		if (!isValidWar(war)) {
			return false;
		}
		if (war.getInitiativeAttacker() == 0 && war.getInitiativeDefender() == 0) {
			return true;
		}
		CampaignPhase phase = war.getCampaignPhase();
		if (phase != CampaignPhase.COUNTER_PUSH) {
			return false;
		}
		int steps = CampaignProgressionService.stepsToCapitulationTarget(war, BelligerentRole.DEFENDER);
		return steps > war.getInitiativeDefender();
	}

	private static boolean isValidWar(War war) {
		return war != null
				&& war.isActive()
				&& war.getCampaignProvinces() != null
				&& !war.getCampaignProvinces().isEmpty();
	}
}
