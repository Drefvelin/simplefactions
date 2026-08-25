package me.Plugins.SimpleFactions.vehicles;

public final class BattleVehicleEligibilityMessages {
	private BattleVehicleEligibilityMessages() {}

	public static String forResult(BattleVehicleEligibilityResult result) {
		if (result == null) {
			return null;
		}
		return switch (result) {
			case DENIED_NOT_BERTHED ->
					"§cThis vehicle must be berthed at a committed installation for this battle.";
			case DENIED_NOT_COMMITTED ->
					"§cThis vehicle is berthed at an installation not committed for this battle.";
			case ALLOWED, NOT_CAMPAIGN_BATTLE -> null;
		};
	}
}
