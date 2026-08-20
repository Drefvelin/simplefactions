package me.Plugins.SimpleFactions.War.battle.engine;

import org.bukkit.Location;

public final class BattleCapturePoints {
	private BattleCapturePoints() {
	}

	public static String letterForIndex(int index) {
		if (index < 0) {
			return "A";
		}
		StringBuilder label = new StringBuilder();
		int value = index + 1;
		while (value > 0) {
			value--;
			label.insert(0, (char) ('A' + (value % 26)));
			value /= 26;
		}
		return label.toString();
	}

	public static int countForSide(Battle battle, String sideId) {
		int count = 0;
		for (CapturePoint point : battle.getPoints()) {
			if (sideId != null && sideId.equalsIgnoreCase(point.getAdvanceSideId())) {
				count++;
			}
		}
		return count;
	}

	public static int nextSequenceIndex(Battle battle, String sideId) {
		int max = -1;
		for (CapturePoint point : battle.getPoints()) {
			if (sideId != null && sideId.equalsIgnoreCase(point.getAdvanceSideId())) {
				max = Math.max(max, point.getSequenceIndex());
			}
		}
		return max + 1;
	}

	public static CapturePoint createAtPlayer(Battle battle, String sideId, Location location, BattleSide controller) {
		String id = letterForIndex(countForSide(battle, sideId));
		while (battle.getPointById(id) != null) {
			id = id + "'";
		}
		CapturePoint point = new CapturePoint(id, location, controller, 100);
		point.setAdvanceSideId(sideId);
		point.setSequenceIndex(nextSequenceIndex(battle, sideId));
		return point;
	}
}
