package me.Plugins.SimpleFactions.War.battle.campaign;

import me.Plugins.SimpleFactions.War.core.Side;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.War.battle.engine.core.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleJoinService;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleSide;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;
import me.Plugins.SimpleFactions.War.battle.warband.Warband;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;
import me.Plugins.SimpleFactions.War.battle.persistence.BattlePersistenceService;
import me.Plugins.SimpleFactions.SimpleFactions;

public final class CampaignBattleRosterService {
	private CampaignBattleRosterService() {
	}

	public static void ensureEnrolled(War war, Battle battle) {
		enrollWarbands(war, battle);
	}

	public static void enrollWarbands(War war, Battle battle) {
		if (war == null || battle == null) {
			return;
		}
		enrollSide(war, battle, war.getAttackers(), BattleTemplate.ATTACKER_SIDE);
		enrollSide(war, battle, war.getDefenders(), BattleTemplate.DEFENDER_SIDE);
		BattlePersistenceService.persistBattle(battle);
		for (BattleSide side : battle.getSides()) {
			for (Warband warband : side.getBands()) {
				BattlePersistenceService.persistWarband(warband);
			}
		}
	}

	private static void enrollSide(War war, Battle battle, Side side, String battleSideId) {
		if (side == null || side.getLeader() == null) {
			if (side == null) {
				SimpleFactions.plugin.getLogger().warning(
						"[SimpleFactions] Skipping campaign warband enroll for missing side " + battleSideId);
			} else {
				SimpleFactions.plugin.getLogger().warning(
						"[SimpleFactions] Skipping campaign warband enroll for side "
								+ battleSideId
								+ " (no war side leader)");
			}
			return;
		}
		String warbandId = Warband.campaignSideWarbandId(war.getId(), battleSideId);
		Warband warband = WarbandManager.getByString(warbandId);
		if (warband == null) {
			warband = Warband.createCampaignSideShell(war, side, battleSideId);
			WarbandManager.addWarband(warband);
		}
		if (isWarbandOnBattleSide(warband, battle, battleSideId)) {
			return;
		}
		String joinError = BattleJoinService.join(warband, battle, battleSideId);
		if (joinError != null && !isWarbandOnBattleSide(warband, battle, battleSideId)) {
			if (SimpleFactions.plugin != null) {
				SimpleFactions.plugin.getLogger().warning(
						"[SimpleFactions] Campaign warband join failed for "
								+ warbandId
								+ ": "
								+ joinError
								+ "; attaching directly.");
			}
			BattleSide battleSide = battle.getSideById(battleSideId);
			if (battleSide != null) {
				battleSide.addBand(warband);
			}
		}
	}

	private static boolean isWarbandOnBattleSide(Warband warband, Battle battle, String battleSideId) {
		if (warband == null || battle == null || battleSideId == null) {
			return false;
		}
		BattleSide side = battle.getSideById(battleSideId);
		if (side == null) {
			return false;
		}
		for (Warband band : side.getBands()) {
			if (band != null && band.getId().equalsIgnoreCase(warband.getId())) {
				return true;
			}
		}
		return false;
	}
}
