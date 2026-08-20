package me.Plugins.SimpleFactions.War.progression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OccupationZone {
	private final List<Integer> provinceIds;

	private OccupationZone(List<Integer> provinceIds) {
		this.provinceIds = List.copyOf(provinceIds);
	}

	public static OccupationZone of(List<Integer> provinceIds) {
		if (provinceIds == null || provinceIds.isEmpty()) {
			return new OccupationZone(List.of());
		}
		return new OccupationZone(new ArrayList<>(provinceIds));
	}

	public List<Integer> provinceIds() {
		return provinceIds;
	}
}
