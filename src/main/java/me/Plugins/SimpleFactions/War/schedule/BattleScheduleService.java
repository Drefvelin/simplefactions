package me.Plugins.SimpleFactions.War.schedule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.Side;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.campaign.CampaignBattleLaunchService;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.progression.BelligerentRole;
import me.Plugins.SimpleFactions.War.progression.CampaignProgressionService;

public final class BattleScheduleService {
	private BattleScheduleService() {}

	public static int battleDayHour(Instant now) {
		if (now == null) {
			return 0;
		}
		return now.atZone(ZoneOffset.UTC).getHour();
	}

	public static LocalDate battleDayDate(Instant now) {
		if (now == null) {
			return null;
		}
		return now.atZone(ZoneOffset.UTC).toLocalDate();
	}

	public static boolean isOnBattleDay(War war, Instant now) {
		if (war == null || war.getBattleDay() == null || now == null) {
			return false;
		}
		return war.getBattleDay().equals(battleDayDate(now));
	}

	public static boolean isDefenderChoiceDeadlineDue(War war, Instant now) {
		return isOnBattleDay(war, now) && battleDayHour(now) >= Cache.warDefenderChoiceDeadlineHour;
	}

	public static boolean isVoteCloseDue(War war, Instant now) {
		return isOnBattleDay(war, now) && battleDayHour(now) >= Cache.warVoteCloseHour;
	}

	public static boolean isBeforeVoteClose(War war, Instant now) {
		return isOnBattleDay(war, now) && battleDayHour(now) < Cache.warVoteCloseHour;
	}

	public static boolean needsDefenderChoice(War war) {
		if (war == null || !war.isActive()) {
			return false;
		}
		return CampaignProgressionService.isAttackerInitiativeExhausted(war)
				&& war.getCampaignPhase() == CampaignPhase.INVASION
				&& CampaignProgressionService.resolveNextBattleNodes(war).size() == 2;
	}

	public static boolean isDefenderChoiceResolved(War war) {
		if (!needsDefenderChoice(war)) {
			return true;
		}
		return war.isDefenderChoiceResolved();
	}

	public static boolean applyDefenderChoiceDeadline(War war, Instant now) {
		if (war == null || !war.isActive() || !isDefenderChoiceDeadlineDue(war, now)) {
			return false;
		}
		if (!needsDefenderChoice(war) || isDefenderChoiceResolved(war)) {
			return false;
		}
		return CampaignProgressionService.applyDefenderHold(war);
	}

	public static void enterAutoresolvePending(War war) {
		if (war == null) {
			return;
		}
		clearScheduledTargets(war);
		war.setBattleSchedulePhase(BattleSchedulePhase.AUTORESOLVE_PENDING);
		war.setAutoresolveProposedByAttacker(false);
		war.setAutoresolveProposedByDefender(false);
	}

	public static void openVote(War war) {
		if (war == null) {
			return;
		}
		war.setBattleSchedulePhase(BattleSchedulePhase.VOTING);
		clearScheduledTargets(war);
		war.setDefenderChoiceResolved(false);
	}

	public static BattleScheduleCloseResult closeVote(
			War war,
			Instant now,
			Function<UUID, Faction> uuidToFaction,
			Function<String, UUID> memberNameToUuid) {
		return closeVote(war, now, uuidToFaction, memberNameToUuid, CloseVoteOptions.scheduled());
	}

	public static BattleScheduleCloseResult closeVote(
			War war,
			Instant now,
			Function<UUID, Faction> uuidToFaction,
			Function<String, UUID> memberNameToUuid,
			CloseVoteOptions options) {
		if (war == null || !war.isActive() || uuidToFaction == null || memberNameToUuid == null) {
			return BattleScheduleCloseResult.SKIPPED;
		}
		if (options == null) {
			options = CloseVoteOptions.scheduled();
		}
		if (war.getBattleSchedulePhase() != BattleSchedulePhase.VOTING) {
			return BattleScheduleCloseResult.SKIPPED;
		}
		if (!options.forceImmediate() && !isVoteCloseDue(war, now)) {
			return BattleScheduleCloseResult.SKIPPED;
		}

		if (needsDefenderChoice(war) && !isDefenderChoiceResolved(war)) {
			applyDefenderChoiceDeadline(war, now);
			if (!isDefenderChoiceResolved(war)) {
				return BattleScheduleCloseResult.BLOCKED_DEFENDER_CHOICE;
			}
		}

		boolean forceQuorum = options.forceQuorum() || war.isForceQuorumNextClose();
		clearForceQuorumNextClose(war);

		boolean quorumPassed = forceQuorum
				|| BattleQuorumService.meetsQuorum(war, memberNameToUuid).passed();
		if (quorumPassed && scheduleFromVotes(war, uuidToFaction)) {
			return BattleScheduleCloseResult.SCHEDULED;
		}

		postpone(war);
		return BattleScheduleCloseResult.POSTPONED;
	}

	public static boolean scheduleFromVotes(War war, Function<UUID, Faction> uuidToFaction) {
		if (war == null || uuidToFaction == null || war.getBattleDay() == null) {
			return false;
		}

		OptionalInt pickedHour = BattleVoteService.pickHour(war, uuidToFaction);
		if (pickedHour.isEmpty()) {
			return false;
		}

		Integer provinceId = resolveScheduledProvinceId(war);
		if (provinceId == null) {
			return false;
		}

		int hour = pickedHour.getAsInt();
		war.setScheduledBattleHour(hour);
		war.setScheduledBattleAt(BattleWindowService.computeScheduledBattleAt(war.getBattleDay(), hour));
		war.setScheduledBattleProvinceId(provinceId);
		war.setBattleSchedulePhase(BattleSchedulePhase.SCHEDULED);
		if (war.getScheduledBattleAt() != null) {
			CampaignBattleLaunchService.prepareScheduledBattle(war);
		}
		return war.getScheduledBattleAt() != null;
	}

	public static boolean applyScheduledInstant(War war, Instant scheduledAt) {
		if (war == null || scheduledAt == null) {
			return false;
		}

		Integer provinceId = resolveScheduledProvinceId(war);
		if (provinceId == null) {
			return false;
		}

		int hour = scheduledAt.atZone(ZoneOffset.UTC).getHour();
		if (!BattleWindowService.isValidHour(hour)) {
			return false;
		}

		war.setBattleDay(scheduledAt.atZone(ZoneOffset.UTC).toLocalDate());
		war.setScheduledBattleHour(hour);
		war.setScheduledBattleAt(scheduledAt);
		war.setScheduledBattleProvinceId(provinceId);
		war.setBattleSchedulePhase(BattleSchedulePhase.SCHEDULED);
		CampaignBattleLaunchService.prepareScheduledBattle(war);
		return true;
	}

	public static int castSpoofVotes(
			War war,
			int hour,
			Function<String, UUID> memberNameToUuid,
			BelligerentRole... sides) {
		if (war == null || memberNameToUuid == null || sides == null || sides.length == 0) {
			return 0;
		}
		if (!BattleWindowService.isValidHour(hour)) {
			return 0;
		}

		int added = 0;
		for (BelligerentRole side : sides) {
			Side belligerentSide = side == BelligerentRole.ATTACKER ? war.getAttackers() : war.getDefenders();
			if (belligerentSide == null) {
				continue;
			}
			for (String memberName : BattleSideMembers.collectEligibleMemberNames(belligerentSide)) {
				UUID playerId = memberNameToUuid.apply(memberName);
				if (playerId == null) {
					continue;
				}
				Set<Integer> selections = war.getBattleVotes().computeIfAbsent(playerId, ignored -> new HashSet<>());
				if (selections.add(hour)) {
					added++;
				}
			}
		}
		return added;
	}

	public static void postpone(War war) {
		if (war == null || war.getBattleDay() == null) {
			return;
		}

		CampaignProgressionService.applyPostponedBattle(war);
		war.setBattleDay(war.getBattleDay().plusDays(1));
		war.setBattleSchedulePhase(BattleSchedulePhase.VOTING);
		clearScheduledTargets(war);
		war.setPostponementsThisCycle(war.getPostponementsThisCycle() + 1);
		war.setDefenderChoiceResolved(false);
	}

	public static void skipBattleDay(War war) {
		if (war == null || war.getBattleDay() == null) {
			return;
		}
		war.setBattleDay(war.getBattleDay().plusDays(1));
	}

	public static Integer resolveScheduledProvinceId(War war) {
		List<Integer> nodes = CampaignProgressionService.resolveNextBattleNodes(war);
		if (nodes.isEmpty()) {
			return null;
		}
		if (nodes.size() == 1) {
			return nodes.get(0);
		}
		if (nodes.size() == 2 && war.isDefenderChoiceResolved()) {
			return nodes.get(0);
		}
		return null;
	}

	private static void clearForceQuorumNextClose(War war) {
		if (war != null && war.isForceQuorumNextClose()) {
			war.setForceQuorumNextClose(false);
		}
	}

	private static void clearScheduledTargets(War war) {
		war.setScheduledBattleAt(null);
		war.setScheduledBattleHour(0);
		war.setScheduledBattleProvinceId(null);
	}
}
