package me.Plugins.SimpleFactions.War.campaign.runtime;

import me.Plugins.SimpleFactions.installation.InstallationKind;

public enum RaidKind {
	NAVAL(InstallationKind.PORT),
	AIR(InstallationKind.AIRPORT),
	FORT(InstallationKind.FORT);

	private final InstallationKind installationKind;

	RaidKind(InstallationKind installationKind) {
		this.installationKind = installationKind;
	}

	public InstallationKind getInstallationKind() {
		return installationKind;
	}

	public boolean matches(InstallationKind kind) {
		return kind != null && installationKind == kind;
	}
}
