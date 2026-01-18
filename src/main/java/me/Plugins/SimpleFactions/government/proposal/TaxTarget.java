package me.Plugins.SimpleFactions.government.proposal;

public enum TaxTarget {
    CITIZENS("Citizens"),
    GUILDS("Guilds"),
    VASSALS("Vassals"),
    TARIFFS("Tariffs"),
    DIVIDENDS("Dividends"),
    GUILD_ID("Guild Specific"),
    VASSAL_ID("Vassal Specific");

    private String displayName;

    TaxTarget(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}