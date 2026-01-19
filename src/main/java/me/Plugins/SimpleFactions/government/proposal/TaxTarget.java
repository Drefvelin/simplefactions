package me.Plugins.SimpleFactions.government.proposal;

public enum TaxTarget {
    CITIZENS("Citizens"),
    GUILDS("Guilds"),
    VASSALS("Vassals"),
    DIVIDENDS("Dividends"),
    TARIFFS("Tariffs"),
    TARIFF_ID("Faction Specific Tariffs"),
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