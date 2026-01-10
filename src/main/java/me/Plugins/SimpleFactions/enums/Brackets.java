package me.Plugins.SimpleFactions.enums;

public enum Brackets {

    CITIZEN_TAX("Citizen Tax"),
    GUILD_TAX("Guild Tax"),
    VASSAL_TAX("Vassal Tax (% of Max)"),
    DIVIDEND_TAX("Dividend Tax");

    private final String display;

    Brackets(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}

