package me.Plugins.SimpleFactions.War.campaign.runtime;

public final class InstallationPickResults {
	private InstallationPickResults() {
	}

	public enum InstallationPickToggleResult {
		ADDED,
		REMOVED,
		REJECTED_WAR_INACTIVE,
		REJECTED_NOT_PARTICIPANT,
		REJECTED_NOT_LEADER,
		REJECTED_LOCKED,
		REJECTED_ZOC_PORT,
		REJECTED_INVALID_INSTALLATION
	}
}
