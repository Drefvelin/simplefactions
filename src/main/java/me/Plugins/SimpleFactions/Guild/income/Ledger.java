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
import me.Plugins.SimpleFactions.Guild.loans.Loan;
import me.Plugins.SimpleFactions.Guild.upgrade.Upgrade;
import me.Plugins.SimpleFactions.Loaders.InstallationConfigLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.installation.Installation;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.FactionModifier;
import me.Plugins.SimpleFactions.Utils.DailyGuildTransfers;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.enums.FactionModifiers;
import me.Plugins.SimpleFactions.government.proposal.TaxTarget;
import me.Plugins.SimpleFactions.War.resolution.WarReparationsObligation;
import me.Plugins.SimpleFactions.War.resolution.WarReparationsService;

public class Ledger {
    private Guild guild;

    private final Map<String, Double> citizenTaxes = new HashMap<>();

    private final Map<String, Double> loanPayments = new HashMap<>();
    private final Map<String, Double> interestPayments = new HashMap<>();

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

    public void addLoanPaymentEntry(String payerGuildId, Double amount) {
        if(loanPayments.containsKey(payerGuildId)) {
            loanPayments.put(payerGuildId, loanPayments.get(payerGuildId)+amount);
        } else {
            loanPayments.put(payerGuildId, amount);
        }
    }

    public void addInterestPaymentEntry(String payerGuildId, Double amount) {
        if(interestPayments.containsKey(payerGuildId)) {
            interestPayments.put(payerGuildId, interestPayments.get(payerGuildId)+amount);
        } else {
            interestPayments.put(payerGuildId, amount);
        }
    }

    public double getIncome(Cashflow cashflow) {
        double amount = 0;
        if(guild.isBankrupt()) return 0.0; //bankrupt guilds dont pay or receive money, they need to get our of bankrupcy first
        Faction f = guild.getFaction();
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
                amount *= guild.getFaction().getTaxRate(TaxTarget.GUILDS, guild.getId(), true)/100.0;
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
                if(!guild.isBase()) return 0;
                amount = getTributeRecieved();
                break;
            case OVERLORD_TAX:
                if(!guild.isBase()) return 0;
                if(f.getOverlord() == null) return 0;
                amount = -getOverlordTax();
                break;
            //Loans
            case LOAN_PAYMENTS: {
                for(Loan loan : guild.getLoanHandler().getLoansTaken()) {
                    if(!loan.isAutoPay()) continue;
                    if(loan.isPaidOff()) continue;
                    amount -= loan.getDailyPayment(true);
                }
                break;
            }
            case LOANS:
                amount += getAggregatedLoanPayments();
                for(Loan loan : guild.getLoanHandler().getLoansGiven()) {
                    if(!loan.isAutoPay()) continue;
                    if(loan.isPaidOff()) continue;
                    amount += loan.getDailyPayment(true);
                }
                break;
            //Interest
            case INTEREST_PAYMENTS: {
                for(Loan loan : guild.getLoanHandler().getLoansTaken()) {
                    if(!loan.isAutoPay()) continue;
                    if(loan.isPaidOff()) continue;
                    amount -= loan.getDailyInterest();
                }
                break;
            }
            case INTEREST:
                amount += getAggregatedInterestPayments();
                for(Loan loan : guild.getLoanHandler().getLoansGiven()) {
                    if(!loan.isAutoPay()) continue;
                    if(loan.isPaidOff()) continue;
                    amount += loan.getDailyInterest();
                }
                break;
            case WAR_REPARATIONS:
                if (!guild.isBase()) {
                    return 0;
                }
                amount = getWarReparationsReceived();
                break;
            case WAR_REPARATIONS_PAYMENT:
                if (!guild.isBase()) {
                    return 0;
                }
                amount = -getWarReparationsPayment();
                break;
            case TRADE:
                amount = guild.getTradeBreakdown().getIncome();
                break;
            case TRADE_UPKEEP:
                amount = -guild.getTradeBreakdown().getUpkeep();
                break;
            case INSTALLATIONS:
                if (!guild.isBase()) {
                    return 0;
                }
                for (Installation installation : f.getInstallationHandler().getAll()) {
                    amount -= InstallationConfigLoader.getDailyUpkeep(installation.getKind());
                }
                break;
            case MILITARY_UPKEEP:
                if (!guild.isBase()) {
                    return 0;
                }
                amount = -f.getMilitary().getTotalUpkeep();
                break;
            case UPGRADES_UPKEEP:
                for(Upgrade u : guild.getUpgrades()) {
                    amount -= u.getTotalUpkeep();
                }
                break;
            case PENALTIES:
                if(!guild.isBase()) return 0;
                amount = -f.getPenalty();
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

    private double getAggregatedLoanPayments() {
        double total = 0;
        for(Double d : loanPayments.values()) {
            total+=d;
        }
        return total;
    }

    private double getAggregatedInterestPayments() {
        double total = 0;
        for(Double d : interestPayments.values()) {
            total+=d;
        }
        return total;
    }

    public double getNetIncome() {
        double net = 0.0;
        if(guild.isBankrupt()) return 0.0; //bankrupt guilds dont pay or receive money, they need to get our of bankrupcy first

        for (Cashflow cf : Cashflow.values()) {
            switch (cf) {

                // -------- POSITIVE / INCOME --------
                case TRADE:
                case CITIZENS:
                case TARIFFS:
                case GUILDS:
                case VASSALS:
                case TRIBUTES:
                case DIVIDENDS:
                case WAR_REPARATIONS:
                case LOANS:
                case INTEREST:
                    net += getIncome(cf);
                    break;

                // -------- NEGATIVE / COSTS --------
                case TRADE_UPKEEP:
                case UPGRADES_UPKEEP:
                case INSTALLATIONS:
                case MILITARY_UPKEEP:
                case PENALTIES:
                case GUILD_PAYMENTS:
                case OVERLORD_TAX:
                case TRIBUTE_PAYMENTS:
                case TARIFF_PAYMENTS:
                case DIVIDEND_PAYOUT:
                case WAR_REPARATIONS_PAYMENT:
                case LOAN_PAYMENTS:
                case INTEREST_PAYMENTS:
                    net += getIncome(cf); // already negative
                    break;

                default:
                    break;
            }
        }

        return Formatter.formatDouble(net);
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
        return base * f.getOverlordTaxRate(overlord);
    }

    public double getTributeTax() {
        Faction f = guild.getFaction();
        double base = getInternalTaxableIncome();
        double paid = 0.0;
        for (FactionModifier mod : f.getModifiers()) {
            if (mod.getFrom() == null) continue;
            if (!mod.getType().equals(FactionModifiers.TRIBUTE)) continue;
            paid += base * (mod.getAmount() / 100.0);
        }
        return paid;
    }

    public double getTributeRecieved() {
        double total = 0.0;
        for(Faction f : FactionManager.factions) {
            if(f.getId().equals(guild.getFaction().getId())) continue;
            for(FactionModifier mod : f.getModifiers()) {
                if(mod.getFrom() == null) continue;
                if(!mod.getType().equals(FactionModifiers.TRIBUTE)) continue;
                if(!mod.getFrom().getId().equals(guild.getFaction().getId())) continue;

                double base = f.getOrCreateMainGuild().getLedger().getInternalTaxableIncome();
                total += base * (mod.getAmount() / 100.0);
            }
        }
        return total;
    }

    public double getWarReparationsPayment() {
        if (!guild.isBase()) {
            return 0.0;
        }
        Faction f = guild.getFaction();
        double base = getReparationsTaxableIncome();
        double paid = 0.0;
        for (WarReparationsObligation obligation : WarReparationsService.activeObligations(f)) {
            paid += base * (obligation.getIncomePercent() / 100.0);
        }
        return paid;
    }

    public double getWarReparationsReceived() {
        if (!guild.isBase()) {
            return 0.0;
        }
        Faction self = guild.getFaction();
        if (self == null || self.getId() == null) {
            return 0.0;
        }
        double total = 0.0;
        for (Faction f : FactionManager.factions) {
            if (f == null || f.getId().equals(self.getId())) {
                continue;
            }
            Guild payerGuild = f.getOrCreateMainGuild();
            if (payerGuild == null || payerGuild.getLedger() == null) {
                continue;
            }
            double base = payerGuild.getLedger().getReparationsTaxableIncome();
            for (WarReparationsObligation obligation : WarReparationsService.activeObligations(f)) {
                if (!self.getId().equalsIgnoreCase(obligation.getPayeeFactionId())) {
                    continue;
                }
                total += base * (obligation.getIncomePercent() / 100.0);
            }
        }
        return total;
    }

    double getReparationsTaxableIncome() {
        return getInternalTaxableIncome();
    }

    /**
     * Positive gross-counted income excluding cross-faction transfers (tribute,
     * war reparations, vassal guild rollups). Used as the base for tribute and
     * reparations so ledger queries cannot recurse between factions.
     */
    double getInternalTaxableIncome() {
        if (guild.isBankrupt()) {
            return 0.0;
        }
        double total = 0.0;
        for (Cashflow cf : Cashflow.values()) {
            if (!cf.isGrossCounted() || isCrossFactionGrossCashflow(cf)) {
                continue;
            }
            double amount = getIncome(cf);
            if (amount <= 0) {
                continue;
            }
            total += amount;
        }
        return total;
    }

    private static boolean isCrossFactionGrossCashflow(Cashflow cashflow) {
        return cashflow == Cashflow.TRIBUTES
                || cashflow == Cashflow.WAR_REPARATIONS
                || cashflow == Cashflow.VASSALS
                || cashflow == Cashflow.GUILDS;
    }

    public double getGrossTaxableIncome() {
        if(guild.isBankrupt()) return 0.0; //bankrupt guilds dont pay or receive money, they need to get our of bankrupcy first
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

    public void populateDailyTransfers(DailyGuildTransfers buffer) {
        if(guild.isBankrupt()) {
            for(Loan loan : guild.getLoanHandler().getLoansTaken()) {
                if(!loan.isAutoPay()) continue;
                if(loan.isPaidOff()) continue;
                loan.setAutoPay(false);
            }
        }
        for (Cashflow cf : Cashflow.values()) {
            applySettlementFor(cf, buffer);
        }
        citizenTaxes.clear();
        loanPayments.clear();
        interestPayments.clear();
    }

    private void applySettlementFor(Cashflow cf, DailyGuildTransfers buffer) {
        if(guild.isBankrupt()) return; //bankrupt guilds dont pay or receive money, they need to get our of bankrupcy first
        switch (cf) {
            // --------- INTERNAL (single guild) ----------
            // These should NOT be computed by reading getIncome() from some other guild.
            // They are simply added to this guild's daily delta.
            case TRADE:
            case TRADE_UPKEEP:
            // INSTALLATIONS: withdrawn in Faction.newDay() — getIncome() is ledger GUI display only
            case UPGRADES_UPKEEP:
            case PENALTIES:
            case CITIZENS:
                buffer.addExternalDelta(guild, getIncome(cf));
                return;

            // --------- TRANSFERS (guild -> guild) ----------
            case GUILD_PAYMENTS: {
                if (guild.isBase()) return; // base doesn't pay guild tax
                Guild capital = guild.getFaction().getOrCreateMainGuild();
                double amount = Math.abs(getIncome(Cashflow.GUILD_PAYMENTS));
                buffer.add(guild, capital, amount);
                return;
            }

            case OVERLORD_TAX: {
                if (!guild.isBase()) return; //only base pays
                Faction overlord = guild.getFaction().getOverlord();
                if (overlord == null) return;
                Guild overlordCapital = overlord.getOrCreateMainGuild();
                double amount = Math.abs(getIncome(Cashflow.OVERLORD_TAX));
                buffer.add(guild, overlordCapital, amount);
                return;
            }

            case TRIBUTE_PAYMENTS: {
                if(!guild.isBase()) return; //only base pays
                Faction f = guild.getFaction();
                double base = getInternalTaxableIncome();

                for (FactionModifier mod : f.getModifiers()) {
                    if (mod.getFrom() == null) continue;
                    if (!mod.getType().equals(FactionModifiers.TRIBUTE)) continue;

                    Faction receiverFaction = mod.getFrom();
                    Guild receiverGuild = receiverFaction.getOrCreateMainGuild();

                    double amount = base * (mod.getAmount() / 100.0);
                    if (amount <= 0) continue;

                    buffer.add(guild, receiverGuild, amount);
                }
                return;
            }

            case WAR_REPARATIONS_PAYMENT: {
                if (!guild.isBase()) {
                    return;
                }
                Faction f = guild.getFaction();
                double base = getReparationsTaxableIncome();
                for (WarReparationsObligation obligation : WarReparationsService.activeObligations(f)) {
                    Faction receiverFaction = FactionManager.getByString(obligation.getPayeeFactionId());
                    if (receiverFaction == null) {
                        continue;
                    }
                    Guild receiverGuild = receiverFaction.getOrCreateMainGuild();
                    double amount = base * (obligation.getIncomePercent() / 100.0);
                    if (amount <= 0) {
                        continue;
                    }
                    buffer.add(guild, receiverGuild, amount);
                }
                return;
            }

            //Taxes and Tariffs
            case TARIFF_PAYMENTS: {
                for(Map.Entry<Faction, Double> entry : guild.getTradeBreakdown().getTariffsByFactionMap().entrySet()) {
                    Faction receiverFaction = entry.getKey();
                    Guild receiverGuild = receiverFaction.getOrCreateMainGuild();
                    double amount = entry.getValue();
                    if(amount <= 0) continue;
                    buffer.add(guild, receiverGuild, amount);
                }
                break;
            }

            //Loans
            case LOAN_PAYMENTS: {
                for(Loan loan : guild.getLoanHandler().getLoansTaken()) {
                    double amount = 0;
                    if(!loan.isAutoPay()) continue;
                    if(loan.isPaidOff()) continue;
                    amount += loan.getDailyPayment(true);
                    if(amount <= 0) continue;
                    loan.setTempPayment(amount);
                    buffer.add(guild, loan.getIssuer(), amount);
                }
                break;
            }

            //Interest
            case INTEREST_PAYMENTS: {
                for(Loan loan : guild.getLoanHandler().getLoansTaken()) {
                    double amount = 0;
                    if(!loan.isAutoPay()) continue;
                    if(loan.isPaidOff()) continue;
                    amount += loan.getDailyInterest();
                    if(amount <= 0) continue;
                    loan.setTempInterestPayment(amount);
                    buffer.add(guild, loan.getIssuer(), amount);
                }
                break;
            }

            case LOANS:
                buffer.addExternalDelta(guild, getAggregatedLoanPayments());
                break;
            case INTEREST:
                buffer.addExternalDelta(guild, getAggregatedInterestPayments());
                break;

            //To be implemented
            case DIVIDEND_PAYOUT:
            
            //Display only
            case GUILDS:
            case VASSALS:
            case DIVIDENDS:
            case TRIBUTES:
            case TARIFFS:
            case WAR_REPARATIONS:
            default:
                return;
        }
    }
}
