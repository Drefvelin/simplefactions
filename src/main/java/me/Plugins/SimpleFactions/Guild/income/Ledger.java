package me.Plugins.SimpleFactions.Guild.income;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Guild.income.entry.FactionEntry;
import me.Plugins.SimpleFactions.Guild.income.entry.GuildEntry;
import me.Plugins.SimpleFactions.Guild.income.entry.PlayerEntry;

public class Ledger {
    private Guild guild;

    private final Map<Cashflow, List<FactionEntry>> factionEntries = new EnumMap<>(Cashflow.class);
    private final Map<Cashflow, List<GuildEntry>> guildEntries = new EnumMap<>(Cashflow.class);
    private final Map<Cashflow, List<PlayerEntry>> playerEntries = new EnumMap<>(Cashflow.class);

    private final Map<Cashflow, Double> incomes = new EnumMap<>(Cashflow.class);

    public Ledger(Guild guild) {
        this.guild = guild;
    }

    public void addFactionEntry(Cashflow cashflow, FactionEntry entry) {
        factionEntries
                .computeIfAbsent(cashflow, k -> new ArrayList<>())
                .add(entry);

        addIncome(cashflow, entry.getAmount());
    }

    public void addGuildEntry(Cashflow cashflow, GuildEntry entry) {
        guildEntries
                .computeIfAbsent(cashflow, k -> new ArrayList<>())
                .add(entry);

        addIncome(cashflow, entry.getAmount());
    }

    public void addPlayerEntry(Cashflow cashflow, PlayerEntry entry) {
        playerEntries
                .computeIfAbsent(cashflow, k -> new ArrayList<>())
                .add(entry);

        addIncome(cashflow, entry.getAmount());
    }

    private void addIncome(Cashflow cashflow, double amount) {
        incomes.merge(cashflow, amount, Double::sum);
    }

    public double getIncome(Cashflow cashflow) {
        switch (cashflow) {
            case TRADE:
                return guild.getTradeBreakdown().getIncome();
            case TRADE_UPKEEP:
                return -guild.getTradeBreakdown().getUpkeep();
            default:
                break;
        }
        return incomes.getOrDefault(cashflow, 0.0);
    }

    public List<FactionEntry> getFactionEntries(Cashflow cashflow) {
        return factionEntries.getOrDefault(cashflow, List.of());
    }

    public List<GuildEntry> getGuildEntries(Cashflow cashflow) {
        return guildEntries.getOrDefault(cashflow, List.of());
    }

    public List<PlayerEntry> getPlayerEntries(Cashflow cashflow) {
        return playerEntries.getOrDefault(cashflow, List.of());
    }

    public double getNetIncome() {
        double net = 0.0;

        for (Cashflow cashflow : Cashflow.values()) {
            net += getIncome(cashflow);
        }

        return net;
    }
    
    public double getInflationDelta() {
        double delta = 0.0;

        for (Cashflow cashflow : Cashflow.values()) {
            if (!cashflow.affectsInflation()) continue;

            delta += getIncome(cashflow);
        }

        return delta;
    }
}
