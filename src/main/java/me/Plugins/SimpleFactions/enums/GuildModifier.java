package me.Plugins.SimpleFactions.enums;

public enum GuildModifier {
    TRADE_POWER("#92d665Trade Power");

    private final String name;

    private GuildModifier(String s) {
        name = s;
    }

    public String getName() { return name; }
}
