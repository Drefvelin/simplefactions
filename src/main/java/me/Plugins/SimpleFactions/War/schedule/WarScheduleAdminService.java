package me.Plugins.SimpleFactions.War.schedule;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.progression.BelligerentRole;

public final class WarScheduleAdminService {
	private WarScheduleAdminService() {}

	public static WarScheduleAdminResult openVote(War war) {
		if (war == null) {
			return WarScheduleAdminResult.error("War not found.");
		}
		if (!war.isActive()) {
			return WarScheduleAdminResult.error("War is not active.");
		}
		BattleScheduleService.openVote(war);
		return WarScheduleAdminResult.ok("Opened voting for war " + war.getId() + ".");
	}

	public static WarScheduleAdminResult closeVote(War war, Instant now) {
		if (war == null) {
			return WarScheduleAdminResult.error("War not found.");
		}
		if (!war.isActive()) {
			return WarScheduleAdminResult.error("War is not active.");
		}
		if (war.getBattleSchedulePhase() != BattleSchedulePhase.VOTING) {
			return WarScheduleAdminResult.error("War is not in VOTING phase.");
		}

		BattleScheduleCloseResult result = BattleScheduleService.closeVote(
				war,
				now,
				BattleScheduleLookups.uuidToFactionForWar(war),
				BattleScheduleLookups.memberNameToUuid(),
				CloseVoteOptions.admin(war.isForceQuorumNextClose()));

		return switch (result) {
			case SCHEDULED -> WarScheduleAdminResult.ok(
					"Vote close scheduled battle at "
							+ war.getScheduledBattleAt()
							+ " (province "
							+ war.getScheduledBattleProvinceId()
							+ ").");
			case POSTPONED -> WarScheduleAdminResult.ok(
					"Vote close postponed to battle day " + war.getBattleDay() + ".");
			case AUTORESOLVE_PENDING -> WarScheduleAdminResult.ok("Vote close set AUTORESOLVE_PENDING.");
			case BLOCKED_DEFENDER_CHOICE -> WarScheduleAdminResult.error(
					"Vote close blocked: defender choice unresolved.");
			case SKIPPED -> WarScheduleAdminResult.error("Vote close skipped.");
		};
	}

	public static WarScheduleAdminResult skipDay(War war) {
		if (war == null) {
			return WarScheduleAdminResult.error("War not found.");
		}
		if (!war.isActive()) {
			return WarScheduleAdminResult.error("War is not active.");
		}
		if (war.getBattleDay() == null) {
			return WarScheduleAdminResult.error("War has no battle day.");
		}
		BattleScheduleService.skipBattleDay(war);
		return WarScheduleAdminResult.ok("Skipped to battle day " + war.getBattleDay() + ".");
	}

	public static WarScheduleAdminResult castVote(War war, int hour, String sideArg) {
		if (war == null) {
			return WarScheduleAdminResult.error("War not found.");
		}
		if (!war.isActive()) {
			return WarScheduleAdminResult.error("War is not active.");
		}
		if (!BattleWindowService.isValidHour(hour)) {
			return WarScheduleAdminResult.error("Hour must be within the battle window.");
		}

		BelligerentRole[] sides = parseSideArg(sideArg);
		if (sides.length == 0) {
			return WarScheduleAdminResult.error("Side must be attacker, defender, or both.");
		}

		int added = BattleScheduleService.castSpoofVotes(
				war,
				hour,
				BattleScheduleLookups.spoofMemberNameToUuid(),
				sides);
		return WarScheduleAdminResult.ok(
				"Cast spoof votes for hour "
						+ hour
						+ " ("
						+ added
						+ " selections, "
						+ BattleQuorumService.countDistinctVoters(war)
						+ " voters total).");
	}

	public static WarScheduleAdminResult forceQuorum(War war) {
		if (war == null) {
			return WarScheduleAdminResult.error("War not found.");
		}
		if (!war.isActive()) {
			return WarScheduleAdminResult.error("War is not active.");
		}
		war.setForceQuorumNextClose(true);
		return WarScheduleAdminResult.ok("Next vote close will bypass quorum for war " + war.getId() + ".");
	}

	public static WarScheduleAdminResult setScheduled(War war, String isoInstant) {
		if (war == null) {
			return WarScheduleAdminResult.error("War not found.");
		}
		if (!war.isActive()) {
			return WarScheduleAdminResult.error("War is not active.");
		}
		if (isoInstant == null || isoInstant.isBlank()) {
			return WarScheduleAdminResult.error("Instant must be ISO-8601 (e.g. 2026-08-21T21:00:00Z).");
		}

		Instant scheduledAt;
		try {
			scheduledAt = Instant.parse(isoInstant.trim());
		} catch (DateTimeParseException e) {
			return WarScheduleAdminResult.error("Could not parse instant: " + isoInstant);
		}

		if (!BattleScheduleService.applyScheduledInstant(war, scheduledAt)) {
			return WarScheduleAdminResult.error(
					"Could not set scheduled battle (invalid hour or no battle province).");
		}

		return WarScheduleAdminResult.ok(
				"Set scheduled battle at "
						+ war.getScheduledBattleAt()
						+ " (province "
						+ war.getScheduledBattleProvinceId()
						+ ").");
	}

	private static BelligerentRole[] parseSideArg(String sideArg) {
		if (sideArg == null || sideArg.isBlank() || sideArg.equalsIgnoreCase("both")) {
			return new BelligerentRole[] {BelligerentRole.ATTACKER, BelligerentRole.DEFENDER};
		}
		if (sideArg.equalsIgnoreCase("attacker")) {
			return new BelligerentRole[] {BelligerentRole.ATTACKER};
		}
		if (sideArg.equalsIgnoreCase("defender")) {
			return new BelligerentRole[] {BelligerentRole.DEFENDER};
		}
		return new BelligerentRole[0];
	}
}
