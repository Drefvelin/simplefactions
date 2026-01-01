package me.Plugins.SimpleFactions.Guild.snapshot;

import java.util.HashMap;
import java.util.Map;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.enums.GuildModifier;

public class GuildModifierSnapshot {
    private final Map<GuildModifier, Double> modifiers = new HashMap<>();

    public GuildModifierSnapshot(Guild guild) {
        for (GuildModifier m : GuildModifier.values()) {
            modifiers.put(m, guild.getModifier(m));
        }
    }

    public void add(GuildModifier m, double delta) {
        modifiers.put(m, modifiers.getOrDefault(m, 0.0) + delta);
    }

    public double get(GuildModifier m) {
        return modifiers.getOrDefault(m, 0.0);
    }
}