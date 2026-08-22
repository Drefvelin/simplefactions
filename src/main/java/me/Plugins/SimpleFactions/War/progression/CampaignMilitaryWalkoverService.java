package me.Plugins.SimpleFactions.War.progression;

import java.util.Optional;

import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.naming.BattleNamingService;
import me.Plugins.SimpleFactions.War.enums.ObjectiveHolder;
import me.Plugins.SimpleFactions.War.resolution.WarResolutionService;
import me.Plugins.SimpleFactions.War.pathfinder.TitleManagerProvinceOwnerLookup;
import me.Plugins.SimpleFactions.SimpleFactions;

public final class CampaignMilitaryWalkoverService {
	private static final int MAX_CHAIN = 32;

	private CampaignMilitaryWalkoverService() {}

	public static void resolvePendingWalkovers(War war) {
		if (war == null || !war.isActive() || CampaignPostBattleChoiceService.needsAnyChoice(war)) {
			return;
		}
		for (int i = 0; i < MAX_CHAIN && war.isActive(); i++) {
			if (CampaignPostBattleChoiceService.needsAnyChoice(war)) {
				return;
			}
			Optional<Integer> province = CampaignCapabilityService.nextBattleProvince(war)
					.stream()
					.boxed()
					.findFirst();
			if (province.isEmpty()) {
				return;
			}
			int battleProvince = province.get();
			CampaignCoalition holder = CampaignCoalitionService.getInitiativeHolderCoalition(war);
			CampaignCoalition opponent = holder.opposing();
			boolean holderCanAttack = CampaignCapabilityService.canAttack(war, holder);
			boolean opponentCanDefend = CampaignCapabilityService.canDefend(war, battleProvince, opponent);
			boolean holderCanDefend = CampaignCapabilityService.canDefend(war, battleProvince, holder);

			if (!holderCanDefend && !opponentCanDefend) {
				WarResolutionService.endWhitePeace(war);
				return;
			}
			if (!holderCanAttack) {
				if (CampaignOffensiveForfeitService.applyIfBattleOffensiveCannotAttack(war, battleProvince)) {
					continue;
				}
				return;
			}
			if (opponentCanDefend) {
				return;
			}

			applyWalkoverWin(war, holder, battleProvince);
		}
	}

	static void applyWalkoverWin(War war, CampaignCoalition winner, int battleProvinceId) {
		CampaignPushTarget preBattlePushTarget = CampaignCapabilityService.effectivePushTarget(war);
		ObjectiveHolder preBattleObjectiveHeldBy = war.getObjectiveHeldBy();
		war.setLastBattleOffensiveCoalition(CampaignCoalitionService.getInitiativeHolderCoalition(war));
		CampaignBattleEndService.spendOffensiveFuel(war);
		CampaignBattleEndService.advanceAlongPushTarget(war);
		BattleNamingService.recordLocationBattle(war, battleProvinceId);
		occupationService().applyBattleWin(
				war,
				battleProvinceId,
				CampaignCoalitionService.coalitionToBelligerentRole(winner));
		CampaignCoalitionService.setInitiativeHolderCoalition(war, winner);
		war.setCampaignBattlesFought(war.getCampaignBattlesFought() + 1);
		CampaignBattleEndService.clearHoldPeace(war);
		WarResolutionService.tryEndAfterBattle(
				war,
				battleProvinceId,
				winner,
				preBattlePushTarget,
				preBattleObjectiveHeldBy);
	}

	private static OccupationService occupationService() {
		if (SimpleFactions.plugin != null) {
			return new OccupationService(
					SimpleFactions.plugin.getProvinceManager(),
					new TitleManagerProvinceOwnerLookup());
		}
		return new OccupationService(null, new TitleManagerProvinceOwnerLookup());
	}
}
