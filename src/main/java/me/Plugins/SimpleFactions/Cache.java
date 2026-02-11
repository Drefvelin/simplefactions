package me.Plugins.SimpleFactions;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.units.qual.t;

import me.Plugins.SimpleFactions.enums.Terrain;

public class Cache {
	public static String mapRef;
	public static String worldName;

	public static int maxMembers;
	public static int maxWealthPrestige;
	public static String baseYear;
	public static int baseIrlYear = java.time.Year.now().getValue();
	public static String bankBlock;
	public static String votingBlock;

	public static int maxExtraNodeCapacity;
	
	public static int maxUntitledProvinces;
	public static int maxFreeTitles;
	public static double deJureRequirement;
	public static boolean mapEnabled;

	public static int provinceCost;
	
	public static HashMap<String, String> icons = new HashMap<>();

	public static double branchUpgradeCost;
	public static double branchUpgradeExponent;

	public static Map<Terrain, Double> tradeCarry = new HashMap<>();
	public static double getTradeCarry(Terrain t) {
		return tradeCarry.getOrDefault(t, 0.5);
	}

	public static String getFantasyYear(long timestamp) {
		// Current IRL year of the timestamp
		int irlYear = Instant.ofEpochMilli(timestamp)
				.atZone(ZoneId.systemDefault())
				.getYear();

		// Parse configured fantasy year (e.g. "322 AE")
		String[] parts = baseYear.split(" ", 2);
		int baseFantasyYear = Integer.parseInt(parts[0]);
		String era = parts.length > 1 ? parts[1] : "";

		// 1-to-1 year mapping
		int yearDelta = irlYear - baseIrlYear;
		int fantasyYear = baseFantasyYear + yearDelta;

		return fantasyYear + (era.isEmpty() ? "" : " " + era);
	}

	public static String getFantasyDate(long timestamp) {

		var zonedDateTime = Instant.ofEpochMilli(timestamp)
				.atZone(ZoneId.systemDefault());

		int day = zonedDateTime.getDayOfMonth();
		int month = zonedDateTime.getMonthValue();

		String fantasyYear = getFantasyYear(timestamp);

		return String.format("%02d/%02d/%s", day, month, fantasyYear);
	}
}
