package me.Plugins.SimpleFactions.Guild.income;

public enum Cashflow {

    // Taxes – redistribution only
    GUILDS("#c7bf85From tax on #b89448Guilds", false, true),
    GUILD_PAYMENTS("#c7bf85From tax on #b89448Guilds", false, false),
    DIVIDENDS("#c7bf85From tax on #c49e5cDividends", false, true),
    DIVIDEND_PAYMENT("#c7bf85From tax on #c49e5cDividends", false, false),
    DIVIDEND_PAYOUT("#c49e5cDividends Payout", false, false),
    VASSALS("#c7bf85From tax on #7299b5Vassals", false, true),
    CITIZENS("#c7bf85From tax on #94b572Citizens", false, true),
    TARIFF_PAYMENTS("#5cc46aTariffs", false, false),
    TARIFFS("#5cc46aTariffs", false, true),
    TRIBUTE_PAYMENTS("#ab8568Tribute Payments", false, false),
    TRIBUTES("#ab8568Tribute Payments", false, true),
    OVERLORD_TAX("#b55e94Overlord Taxes", false, false),
    WAR_REPARATIONS("#8a433bWar Reparations", false, true),
    WAR_REPARATIONS_PAYMENT("#8a433bWar Reparations", false, false),

    // Money creation
    TRADE("#92d665Trade", true, true),

    // Money sinks
    TRADE_UPKEEP("#d6645aTrade Upkeep", true, false),
    UPGRADES_UPKEEP("#c46054Upgrades Upkeep", true, false),
    PENALTIES("#c74c3fPenalties", true, false),
    FORTS("#706964Forts", true, false);

    private final String display;
    private final boolean affectsInflation;
    private final boolean grossCounted;

    Cashflow(String display, boolean affectsInflation, boolean grossCounted) {
        this.display = display;
        this.affectsInflation = affectsInflation;
        this.grossCounted = grossCounted;
    }

    public String getDisplay() {
        return display;
    }

    public boolean affectsInflation() {
        return affectsInflation;
    }

    public boolean isGrossCounted() {
        return grossCounted;
    }
}
