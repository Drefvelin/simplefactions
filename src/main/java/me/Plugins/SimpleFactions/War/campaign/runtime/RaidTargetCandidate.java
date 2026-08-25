package me.Plugins.SimpleFactions.War.campaign.runtime;

import me.Plugins.SimpleFactions.installation.Installation;

public record RaidTargetCandidate(
		String ownerFactionId,
		String installationId,
		Installation installation) {}
