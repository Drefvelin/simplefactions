package me.Plugins.SimpleFactions.War.civilwar;

import java.util.LinkedHashMap;
import java.util.Map;

import me.Plugins.SimpleFactions.Army.Military;
import me.Plugins.SimpleFactions.Army.Regiment;
import me.Plugins.SimpleFactions.Objects.Faction;

public final class CivilWarRegimentSplitService {
	private CivilWarRegimentSplitService() {}

	public static Map<String, Integer> split(Faction host, Faction rebels, double powerPercent) {
		Map<String, Integer> moved = new LinkedHashMap<>();
		if (host == null || rebels == null) {
			return moved;
		}
		Military hostMilitary = host.getMilitary();
		Military rebelMilitary = rebels.getMilitary();
		if (hostMilitary == null || rebelMilitary == null) {
			return moved;
		}
		double percent = Math.max(0, Math.min(100, powerPercent));
		if (percent <= 0) {
			return moved;
		}
		if (hostMilitary.getRegiments() == null) {
			return moved;
		}
		for (Regiment hostRegiment : hostMilitary.getRegiments()) {
			if (hostRegiment == null || hostRegiment.isLevy() || hostRegiment.getId() == null) {
				continue;
			}
			Regiment rebelRegiment = rebelMilitary.getRegiment(hostRegiment.getId());
			if (rebelRegiment == null) {
				continue;
			}
			int slots = Math.max(0, hostRegiment.getCurrentSlots());
			int transfer = Math.min(slots, (int) Math.round(slots * percent / 100.0));
			if (transfer <= 0) {
				continue;
			}
			hostRegiment.setCurrentSlots(slots - transfer);
			rebelRegiment.setCurrentSlots(Math.max(0, rebelRegiment.getCurrentSlots()) + transfer);
			moved.put(hostRegiment.getId(), transfer);
		}
		return moved;
	}

	public static void rollback(Faction host, Faction rebels, Map<String, Integer> moved) {
		if (host == null || rebels == null || moved == null || moved.isEmpty()) {
			return;
		}
		Military hostMilitary = host.getMilitary();
		Military rebelMilitary = rebels.getMilitary();
		if (hostMilitary == null || rebelMilitary == null) {
			return;
		}
		for (Map.Entry<String, Integer> entry : moved.entrySet()) {
			if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) {
				continue;
			}
			int amount = entry.getValue();
			Regiment hostRegiment = hostMilitary.getRegiment(entry.getKey());
			Regiment rebelRegiment = rebelMilitary.getRegiment(entry.getKey());
			if (hostRegiment != null) {
				hostRegiment.setCurrentSlots(Math.max(0, hostRegiment.getCurrentSlots()) + amount);
			}
			if (rebelRegiment != null) {
				rebelRegiment.setCurrentSlots(Math.max(0, rebelRegiment.getCurrentSlots() - amount));
			}
		}
	}

	public static void mergeRemaining(Faction from, Faction to) {
		if (from == null || to == null) {
			return;
		}
		Military fromMilitary = from.getMilitary();
		Military toMilitary = to.getMilitary();
		if (fromMilitary == null || toMilitary == null || fromMilitary.getRegiments() == null) {
			return;
		}
		for (Regiment fromRegiment : fromMilitary.getRegiments()) {
			if (fromRegiment == null || fromRegiment.isLevy() || fromRegiment.getId() == null) {
				continue;
			}
			Regiment toRegiment = toMilitary.getRegiment(fromRegiment.getId());
			if (toRegiment == null) {
				continue;
			}
			int slots = Math.max(0, fromRegiment.getCurrentSlots());
			if (slots <= 0) {
				continue;
			}
			toRegiment.setCurrentSlots(Math.max(0, toRegiment.getCurrentSlots()) + slots);
			fromRegiment.setCurrentSlots(0);
		}
	}
}