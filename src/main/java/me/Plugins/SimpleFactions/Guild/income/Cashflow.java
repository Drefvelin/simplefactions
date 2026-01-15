package me.Plugins.SimpleFactions.Guild.income;

public enum Cashflow {

    // Taxes – redistribution only
    GUILDS("#c7bf85From tax on #b89448Guilds", false),
    DIVIDENDS("#c7bf85From tax on #c49e5cDividends", false),
    VASSALS("#c7bf85From tax on #7299b5Vassals", false),
    CITIZENS("#c7bf85From tax on #94b572Citizens", false),
    TARIFFS("#5cc46aTariffs", false),
    TRIBUTE_PAYMENTS("#ab8568Tribute Payments", false),
    OVERLORD_TAX("#b55e94Overlord Taxes", false),
    WAR_REPARATIONS("#8a433bWar Reparations", false),

    // Money creation
    TRADE("#92d665Trade", true),

    // Money sinks
    TRADE_UPKEEP("#d6645aTrade Upkeep", true),
    FORTS("#706964Forts", true);

    private final String display;
    private final boolean affectsInflation;

    Cashflow(String display, boolean affectsInflation) {
        this.display = display;
        this.affectsInflation = affectsInflation;
    }

    public String getDisplay() {
        return display;
    }

    public boolean affectsInflation() {
        return affectsInflation;
    }
}
