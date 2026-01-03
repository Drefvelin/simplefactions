package me.Plugins.SimpleFactions.Guild.income;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import me.Plugins.SimpleFactions.Objects.Faction;

public class TradeBreakdown {
    private double income;
    private double upkeep;
    private double tradePower;
    private final Map<Faction, Double> incomeByFaction = new HashMap<>();

    public void clear() {
        income = 0;
        upkeep = 0;
        tradePower = 0;
        incomeByFaction.clear();
    }

    public TradeBreakdown() {
        income = 0;
        upkeep = 0;
        tradePower = 0;
    }

    public double getIncome() {
        return income;
    }
    public double getUpkeep() {
        return upkeep;
    }
    public double getTradePower() {
        return tradePower;
    }

    public void setIncome(double d) {
        income = Math.round(d * 100.0) / 100.0;
    }

    public void setUpkeep(double d) {
        upkeep = Math.round(d * 100.0) / 100.0;
    }

    public void setTradePower(double d) {
        tradePower = Math.round(d * 100.0) / 100.0;
    }

    public Map<Faction, Double> getIncomes() {
        return incomeByFaction;
    }

    public double getNetIncome() {
        return Math.round((income-upkeep) * 100.0) / 100.0;
    }

    public void registerIncome(Faction f, double d) {
        if(incomeByFaction.containsKey(f)) {
            incomeByFaction.put(f, incomeByFaction.get(f)+d);
            return;
        }
        incomeByFaction.put(f, d);
    }

    public double getIncomeByFaction(Faction f) {
        return Math.round(incomeByFaction.getOrDefault(f, 0.0) * 100.0) / 100.0;
    }

    public List<Faction> getFactionsByIncomeDesc() {
        return incomeByFaction.entrySet()
                .stream()
                .sorted(Map.Entry.<Faction, Double>comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
