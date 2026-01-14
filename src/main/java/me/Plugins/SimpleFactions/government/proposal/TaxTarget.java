package me.Plugins.SimpleFactions.government.proposal;

public enum TaxTarget {
    CITIZENS("Citizen Tax"),
    GUILDS("Default Guild Tax"),
    VASSALS("Default Vassal Tax"),
    DIVIDENDS("Dividends Tax"),
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