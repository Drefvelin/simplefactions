package me.Plugins.SimpleFactions.War.battle.campaign;

import me.Plugins.SimpleFactions.War.Participant;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.engine.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.BattleJoinService;
import me.Plugins.SimpleFactions.War.battle.engine.BattleManager;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;
import me.Plugins.SimpleFactions.War.battle.warband.Warband;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;

public final class CampaignBattleRosterService {
	private CampaignBattleRosterService() {
	}

	public static void enrollWarbands(War war, Battle battle) {
		if (war == null || battle == null) {
			return;
		}
		for (Participant par : war.getAttackers().getMainParticipants()) {
			enrollParticipant(war, par, battle, BattleTemplate.ATTACKER_SIDE);
		}
		for (Participant par : war.getDefenders().getMainParticipants()) {
			enrollParticipant(war, par, battle, BattleTemplate.DEFENDER_SIDE);
		}
	}

	private static void enrollParticipant(War war, Participant par, Battle battle, String battleSideId) {
		if (par == null || par.getLeader() == null) {
			return;
		}
		String warbandId = par.getLeader().getId();
		Warband warband = WarbandManager.getByString(warbandId);
		if (warband == null) {
			boolean offense = war.getType(par.getLeader()).equalsIgnoreCase("main_attacker");
			if (par.isCivilWar()) {
				offense = false;
			}
			warband = new Warband(war, par, offense);
			WarbandManager.addWarband(warband);
		}
		if (BattleManager.getBattleByMemberId(warband.getLeaderId()) != null) {
			return;
		}
		BattleJoinService.join(warband, battle, battleSideId);
	}
}
