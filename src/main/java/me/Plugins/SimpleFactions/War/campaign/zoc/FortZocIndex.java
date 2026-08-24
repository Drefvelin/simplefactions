package me.Plugins.SimpleFactions.War.campaign.zoc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Map.export.ZocRealm;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.installation.Installation;
import me.Plugins.SimpleFactions.installation.InstallationKind;

public final class FortZocIndex {
	public record OperationalFort(String id, Faction owner, int province, long completedAt) {
	}

	private final Map<Integer, OperationalFort> provinceToFort;

	FortZocIndex(Map<Integer, OperationalFort> provinceToFort) {
		this.provinceToFort = Map.copyOf(provinceToFort);
	}

	public static List<OperationalFort> listOperationalForts() {
		List<OperationalFort> forts = new ArrayList<>();
		for (Faction faction : FactionManager.factions) {
			if (faction == null) {
				continue;
			}
			for (Installation installation : faction.getInstallationHandler().getAll()) {
				if (installation.getKind() != InstallationKind.FORT) {
					continue;
				}
				forts.add(new OperationalFort(
						installation.getId(),
						faction,
						installation.getProvince(),
						installation.getCompletedAt()));
			}
		}
		return forts;
	}

	public static FortZocIndex fromGameState() {
		return fromForts(listOperationalForts());
	}

	public static FortZocIndex fromForts(List<OperationalFort> forts) {
		if (forts == null || forts.isEmpty()) {
			return new FortZocIndex(Map.of());
		}

		List<OperationalFort> sorted = new ArrayList<>(forts);
		sorted.sort(Comparator
				.comparingLong(OperationalFort::completedAt)
				.thenComparing(OperationalFort::id));

		Map<Integer, OperationalFort> provinceToFort = new HashMap<>();
		for (OperationalFort fort : sorted) {
			if (fort == null || fort.owner() == null || fort.id() == null) {
				continue;
			}
			for (int provinceId : ZocRealm.computeZocProvinces(fort.owner(), fort.province())) {
				provinceToFort.putIfAbsent(provinceId, fort);
			}
		}
		return new FortZocIndex(provinceToFort);
	}

	public Optional<OperationalFort> fortForProvince(int provinceId) {
		return Optional.ofNullable(provinceToFort.get(provinceId));
	}
}
