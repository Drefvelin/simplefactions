package me.Plugins.SimpleFactions.enums;

public enum GuildModifier {

    TRADE_POWER("#92d665Trade Power"),
    PRODUCTION("#f2c94cProduction"),
    TRADE_CARRY("#86d1b0Trade Carry"),
    DIPLOMATIC_CAPACITY("#56ccf2Diplomatic Capacity");

    private final String name;

    GuildModifier(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

