package me.Plugins.SimpleFactions.War.civilwar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.Diplomacy.Relation;
import me.Plugins.SimpleFactions.Diplomacy.RelationType;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Loaders.GuildLoader;
import me.Plugins.SimpleFactions.Loaders.RelationLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.civilwar.CivilWarLandSplitService.LandSplitPlan;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.enums.Member;
import me.Plugins.SimpleFactions.government.movement.Action;
import me.Plugins.SimpleFactions.government.movement.Movement;
import me.Plugins.SimpleFactions.government.movement.cause.Cause;
import me.Plugins.SimpleFactions.government.proposal.Proposal;

public final class CivilWarStartService {
	private CivilWarStartService() {}

	public static String start(Movement movement) {
		if (movement == null || movement.isFrozen()) {
			return CivilWarCopy.COULD_NOT_START;
		}
		Faction host = movement.getFaction();
		if (host == null) {
			return CivilWarCopy.COULD_NOT_START;
		}
		WarGoalType goal = CivilWarGoalMapper.fromFirstCause(movement);
		if (goal == null) {
			return CivilWarCopy.UNMAPPABLE_CAUSE;
		}
		String lockError = CivilWarBorderLock.refuseStart(movement, host);
		if (lockError != null) {
			return lockError;
		}

		List<Guild> hostRebelGuilds = supportingHostGuilds(movement, host);
		boolean leaderIsVassal = isVassalMember(host, movement.getLeader());
		boolean needsTempRebels = !hostRebelGuilds.isEmpty() || !leaderIsVassal;
		LandSplitPlan plan = null;
		if (needsTempRebels) {
			plan = CivilWarLandSplitService.plan(host, hostRebelGuilds);
			if (plan == null) {
				return CivilWarCopy.LAND_SPLIT_FAILED;
			}
			if (!CivilWarSeaPortGate.rebelsWouldHaveRequiredPort(host, plan)) {
				return CivilWarCopy.NO_PORT_ON_SEA;
			}
		}

		AppliedStart applied = applyShape(movement, host, hostRebelGuilds, leaderIsVassal, needsTempRebels, plan);
		if (applied.error != null) {
			return applied.error;
		}
		applied.regimentMoves = CivilWarRegimentSplitService.split(
				host,
				applied.tempRebels,
				movement.getPower());

		War war = WarManager.startCivilWar(
				applied.warLeader,
				host,
				goal,
				movement.getId(),
				applied.extraAttackers,
				movement.getForeignBackers(),
				applied.snapshot);
		if (war == null) {
			rollback(applied);
			String last = WarManager.getLastDeclareError();
			return last != null && !last.isBlank() ? last : CivilWarCopy.COULD_NOT_START;
		}

		movement.setFrozen(true);
		return null;
	}

	static List<Guild> supportingHostGuilds(Movement movement, Faction host) {
		List<Guild> result = new ArrayList<>();
		if (movement == null || host == null || host.getId() == null) {
			return result;
		}
		for (Guild guild : movement.getAllSupportingGuilds()) {
			if (guild == null || guild.isBase()) {
				continue;
			}
			if (guild.getFaction() != null && host.getId().equalsIgnoreCase(guild.getFaction().getId())) {
				result.add(guild);
			}
		}
		return result;
	}

	static List<Faction> supportingVassals(Movement movement, Faction host) {
		List<Faction> result = new ArrayList<>();
		if (movement == null || host == null || host.getId() == null) {
			return result;
		}
		for (Faction vassal : movement.getAllSupportingFactions()) {
			if (vassal == null || vassal.getId() == null) {
				continue;
			}
			if (vassal.getId().equalsIgnoreCase(host.getId())) {
				continue;
			}
			result.add(vassal);
		}
		return result;
	}

	private static AppliedStart applyShape(
			Movement movement,
			Faction host,
			List<Guild> hostRebelGuilds,
			boolean leaderIsVassal,
			boolean needsTempRebels,
			LandSplitPlan plan) {
		AppliedStart applied = new AppliedStart();
		applied.host = host;
		applied.plan = plan;
		applied.hostOldCapital = host.getCapital();

		List<Faction> vassals = supportingVassals(movement, host);
		if (needsTempRebels) {
			Guild main = pickRebelMainGuild(movement, hostRebelGuilds);
			Faction rebels;
			if (main != null) {
				rebels = CivilWarTempRebelFactory.createFromMainGuild(host, main, movement.getLeader());
			} else {
				rebels = CivilWarTempRebelFactory.create(host, movement.getLeader());
			}
			if (rebels == null) {
				applied.error = CivilWarCopy.COULD_NOT_START;
				return applied;
			}
			applied.tempRebels = rebels;
			for (Guild guild : hostRebelGuilds) {
				if (main != null && guild.getId() != null && guild.getId().equalsIgnoreCase(main.getId())) {
					continue;
				}
				int capital = guild.hasCapital() ? guild.getCapital() : -1;
				guild.relocate(rebels, capital);
			}
			if (main != null && GuildLoader.getBaseType() != null && !main.isBase()) {
				main.convert(GuildLoader.getBaseType());
			}
			moveCitizenSupporters(movement, host, rebels);
			applyChangeLeaderTarget(movement, host, rebels);
			CivilWarLandSplitService.apply(host, rebels, plan);
			applied.splitApplied = true;
			assignCapitals(host, rebels, plan, applied);
		}

		endWartimeVassalage(host, vassals, applied);

		if (applied.tempRebels != null && !leaderIsVassal) {
			applied.warLeader = applied.tempRebels;
			applied.extraAttackers.addAll(vassals);
		} else {
			Faction leaderFaction = FactionManager.getByMember(movement.getLeader());
			applied.warLeader = leaderFaction != null ? leaderFaction : (vassals.isEmpty() ? applied.tempRebels : vassals.get(0));
			for (Faction vassal : vassals) {
				if (applied.warLeader != null && vassal.getId().equalsIgnoreCase(applied.warLeader.getId())) {
					continue;
				}
				applied.extraAttackers.add(vassal);
			}
			if (applied.tempRebels != null
					&& (applied.warLeader == null
							|| !applied.tempRebels.getId().equalsIgnoreCase(applied.warLeader.getId()))) {
				applied.extraAttackers.add(applied.tempRebels);
			}
		}
		if (applied.warLeader == null) {
			applied.error = CivilWarCopy.COULD_NOT_START;
			rollback(applied);
			return applied;
		}

		applied.snapshot = buildSnapshot(applied);
		return applied;
	}

	private static void assignCapitals(Faction host, Faction rebels, LandSplitPlan plan, AppliedStart applied) {
		int rebelCapital = plan.rebelProvinceIds().get(0);
		rebels.setCapital(rebelCapital, true, false);
		applied.rebelCapital = rebelCapital;
		if (plan.rebelProvinceIds().contains(applied.hostOldCapital)) {
			applied.hostCapitalMoved = true;
			int loyalCapital = plan.loyalProvinceIds().isEmpty() ? -1 : plan.loyalProvinceIds().get(0);
			if (loyalCapital > 0) {
				host.setCapital(loyalCapital, true, false);
			}
		}
	}

	private static CivilWarSnapshot buildSnapshot(AppliedStart applied) {
		CivilWarSnapshot snapshot = new CivilWarSnapshot();
		snapshot.setHostFactionId(applied.host.getId());
		if (applied.tempRebels != null) {
			snapshot.setTempRebelFactionId(applied.tempRebels.getId());
		}
		Map<Integer, String> transferred = new LinkedHashMap<>();
		if (applied.plan != null) {
			for (int provinceId : applied.plan.rebelProvinceIds()) {
				transferred.put(provinceId, applied.host.getId());
			}
		}
		snapshot.setTransferredProvinces(transferred);
		snapshot.setWartimeVassalEnds(applied.vassalEnds);
		if (applied.hostCapitalMoved) {
			snapshot.setHostOldCapitalId(applied.hostOldCapital);
		}
		snapshot.setRebelCapitalId(applied.rebelCapital);
		return snapshot;
	}

	private static void endWartimeVassalage(Faction host, List<Faction> vassals, AppliedStart applied) {
		for (Faction vassal : vassals) {
			String typeId = null;
			Relation relation = vassal.getRelation(host.getId());
			if (relation != null && relation.getType() != null) {
				typeId = relation.getType().getId();
			}
			applied.vassalEnds.add(new CivilWarWartimeVassalEnd(vassal.getId(), host.getId(), typeId));
			RelationManager.endVassalage(vassal, host, false);
		}
	}

	private static void moveCitizenSupporters(Movement movement, Faction host, Faction rebels) {
		List<String> citizens = new ArrayList<>(movement.getSupporters().getCitizens());
		for (Cause cause : movement.getCauses()) {
			citizens.addAll(cause.getPool().getCitizens());
		}
		for (String citizen : citizens) {
			if (citizen == null) {
				continue;
			}
			if (host.getRelationToFaction(citizen) != Member.MEMBER) {
				continue;
			}
			host.getOrCreateMainGuild().kick(citizen);
			rebels.getOrCreateMainGuild().addMember(citizen);
		}
	}

	private static void applyChangeLeaderTarget(Movement movement, Faction host, Faction rebels) {
		Cause first = movement.getCauses().isEmpty() ? null : movement.getCauses().get(0);
		if (first == null || first.getAction() != Action.CHANGE_LEADER) {
			return;
		}
		Proposal proposal = first.getProposal();
		if (proposal == null || !proposal.hasTarget()) {
			return;
		}
		String target = proposal.getTarget();
		if (host.getRelationToFaction(target) != Member.MEMBER) {
			return;
		}
		host.getOrCreateMainGuild().kick(target);
		rebels.getOrCreateMainGuild().addMember(target);
		rebels.setLeader(target);
	}

	static Guild pickRebelMainGuild(Movement movement, List<Guild> hostRebelGuilds) {
		if (hostRebelGuilds == null || hostRebelGuilds.isEmpty()) {
			return null;
		}
		if (movement != null && movement.getLeader() != null) {
			for (Guild guild : hostRebelGuilds) {
				if (guild != null && guild.isMember(movement.getLeader())) {
					return guild;
				}
			}
		}
		Guild strongest = null;
		double best = Double.NEGATIVE_INFINITY;
		for (Guild guild : hostRebelGuilds) {
			double power = 0;
			if (guild != null && guild.getTradeBreakdown() != null) {
				power = guild.getTradeBreakdown().getTradePower();
			}
			if (strongest == null || power > best) {
				strongest = guild;
				best = power;
			}
		}
		return strongest;
	}

	static boolean isVassalMember(Faction host, String player) {
		if (host == null || player == null) {
			return false;
		}
		Member relation = host.getRelationToFaction(player);
		return relation == Member.VASSAL_LEADER || relation == Member.VASSAL_MEMBER;
	}

	private static void rollback(AppliedStart applied) {
		if (applied == null) {
			return;
		}
		if (applied.regimentMoves != null && !applied.regimentMoves.isEmpty()) {
			CivilWarRegimentSplitService.rollback(applied.host, applied.tempRebels, applied.regimentMoves);
		}
		if (applied.splitApplied && applied.tempRebels != null && applied.plan != null) {
			if (applied.hostCapitalMoved && applied.hostOldCapital > 0) {
				applied.host.setCapital(applied.hostOldCapital, true, false);
			}
			CivilWarLandSplitService.rollback(applied.host, applied.tempRebels, applied.plan);
		}
		for (CivilWarWartimeVassalEnd end : applied.vassalEnds) {
			Faction vassal = FactionManager.getByString(end.factionId());
			Faction overlord = FactionManager.getByString(end.formerOverlordId());
			RelationType type = end.relationTypeId() == null ? null : RelationLoader.getType(end.relationTypeId());
			if (vassal != null && overlord != null && type != null) {
				RelationManager.setRelationForced(type, vassal, overlord);
			}
		}
		if (applied.tempRebels != null) {
			try {
				FactionManager.deleteFaction(applied.tempRebels);
			} catch (Exception ignored) {
				FactionManager.factions.remove(applied.tempRebels);
			}
		}
	}

	private static final class AppliedStart {
		String error;
		Faction host;
		Faction tempRebels;
		Faction warLeader;
		List<Faction> extraAttackers = new ArrayList<>();
		LandSplitPlan plan;
		boolean splitApplied;
		int hostOldCapital;
		boolean hostCapitalMoved;
		Integer rebelCapital;
		List<CivilWarWartimeVassalEnd> vassalEnds = new ArrayList<>();
		CivilWarSnapshot snapshot;
		Map<String, Integer> regimentMoves = new LinkedHashMap<>();
	}
}