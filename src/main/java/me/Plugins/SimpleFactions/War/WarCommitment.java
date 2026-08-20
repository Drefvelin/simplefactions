package me.Plugins.SimpleFactions.War;

import java.time.Instant;

public record WarCommitment(
		int warId,
		String factionId,
		String regimentId,
		int count,
		Instant committedAt) {}
