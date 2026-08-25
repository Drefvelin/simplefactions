package me.Plugins.SimpleFactions.War.campaign.raid;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.battle.campaign.CampaignWarbandBattleService;
import me.Plugins.SimpleFactions.War.battle.persistence.BattlePersistenceService;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;
import me.Plugins.SimpleFactions.War.battle.warband.Warband;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;
import me.Plugins.SimpleFactions.War.campaign.progression.CampaignCoalition;
import me.Plugins.SimpleFactions.War.campaign.runtime.BattleSideMembers;
import me.Plugins.SimpleFactions.War.core.Side;
import me.Plugins.SimpleFactions.War.core.War;

public final class CampaignRaidWarbandService {
	private static final String RAID_WARBAND_PREFIX = "campaign_raid_";
	private static final String ATTACKER_SUFFIX = "_atk";
	private static final String DEFENDER_SUFFIX = "_def";

	private CampaignRaidWarbandService() {}

	public static String attackerWarbandId(CampaignRaid raid) {
		if (raid == null || raid.getBattleDay() == null) {
			return null;
		}
		return raidWarbandId(raid.getWarId(), raid.getBattleDay().toString(), ATTACKER_SUFFIX);
	}

	public static String defenderWarbandId(CampaignRaid raid) {
		if (raid == null || raid.getBattleDay() == null) {
			return null;
		}
		return raidWarbandId(raid.getWarId(), raid.getBattleDay().toString(), DEFENDER_SUFFIX);
	}

	public static boolean isRaidWarband(Warband warband) {
		return warband != null && warband.getId() != null && warband.getId().startsWith(RAID_WARBAND_PREFIX);
	}

	public static void createRaidWarbands(War war, CampaignRaid raid) {
		if (war == null || raid == null) {
			return;
		}
		ensureWarband(war, raid, true);
		ensureWarband(war, raid, false);
	}

	public static Warband getAttackerWarband(CampaignRaid raid) {
		String id = attackerWarbandId(raid);
		return id != null ? WarbandManager.getByString(id) : null;
	}

	public static Warband getDefenderWarband(CampaignRaid raid) {
		String id = defenderWarbandId(raid);
		return id != null ? WarbandManager.getByString(id) : null;
	}

	public static void signupAttacker(War war, CampaignRaid raid, UUID playerId, String playerName) {
		if (war == null || raid == null || playerId == null || playerName == null) {
			return;
		}
		createRaidWarbands(war, raid);
		Warband warband = getAttackerWarband(raid);
		if (warband == null || warband.hasMember(playerId)) {
			return;
		}
		applyRaidLeaderRules(war, warband, playerId, playerName);
		warband.addMember(playerId);
	}

	public static void signupDefender(War war, CampaignRaid raid, UUID playerId, String playerName) {
		if (war == null || raid == null || playerId == null || playerName == null) {
			return;
		}
		createRaidWarbands(war, raid);
		Warband warband = getDefenderWarband(raid);
		if (warband == null || warband.hasMember(playerId)) {
			return;
		}
		applyRaidLeaderRules(war, warband, playerId, playerName);
		warband.addMember(playerId);
	}

	public static void enrollOnlineDefenders(War war, CampaignRaid raid) {
		if (war == null || raid == null) {
			return;
		}
		createRaidWarbands(war, raid);
		Side defenderSide = coalitionSide(war, raid.getAttackerCoalition() != null
				? raid.getAttackerCoalition().opposing()
				: null);
		if (defenderSide == null) {
			return;
		}
		for (String memberName : BattleSideMembers.collectEligibleMemberNames(defenderSide)) {
			Player player = Bukkit.getPlayerExact(memberName);
			if (player == null || !player.isOnline()) {
				continue;
			}
			if (WarbandManager.getByMemberId(player.getUniqueId()) != null) {
				continue;
			}
			signupDefender(war, raid, player.getUniqueId(), player.getName());
		}
	}

	public static void tryEnrollDefenderOnLogin(Player player) {
		if (player == null) {
			return;
		}
		Faction faction = FactionManager.getByMember(player.getName());
		if (faction == null) {
			faction = FactionManager.getByLeader(player.getName());
		}
		if (faction == null || WarbandManager.getByMemberId(player.getUniqueId()) != null) {
			return;
		}
		for (War war : WarManager.getActive()) {
			if (!war.isActive() || war.getSide(faction) == null) {
				continue;
			}
			CampaignRaid raid = CampaignRaidService.getActive(war);
			if (raid == null || raid.getState() != CampaignRaidState.FIGHTING) {
				continue;
			}
			CampaignCoalition playerCoalition = CampaignRaidService.coalitionForFaction(war, faction);
			CampaignCoalition defendingCoalition = raid.getAttackerCoalition() != null
					? raid.getAttackerCoalition().opposing()
					: null;
			if (playerCoalition != defendingCoalition) {
				continue;
			}
			signupDefender(war, raid, player.getUniqueId(), player.getName());
			return;
		}
	}

	public static void promoteLeaderIfNeeded(Warband warband) {
		if (warband == null || !isRaidWarband(warband)) {
			return;
		}
		if (!warband.isPendingLeader() && warband.getRealMemberCount() > 0) {
			UUID currentLeader = warband.getLeaderId();
			if (currentLeader != null && warband.hasMember(currentLeader)) {
				return;
			}
		}
		UUID nextLeader = warband.getOldestRealMemberId(null);
		if (nextLeader != null) {
			warband.setLeaderId(nextLeader);
		} else {
			warband.resetToPendingLeader();
		}
	}

	public static void destroyRaidWarbands(War war, CampaignRaid raid) {
		if (raid == null) {
			return;
		}
		deleteIfPresent(attackerWarbandId(raid));
		deleteIfPresent(defenderWarbandId(raid));
	}

	private static void ensureWarband(War war, CampaignRaid raid, boolean attacker) {
		String id = attacker ? attackerWarbandId(raid) : defenderWarbandId(raid);
		if (id == null || WarbandManager.getByString(id) != null) {
			return;
		}
		CampaignCoalition coalition = attacker
				? raid.getAttackerCoalition()
				: (raid.getAttackerCoalition() != null ? raid.getAttackerCoalition().opposing() : null);
		Side side = coalitionSide(war, coalition);
		if (side == null) {
			return;
		}
		String campaignSideId = campaignSideIdForCoalition(coalition);
		Warband warband = Warband.createRaidShell(id, side, campaignSideId);
		WarbandManager.addWarband(warband);
	}

	private static void applyRaidLeaderRules(War war, Warband warband, UUID playerId, String playerName) {
		if (warband.isPendingLeader()) {
			warband.setLeaderId(playerId);
			return;
		}
		if (CampaignWarbandBattleService.isWarSideMainLeader(war, warband, playerName)) {
			warband.setLeaderId(playerId);
		}
	}

	private static String campaignSideIdForCoalition(CampaignCoalition coalition) {
		return coalition == CampaignCoalition.AGGRESSOR
				? BattleTemplate.ATTACKER_SIDE
				: BattleTemplate.DEFENDER_SIDE;
	}

	private static Side coalitionSide(War war, CampaignCoalition coalition) {
		if (war == null || coalition == null) {
			return null;
		}
		return coalition == CampaignCoalition.AGGRESSOR ? war.getAttackers() : war.getDefenders();
	}

	private static String raidWarbandId(int warId, String battleDay, String suffix) {
		return RAID_WARBAND_PREFIX + warId + "_" + battleDay + suffix;
	}

	private static void deleteIfPresent(String warbandId) {
		if (warbandId == null) {
			return;
		}
		Warband warband = WarbandManager.getByString(warbandId);
		if (warband != null) {
			BattlePersistenceService.deleteWarband(warband);
		}
	}
}
