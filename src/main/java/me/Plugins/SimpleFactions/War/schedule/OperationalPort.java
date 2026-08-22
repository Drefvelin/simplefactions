package me.Plugins.SimpleFactions.War.schedule;

import me.Plugins.SimpleFactions.Objects.Faction;

public record OperationalPort(String id, Faction owner, int province, long completedAt) {
}
