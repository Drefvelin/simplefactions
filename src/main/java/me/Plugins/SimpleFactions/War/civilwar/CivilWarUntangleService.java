package me.Plugins.SimpleFactions.War.civilwar;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import me.Plugins.SimpleFactions.Diplomacy.RelationType;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Loaders.GuildLoader;
import me.Plugins.SimpleFactions.Loaders.RelationLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.War.civilwar.CivilWarLandSplitService.LandSplitPlan;

public final class CivilWarUntangleService {
	private CivilWarUntangleService() {}

	public static void restore(War war) {
		if (war == null) {
			return;
		}
		CivilWarSnapshot snapshot = war.getCivilWarSnapshot();
		if (snapshot == null) {
			return;
		}
		Faction host = FactionManager.getByString(snapshot.getHostFactionId());
		Faction rebels = snapshot.getTempRebelFactionId() == null
				? null
				: FactionManager.getByString(snapshot.getTempRebelFactionId());
		if (host != null && rebels != null) {
			mergeTempRebels(host, rebels, snapshot);
		}
		restoreVassals(snapshot);
	}

	private static void mergeTempRebels(Faction host, Faction rebels, CivilWarSnapshot snapshot) {
		CivilWarRegimentSplitService.mergeRemaining(rebels, host);
		relocateNonBaseGuilds(rebels, host);
		restoreLand(host, rebels, snapshot);
		absorbRebelMainGuild(rebels, host);
		Integer oldCapital = snapshot.getHostOldCapitalId();
		if (oldCapital != null && oldCapital > 0) {
			host.setCapital(oldCapital, true, false);
		}
		try {
			FactionManager.deleteFaction(rebels);
		} catch (Exception ignored) {
			FactionManager.factions.remove(rebels);
		}
	}

	private static void relocateNonBaseGuilds(Faction rebels, Faction host) {
		if (rebels.getGuildHandler() == null) {
			return;
		}
		for (Guild guild : new ArrayList<>(rebels.getGuildHandler().getGuilds())) {
			if (guild == null || guild.isBase()) {
				continue;
			}
			int capital = guild.hasCapital() ? guild.getCapital() : -1;
			guild.relocate(host, capital);
		}
	}

	private static void restoreLand(Faction host, Faction rebels, CivilWarSnapshot snapshot) {
		Set<Integer> tiles = new LinkedHashSet<>();
		if (snapshot.getTransferredProvinces() != null) {
			tiles.addAll(snapshot.getTransferredProvinces().keySet());
		}
		if (rebels.getProvinces() != null) {
			tiles.addAll(rebels.getProvinces());
		}
		if (tiles.isEmpty()) {
			return;
		}
		CivilWarLandSplitService.rollback(
				host,
				rebels,
				new LandSplitPlan(new ArrayList<>(tiles), List.of()));
	}

	private static void absorbRebelMainGuild(Faction rebels, Faction host) {
		if (rebels.getGuildHandler() == null || host.getGuildHandler() == null) {
			return;
		}
		Guild base = rebels.getOrCreateMainGuild();
		if (base == null) {
			return;
		}
		if (GuildLoader.getDefaultType() != null) {
			base.convert(GuildLoader.getDefaultType());
		}
		if (base.isBase()) {
			host.getGuildHandler().addGuild(base);
			return;
		}
		int capital = base.hasCapital() ? base.getCapital() : -1;
		base.relocate(host, capital);
	}

	private static void restoreVassals(CivilWarSnapshot snapshot) {
		if (snapshot.getWartimeVassalEnds() == null) {
			return;
		}
		for (CivilWarWartimeVassalEnd end : snapshot.getWartimeVassalEnds()) {
			if (end == null) {
				continue;
			}
			Faction vassal = FactionManager.getByString(end.factionId());
			Faction overlord = FactionManager.getByString(end.formerOverlordId());
			RelationType type = end.relationTypeId() == null ? null : RelationLoader.getType(end.relationTypeId());
			if (vassal != null && overlord != null && type != null) {
				RelationManager.setRelationForced(type, vassal, overlord);
			}
		}
	}
}
