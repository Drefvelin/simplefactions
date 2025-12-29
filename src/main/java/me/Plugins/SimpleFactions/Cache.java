package me.Plugins.SimpleFactions;

import java.util.HashMap;
import java.util.Map;

import me.Plugins.SimpleFactions.enums.Terrain;

public class Cache {
	public static String mapRef;

	public static int maxMembers;
	public static int maxWealthPrestige;
	public static String bankBlock;

	public static int maxExtraNodeCapacity;
	
	public static int maxUntitledProvinces;
	public static int maxFreeTitles;
	public static double deJureRequirement;
	public static boolean mapEnabled;

	public static int provinceCost;
	
	public static HashMap<String, String> icons = new HashMap<>();

	public static Map<Terrain, Double> tradeCarry = new HashMap<>();
}
