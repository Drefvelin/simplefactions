package me.Plugins.SimpleFactions.War.civilwar;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.Utils.RandomRGB;

public final class CivilWarTempRebelFactory {
	private CivilWarTempRebelFactory() {}

	public static Faction create(Faction host, String leaderName) {
		if (host == null || leaderName == null) {
			return null;
		}
		String id = uniqueId(host);
		Faction rebels = new Faction(id, leaderName);
		rebels.setName(plainName(host) + " Rebels");
		if (host.getRGB() != null) {
			String rgb = RandomRGB.similarButDistinct(host.getRGB());
			while (!RandomRGB.isFree(rgb)) {
				rgb = RandomRGB.similarButDistinct(host.getRGB());
			}
			rebels.setRGB(rgb);
		}
		FactionManager.addFaction(rebels);
		return rebels;
	}

	public static Faction createFromMainGuild(Faction host, Guild main, String fallbackLeader) {
		if (main == null) {
			return create(host, fallbackLeader);
		}
		Faction oldHost = main.getFaction();
		if (oldHost != null && oldHost.getGuildHandler() != null) {
			oldHost.getGuildHandler().removeGuild(main.getId());
		}
		Faction rebels = new Faction(main);
		rebels.setName(plainName(host) + " Rebels");
		FactionManager.addFaction(rebels);
		return rebels;
	}

	static String uniqueId(Faction host) {
		String base = Formatter.formatId(host.getId() + "_rebels");
		String id = base;
		int suffix = 2;
		while (FactionManager.getByString(id) != null) {
			id = base + suffix;
			suffix++;
		}
		return id;
	}

	static String plainName(Faction host) {
		if (host == null || host.getName() == null) {
			return "Rebel";
		}
		String plain = Formatter.formatId(host.getName()).trim();
		return plain.isEmpty() ? "Rebel" : plain;
	}
}