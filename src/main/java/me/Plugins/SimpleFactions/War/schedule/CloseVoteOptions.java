package me.Plugins.SimpleFactions.War.schedule;

public record CloseVoteOptions(boolean forceImmediate, boolean forceQuorum) {
	public static CloseVoteOptions scheduled() {
		return new CloseVoteOptions(false, false);
	}

	public static CloseVoteOptions admin(boolean forceQuorum) {
		return new CloseVoteOptions(true, forceQuorum);
	}
}
