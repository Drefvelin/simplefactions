package me.Plugins.SimpleFactions.installation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.installation.handler.InstallationHandler;

public final class InstallationLookup {
	private InstallationLookup() {}

	public static Installation findById(String installationId) {
		if (installationId == null || installationId.isBlank()) {
			return null;
		}
		for (Faction faction : FactionManager.factions) {
			if (faction == null || faction.getInstallationHandler() == null) {
				continue;
			}
			Installation installation = faction.getInstallationHandler().getById(installationId);
			if (installation != null) {
				return installation;
			}
		}
		return null;
	}

	public static Installation findCovering(Location location) {
		if (location == null) {
			return null;
		}
		for (Installation installation : all()) {
			if (InstallationBounds.isWithinRadius(installation, location)
					&& InstallationBounds.isCorrectProvince(installation, location)) {
				return installation;
			}
		}
		return null;
	}

	public static List<Installation> all() {
		List<Installation> installations = new ArrayList<>();
		for (Faction faction : FactionManager.factions) {
			if (faction == null || faction.getInstallationHandler() == null) {
				continue;
			}
			InstallationHandler handler = faction.getInstallationHandler();
			installations.addAll(handler.getAll());
		}
		return Collections.unmodifiableList(installations);
	}
}
