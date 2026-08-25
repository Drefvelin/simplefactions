package me.Plugins.SimpleFactions.War.campaign.raid;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.campaign.raid.CampaignRaidResults.JoinResult;
import me.Plugins.SimpleFactions.War.campaign.raid.CampaignRaidResults.LaunchResult;
import me.Plugins.SimpleFactions.War.campaign.raid.CampaignRaidResults.ValidateLaunchResult;
import me.Plugins.SimpleFactions.installation.Installation;

public final class CampaignRaidMessages {
	public static final String NOT_LEADER = "§cOnly your faction leader can launch a campaign raid.";
	public static final String OUTSIDE_WINDOW =
			"§cCampaign raids can only be called between 19:00 and 20:00 on battle day.";
	public static final String SIDE_QUOTA_SPENT =
			"§cYour coalition has already launched its raid for this battle day.";
	public static final String RAID_IN_PROGRESS = "§cAnother campaign raid is already in progress.";
	public static final String INVALID_SOURCE = "§cThat installation cannot be used as a raid source.";
	public static final String INVALID_TARGET = "§cThat installation cannot be targeted.";
	public static final String KIND_MISMATCH =
			"§cSource and target installation types do not match for a campaign raid.";
	public static final String WAR_INACTIVE = "§cWar not found.";
	public static final String NOT_PARTICIPANT = "§cYou are not a belligerent in this war.";
	public static final String INVALID_INPUT = "§cCould not launch campaign raid.";
	public static final String IN_WARBAND = "§cLeave your warband before joining a campaign raid.";
	public static final String NOT_MUSTER = "§cCampaign raid muster has ended.";
	public static final String NOT_ATTACKER_COALITION = "§cOnly the attacking coalition can join this raid.";
	public static final String RAID_NOT_FOUND = "§cNo active campaign raid with that id.";
	public static final String JOINED = "§aJoined the campaign raid muster.";
	public static final String ALREADY_JOINED = "§7You are already in this campaign raid muster.";

	private CampaignRaidMessages() {}

	public static String messageForLaunchResult(LaunchResult result) {
		if (result == null || result == LaunchResult.STARTED) {
			return null;
		}
		return switch (result) {
			case REJECTED_WAR_INACTIVE -> WAR_INACTIVE;
			case REJECTED_NOT_PARTICIPANT -> NOT_PARTICIPANT;
			case REJECTED_OUTSIDE_WINDOW -> OUTSIDE_WINDOW;
			case REJECTED_QUOTA_SPENT -> SIDE_QUOTA_SPENT;
			case REJECTED_RAID_IN_PROGRESS -> RAID_IN_PROGRESS;
			case REJECTED_INVALID_INPUT -> INVALID_INPUT;
			default -> null;
		};
	}

	public static String messageForValidateResult(ValidateLaunchResult result) {
		if (result == null || result == ValidateLaunchResult.OK) {
			return null;
		}
		return switch (result) {
			case REJECTED_WAR_INACTIVE -> WAR_INACTIVE;
			case REJECTED_NOT_PARTICIPANT -> NOT_PARTICIPANT;
			case REJECTED_OUTSIDE_WINDOW -> OUTSIDE_WINDOW;
			case REJECTED_QUOTA_SPENT -> SIDE_QUOTA_SPENT;
			case REJECTED_RAID_IN_PROGRESS -> RAID_IN_PROGRESS;
			case REJECTED_INVALID_SOURCE -> INVALID_SOURCE;
			case REJECTED_INVALID_TARGET -> INVALID_TARGET;
			case REJECTED_KIND_MISMATCH -> KIND_MISMATCH;
			default -> null;
		};
	}

	public static String buildRaidCalledMessage(Faction launcher, Installation target, String raidId) {
		String factionName = launcher != null ? launcher.getName() : "A faction";
		String targetName = target != null ? target.getName() : "an installation";
		String id = raidId != null ? raidId : "";
		return "§e" + factionName + " raid called on §c" + targetName + "§e! §7/raid join " + id + " §e(60s)";
	}

	public static String buildRaidStartedMessage(Installation target) {
		String targetName = target != null ? target.getName() : "an installation";
		return "§cCampaign raid underway at §e" + targetName + "§c!";
	}

	public static String buildRaidEndedMessage(Installation target) {
		String targetName = target != null ? target.getName() : "an installation";
		return "§7Campaign raid at §e" + targetName + " §7has ended.";
	}

	public static String messageForJoinResult(JoinResult result) {
		if (result == null || result == JoinResult.OK) {
			return null;
		}
		return switch (result) {
			case REJECTED_RAID_NOT_FOUND -> RAID_NOT_FOUND;
			case REJECTED_NOT_PARTICIPANT -> NOT_PARTICIPANT;
			case REJECTED_NOT_ATTACKER_COALITION -> NOT_ATTACKER_COALITION;
			case REJECTED_NOT_MUSTER -> NOT_MUSTER;
			case REJECTED_IN_WARBAND -> IN_WARBAND;
			case REJECTED_ALREADY_JOINED -> ALREADY_JOINED;
			default -> null;
		};
	}
}
