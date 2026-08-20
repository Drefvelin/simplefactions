package me.Plugins.SimpleFactions.War.schedule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Function;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
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

	public static boolean proposeAutoresolve(War war, BelligerentRole side) {
		if (war == null || !war.isActive() || side == null) {
			return false;
		}
		return switch (side) {
			case ATTACKER -> {
				war.setAutoresolveProposedByAttacker(true);
				yield true;
			}
			case DEFENDER -> {
				war.setAutoresolveProposedByDefender(true);
				yield true;
			}
		};
	}

	public static boolean isAutoresolveReady(War war) {
		return war != null
				&& war.isActive()
				&& war.isAutoresolveProposedByAttacker()
				&& war.isAutoresolveProposedByDefender();
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

	public static BattleScheduleCloseResult closeVote(
			War war,
			Instant now,
			Function<UUID, Faction> uuidToFaction,
			Function<String, UUID> memberNameToUuid) {
		if (war == null || !war.isActive() || uuidToFaction == null || memberNameToUuid == null) {
			return BattleScheduleCloseResult.SKIPPED;
		}
		if (war.getBattleSchedulePhase() != BattleSchedulePhase.VOTING || !isVoteCloseDue(war, now)) {
			return BattleScheduleCloseResult.SKIPPED;
		}

		if (needsDefenderChoice(war) && !isDefenderChoiceResolved(war)) {
			applyDefenderChoiceDeadline(war, now);
			if (!isDefenderChoiceResolved(war)) {
				return BattleScheduleCloseResult.BLOCKED_DEFENDER_CHOICE;
			}
		}

		if (isAutoresolveReady(war)) {
			enterAutoresolvePending(war);
			return BattleScheduleCloseResult.AUTORESOLVE_PENDING;
		}

		QuorumResult quorum = BattleQuorumService.meetsQuorum(war, memberNameToUuid);
		if (quorum.passed() && scheduleFromVotes(war, uuidToFaction)) {
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
		return war.getScheduledBattleAt() != null;
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

	private static Integer resolveScheduledProvinceId(War war) {
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

	private static void clearScheduledTargets(War war) {
		war.setScheduledBattleAt(null);
		war.setScheduledBattleHour(0);
		war.setScheduledBattleProvinceId(null);
	}
}
