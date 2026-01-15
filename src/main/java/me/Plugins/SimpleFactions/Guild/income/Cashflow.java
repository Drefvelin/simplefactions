package me.Plugins.SimpleFactions.Guild.income;

public enum Cashflow {
    GUILDS("#c7bf85From tax on #b89448Guilds"),
    DIVIDENDS("#c7bf85From tax on #c49e5cDividends"),
    VASSALS("#c7bf85From tax on #7299b5Vassals"),
    CITIZENS("#c7bf85From tax on #94b572Citizens"),
    TRADE("#92d665Trade"),
    TARIFFS("#5cc46aTariffs"),
    TRIBUTE_PAYMENTS("#ab8568Tribute Payments"),
    FORTS("#706964Forts"),
    OVERLORD_TAX("#b55e94Overlord Taxes"),
    TRADE_UPKEEP("#d6645aTrade Upkeep");

    private String display;

    Cashflow(String d) {
        display = d;
    }

    public String getDisplay() { return display; }
}
