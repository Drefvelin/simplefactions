package me.Plugins.SimpleFactions.enums;

public enum GuildModifier {

    TRADE_POWER("#92d665Trade Power", true),
    TRADE_UPKEEP("#d6645aTrade Upkeep", false),
    PRODUCTION("#f2c94cProduction", true),
    TRADE_CARRY("#86d1b0Trade Carry", true),
    DIPLOMATIC_CAPACITY("#56ccf2Diplomatic Capacity", true);

    private final String name;
    private final boolean positive;

    GuildModifier(String name, boolean positive) {
        this.name = name;
        this.positive = positive;
    }

    public String getName() {
        return name;
    }

    public boolean isPositive() {
        return positive;
    }
}

