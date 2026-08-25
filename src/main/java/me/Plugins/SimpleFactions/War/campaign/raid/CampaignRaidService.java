package me.Plugins.SimpleFactions.War.campaign.raid;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.campaign.progression.CampaignCoalition;
import me.Plugins.SimpleFactions.War.campaign.runtime.BattleScheduleService;
import me.Plugins.SimpleFactions.War.campaign.raid.CampaignRaidResults.LaunchResult;
import me.Plugins.SimpleFactions.War.campaign.raid.CampaignRaidResults.ValidateLaunchOutcome;
import me.Plugins.SimpleFactions.War.campaign.raid.CampaignRaidResults.ValidateLaunchResult;
import me.Plugins.SimpleFactions.War.campaign.raid.CampaignRaidResults.TransitionResult;
import me.Plugins.SimpleFactions.War.core.Side;
import me.Plugins.SimpleFactions.War.core.War;

public final class CampaignRaidService {
	private CampaignRaidService() {}

	public static void syncBattleDay(War war) {
		if (war == null) {
			return;
		}
		LocalDate battleDay = war.getBattleDay();
		CampaignRaid active = war.getActiveCampaignRaid();
		if (active != null && battleDay != null && active.getBattleDay() != null
				&& !active.getBattleDay().equals(battleDay)) {
			clearActiveRaid(war);
		}
		if (battleDay == null) {
			war.getCampaignRaidsUsed().clear();
			return;
		}
		String battleDayKey = battleDay.toString();
		war.getCampaignRaidsUsed().entrySet().removeIf(entry ->
				entry.getValue() == null || !battleDayKey.equals(entry.getValue()));
	}

	public static LaunchResult canLaunch(War war, Faction faction, Instant now) {
		if (war == null || !war.isActive() || faction == null || now == null) {
			return LaunchResult.REJECTED_WAR_INACTIVE;
		}
		syncBattleDay(war);
		if (war.getBattleDay() == null) {
			return LaunchResult.REJECTED_WAR_INACTIVE;
		}
		if (war.getSide(faction) == null) {
			return LaunchResult.REJECTED_NOT_PARTICIPANT;
		}
		if (!BattleScheduleService.isRaidWindowOpen(war, now)) {
			return LaunchResult.REJECTED_OUTSIDE_WINDOW;
		}
		if (war.getActiveCampaignRaid() != null) {
			return LaunchResult.REJECTED_RAID_IN_PROGRESS;
		}
		CampaignCoalition coalition = coalitionForFaction(war, faction);
		if (coalition == null || isSideQuotaUsed(war, coalition)) {
			return LaunchResult.REJECTED_QUOTA_SPENT;
		}
		return LaunchResult.STARTED;
	}

	public static LaunchResult beginMuster(
			War war,
			Faction faction,
			String sourceInstallationId,
			String targetInstallationId,
			Instant now) {
		if (faction == null || faction.getId() == null) {
			return LaunchResult.REJECTED_NOT_PARTICIPANT;
		}
		ValidateLaunchOutcome outcome = CampaignRaidEligibilityService.validateLaunch(
				war, faction.getId(), sourceInstallationId, targetInstallationId, now);
		LaunchResult gate = mapValidateLaunchResult(outcome.result());
		if (gate != LaunchResult.STARTED) {
			return gate;
		}

		CampaignCoalition coalition = coalitionForFaction(war, faction);
		if (coalition == null) {
			return LaunchResult.REJECTED_NOT_PARTICIPANT;
		}

		LocalDate battleDay = war.getBattleDay();
		CampaignRaid raid = new CampaignRaid();
		raid.setId(CampaignRaid.buildId(war.getId(), battleDay));
		raid.setWarId(war.getId());
		raid.setBattleDay(battleDay);
		raid.setAttackerCoalition(coalition);
		raid.setLauncherFactionId(faction.getId());
		raid.setSourceInstallationId(sourceInstallationId);
		raid.setTargetInstallationId(targetInstallationId);
		raid.setRaidKind(outcome.raidKind());
		raid.setState(CampaignRaidState.MUSTER);
		raid.setMusterEndsAt(now.plusSeconds(Cache.campaignRaidMusterSeconds));
		war.setActiveCampaignRaid(raid);
		CampaignRaidWarbandService.createRaidWarbands(war, raid);
		CampaignRaidMusterScheduler.onMusterStarted(war, now);
		return LaunchResult.STARTED;
	}

	public static CampaignRaid getActive(War war) {
		if (war == null) {
			return null;
		}
		syncBattleDay(war);
		return war.getActiveCampaignRaid();
	}

	public static TransitionResult transitionToFighting(War war, Instant now) {
		if (war == null || now == null) {
			return TransitionResult.REJECTED_NO_ACTIVE_RAID;
		}
		syncBattleDay(war);
		CampaignRaid raid = war.getActiveCampaignRaid();
		if (raid == null) {
			return TransitionResult.REJECTED_NO_ACTIVE_RAID;
		}
		if (raid.getState() != CampaignRaidState.MUSTER) {
			return TransitionResult.REJECTED_WRONG_STATE;
		}
		raid.setState(CampaignRaidState.FIGHTING);
		raid.setFightEndsAt(now.plusSeconds(Cache.campaignRaidDurationSeconds));
		if (raid.getAttackerCoalition() != null && raid.getBattleDay() != null) {
			war.getCampaignRaidsUsed().put(
					raid.getAttackerCoalition().toJson(),
					raid.getBattleDay().toString());
		}
		return TransitionResult.OK;
	}

	public static void endRaid(War war, Instant now) {
		if (war == null) {
			return;
		}
		syncBattleDay(war);
		CampaignRaid raid = war.getActiveCampaignRaid();
		if (raid != null) {
			CampaignRaidWarbandService.destroyRaidWarbands(war, raid);
			raid.setState(CampaignRaidState.ENDED);
		}
		clearActiveRaid(war);
	}

	public static void clearForNewBattleDay(War war) {
		if (war == null) {
			return;
		}
		CampaignRaid raid = war.getActiveCampaignRaid();
		if (raid != null) {
			CampaignRaidWarbandService.destroyRaidWarbands(war, raid);
		}
		clearActiveRaid(war);
		war.getCampaignRaidsUsed().clear();
	}

	public static boolean isSideQuotaUsed(War war, CampaignCoalition coalition) {
		if (war == null || coalition == null || war.getBattleDay() == null) {
			return false;
		}
		syncBattleDay(war);
		String usedDay = war.getCampaignRaidsUsed().get(coalition.toJson());
		return usedDay != null && usedDay.equals(war.getBattleDay().toString());
	}

	public static void setRepairLockUntil(War war, String installationId, Instant until) {
		if (war == null || installationId == null || installationId.isBlank() || until == null) {
			return;
		}
		war.getRaidRepairLockUntil().put(installationId, until);
	}

	public static boolean isRepairLocked(War war, String installationId, Instant now) {
		if (war == null || installationId == null || installationId.isBlank() || now == null) {
			return false;
		}
		Instant until = war.getRaidRepairLockUntil().get(installationId);
		return until != null && now.isBefore(until);
	}

	public static Instant repairLockUntilFromStart(Instant fightStart) {
		if (fightStart == null) {
			return null;
		}
		return fightStart.plus(Cache.campaignRaidRepairLockHours, ChronoUnit.HOURS);
	}

	private static void clearActiveRaid(War war) {
		war.setActiveCampaignRaid(null);
	}

	public static CampaignCoalition coalitionForFaction(War war, Faction faction) {
		Side side = war.getSide(faction);
		if (side == null) {
			return null;
		}
		if (side == war.getAttackers()) {
			return CampaignCoalition.AGGRESSOR;
		}
		if (side == war.getDefenders()) {
			return CampaignCoalition.DEFENDER;
		}
		return null;
	}

	private static LaunchResult mapValidateLaunchResult(ValidateLaunchResult result) {
		if (result == null) {
			return LaunchResult.REJECTED_WAR_INACTIVE;
		}
		return switch (result) {
			case OK -> LaunchResult.STARTED;
			case REJECTED_WAR_INACTIVE -> LaunchResult.REJECTED_WAR_INACTIVE;
			case REJECTED_NOT_PARTICIPANT -> LaunchResult.REJECTED_NOT_PARTICIPANT;
			case REJECTED_OUTSIDE_WINDOW -> LaunchResult.REJECTED_OUTSIDE_WINDOW;
			case REJECTED_QUOTA_SPENT -> LaunchResult.REJECTED_QUOTA_SPENT;
			case REJECTED_RAID_IN_PROGRESS -> LaunchResult.REJECTED_RAID_IN_PROGRESS;
			case REJECTED_INVALID_SOURCE, REJECTED_INVALID_TARGET, REJECTED_KIND_MISMATCH ->
					LaunchResult.REJECTED_INVALID_INPUT;
		};
	}
}
