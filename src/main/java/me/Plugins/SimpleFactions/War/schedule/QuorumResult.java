package me.Plugins.SimpleFactions.War.schedule;

public record QuorumResult(
		boolean passed,
		boolean passMinPlayers,
		boolean passSmallestSideFull,
		int distinctVoters,
		int attackerEligible,
		int defenderEligible) {
}
