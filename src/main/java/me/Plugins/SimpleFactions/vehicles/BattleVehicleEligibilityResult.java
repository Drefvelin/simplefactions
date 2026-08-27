package me.Plugins.SimpleFactions.vehicles;

	public enum BattleVehicleEligibilityResult {
	ALLOWED,
	NOT_CAMPAIGN_BATTLE,
	DENIED_PRE_BATTLE_WARBAND,
	DENIED_NOT_BERTHED,
	DENIED_NOT_COMMITTED;

	public boolean isDenied() {
		return this == DENIED_PRE_BATTLE_WARBAND
				|| this == DENIED_NOT_BERTHED
				|| this == DENIED_NOT_COMMITTED;
	}
}
