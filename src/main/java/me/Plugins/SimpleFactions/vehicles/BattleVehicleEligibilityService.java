package me.Plugins.SimpleFactions.vehicles;

import java.util.Optional;

import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.campaign.runtime.BattleInstallationInPlayService;
import me.Plugins.SimpleFactions.War.battle.engine.core.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleManager;
import me.Plugins.SimpleFactions.War.core.War;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public final class BattleVehicleEligibilityService {
	private BattleVehicleEligibilityService() {}

	public static boolean isEligible(War war, String factionId, PlayerVehicleRecord record) {
		if (war == null || factionId == null || factionId.isBlank() || record == null) {
			return true;
		}
		if (!VehicleCategoryRules.isBerthableType(record.getVehicleTypeId())) {
			return true;
		}
		if (record.getMode() != OwnershipMode.INSTALLATION) {
			return false;
		}
		String installationId = record.getInstallationId();
		if (installationId == null || installationId.isBlank()) {
			return false;
		}
		return BattleInstallationInPlayService.isInPlay(war, factionId, installationId);
	}

	public static BattleVehicleEligibilityResult check(
			Player player,
			ActiveVehicle vehicle,
			PlayerVehicleRegistry registry) {
		if (player == null || vehicle == null || registry == null) {
			return BattleVehicleEligibilityResult.ALLOWED;
		}

		Battle battle = BattleManager.getBattleByMemberId(player.getUniqueId());
		if (battle == null || battle.getWarId() == null) {
			return BattleVehicleEligibilityResult.NOT_CAMPAIGN_BATTLE;
		}

		War war = WarManager.getById(battle.getWarId());
		if (war == null || !war.isActive()) {
			return BattleVehicleEligibilityResult.NOT_CAMPAIGN_BATTLE;
		}

		Faction faction = FactionManager.getByMember(player.getName());
		if (faction == null) {
			faction = FactionManager.getByLeader(player.getName());
		}
		if (faction == null || !war.isParticipating(faction)) {
			return BattleVehicleEligibilityResult.NOT_CAMPAIGN_BATTLE;
		}

		Optional<PlayerVehicleRecord> recordOpt = registry.getByVehicleUuid(vehicle.getUUID());
		if (recordOpt.isEmpty()) {
			return BattleVehicleEligibilityResult.ALLOWED;
		}

		PlayerVehicleRecord record = recordOpt.get();
		if (!VehicleCategoryRules.isBerthableType(record.getVehicleTypeId())) {
			return BattleVehicleEligibilityResult.ALLOWED;
		}
		if (record.getMode() != OwnershipMode.INSTALLATION) {
			return BattleVehicleEligibilityResult.DENIED_NOT_BERTHED;
		}
		String installationId = record.getInstallationId();
		if (installationId == null || installationId.isBlank()) {
			return BattleVehicleEligibilityResult.DENIED_NOT_BERTHED;
		}
		if (!BattleInstallationInPlayService.isInPlay(war, faction.getId(), installationId)) {
			return BattleVehicleEligibilityResult.DENIED_NOT_COMMITTED;
		}
		return BattleVehicleEligibilityResult.ALLOWED;
	}
}
