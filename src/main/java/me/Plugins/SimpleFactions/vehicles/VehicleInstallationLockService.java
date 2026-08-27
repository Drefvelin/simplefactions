package me.Plugins.SimpleFactions.vehicles;

import java.time.Instant;

import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.War.campaign.raid.CampaignRaidService;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.installation.InstallationVulnerabilityService;

public final class VehicleInstallationLockService {
	public static final String BERTH_BLOCKED =
			"§cCannot berth vehicles at this installation during battle or raid embargo.";
	public static final String UNBERTH_BLOCKED =
			"§cCannot unberth vehicles at this installation during battle or raid embargo.";

	private VehicleInstallationLockService() {}

	public static boolean isVehicleLocked(String installationId, Instant now) {
		if (installationId == null || installationId.isBlank() || now == null) {
			return false;
		}
		if (InstallationVulnerabilityService.isVulnerable(installationId, now)) {
			return true;
		}
		for (War war : WarManager.getActive()) {
			if (CampaignRaidService.isRepairLocked(war, installationId, now)) {
				return true;
			}
		}
		return false;
	}
}
