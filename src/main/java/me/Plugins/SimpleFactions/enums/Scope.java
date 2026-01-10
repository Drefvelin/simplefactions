package me.Plugins.SimpleFactions.enums;

public enum Scope {

    FACTION("Faction"),
    VASSALS("All Vassals"),
    FAVOURED_VASSALS("Favoured Vassals"),
    UNFAVOURED_VASSALS("Unfavoured Vassals"),
    FAVOURED_GUILDS("Favoured Guilds"),
    UNFAVOURED_GUILDS("Unfavoured Guilds"),
    DOMESTIC_GUILDS("Domestic Guilds"),
    FOREIGN_GUILDS("Foreign Guilds"),
    VASSAL_GUILDS("Vassal Guilds"),
    OVERLORD_GUILDS("Overlord Guilds");

    private final String display;

    Scope(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
