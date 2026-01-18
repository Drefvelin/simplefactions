package me.Plugins.SimpleFactions.Guild.income;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Guild.income.entry.PlayerEntry;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.FactionModifier;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.enums.FactionModifiers;
import me.Plugins.SimpleFactions.government.proposal.TaxTarget;

public class Ledger {
    private Guild guild;

    private final Map<String, Double> citizenTaxes = new HashMap<>();

    public Ledger(Guild guild) {
        this.guild = guild;
    }

    public void addCitizenTaxEntry(String p, Double tax) {
        if(citizenTaxes.containsKey(p)) {
            citizenTaxes.put(p, citizenTaxes.get(p)+tax);
        } else {
            citizenTaxes.put(p, tax);
        }
    }

    public double getIncome(Cashflow cashflow) {
        double amount = 0;
        switch (cashflow) {
            case GUILDS:
                if(!guild.isBase()) return 0;
                for(Guild g : guild.getFaction().getGuildHandler().getGuilds()) {
                    if(g.isBase()) continue;
                    amount += Math.abs(g.getLedger().getIncome(Cashflow.GUILD_PAYMENTS));
                }
                break;
            case GUILD_PAYMENTS:
                if(guild.isBase()) return 0;
                amount = -getGrossTaxableIncome();
                amount *= guild.getFaction().getTaxRate(TaxTarget.GUILDS, guild.getId())/100.0;
                break;
            case DIVIDENDS:
                if(!guild.isBase()) return 0;
                //TODO implement
                break;
            case DIVIDEND_PAYMENT:
                //TODO implement
                break;
            case DIVIDEND_PAYOUT:
                //TODO implement
                break;
            case VASSALS:
                if(!guild.isBase()) return 0;
                for(Faction vassal : RelationManager.getSubjects(guild.getFaction())) {
                    amount += Math.abs(vassal.getOrCreateMainGuild().getLedger().getIncome(Cashflow.OVERLORD_TAX));
                }
                break;
            case CITIZENS:
                if(!guild.isBase()) return 0;
                amount = getAggregatedCitizenTax();
                break;
            case TARIFFS:
                if(!guild.isBase()) return 0;
                amount = getTotalTariffsEarned();
                break;
            case TARIFF_PAYMENTS:
                amount = -guild.getTradeBreakdown().getTariffs();
                break;
            case TRIBUTE_PAYMENTS:
                if(!guild.isBase()) return 0;
                amount = -getTributeTax();
                break;
            case TRIBUTES:
                //TODO implement
                break;
            case OVERLORD_TAX:
                Faction f = guild.getFaction();
                if(f.getOverlord() == null) return 0;
                amount = -getOverlordTax();
                break;
            case WAR_REPARATIONS:
                //TODO implement
                break;
            case WAR_REPARATIONS_PAYMENT:
                //TODO implement
                break;
            case TRADE:
                amount = guild.getTradeBreakdown().getIncome();
                break;
            case TRADE_UPKEEP:
                amount = -guild.getTradeBreakdown().getUpkeep();
                break;
            case FORTS:
                //TODO implement
                break;
            default:
                break;
        }
        return Formatter.formatDouble(amount);
    }

    public double getTotalTariffsEarned() {
        double total = 0;
        for(Guild g : FactionManager.getAllGuilds()) {
            total += g.getTradeBreakdown().getTariffsByFaction(guild.getFaction());
        }
        return total;
    }

    public List<Map.Entry<String, Double>> getCitizenTaxEntriesDescending() {
        return citizenTaxes.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .toList();
    }


    private double getAggregatedCitizenTax() {
        double total = 0;
        for(Double d : citizenTaxes.values()) {
            total+=d;
        }
        return total;
    }

    public double getNetIncome() {
        double gross = getGrossTaxableIncome();
        if(guild.isBase()) gross += getOverlordTax();
        if(guild.isBase()) gross += getTributeTax();
        if(!guild.isBase()) gross += getIncome(Cashflow.GUILD_PAYMENTS);
        return gross;
    }

    
    public double getInflationDelta() {
        double delta = 0.0;

        for (Cashflow cashflow : Cashflow.values()) {
            if (!cashflow.affectsInflation()) continue;

            delta += getIncome(cashflow);
        }

        return delta;
    }

    //Taxes
    public double getOverlordTax() {
        Faction f = guild.getFaction();
        Faction overlord = f.getOverlord();
        if (overlord == null) return 0.0;

        double base = getGrossTaxableIncome();
        return base * f.getOverlordTaxRate(overlord)/100.0;
    }

    public double getTributeTax() {
        Faction f = guild.getFaction();
        double base = getGrossTaxableIncome();
        double paid = 0.0;

        for (FactionModifier mod : f.getModifiers()) {
            if (mod.getFrom() == null) continue;
            if (!mod.getType().equals(FactionModifiers.TRIBUTE)) continue;
            paid += base * (mod.getAmount() / 100.0);
        }
        return paid;
    }

    public double getGrossTaxableIncome() {
        double total = 0.0;
        for (Cashflow cf : Cashflow.values()) {
            if(!cf.isGrossCounted()) continue;
            double amount = getIncome(cf);
            if(amount <= 0) continue;
            total += amount;
        }
        return total;
    }

    public void clearDailyIncome() {
        citizenTaxes.clear();
    }
}
