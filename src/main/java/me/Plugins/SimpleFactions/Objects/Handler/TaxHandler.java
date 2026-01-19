package me.Plugins.SimpleFactions.Objects.Handler;

import java.util.HashMap;

import me.Plugins.SimpleFactions.Objects.Bracket;
import me.Plugins.SimpleFactions.government.proposal.TaxTarget;

public class TaxHandler {
    private TaxSnapshot savedSnapshot;

    private double citizenTax;
    private double guildTax;
    private double vassalTax;
    private double dividendTax;
    private double tariffs;

    private HashMap<TaxTarget, HashMap<String, Double>> specificTaxes = new HashMap<>();

    public TaxHandler(double citizenTax, double guildTax, double vassalTax, double dividendTax, double tariffs) {
        this.citizenTax = citizenTax;
        this.guildTax = guildTax;
        this.vassalTax = vassalTax;
        this.dividendTax = dividendTax;
        this.tariffs = tariffs;
    }

    public boolean hasTariffs() {
        return tariffs > 0;
    }

    public void setTariffs(double tariffs) {
        this.tariffs = tariffs;
    }

    public void setCitizenTax(double citizenTax) {
        this.citizenTax = citizenTax;
    }

    public void setGuildTax(double guildTax) {
        this.guildTax = guildTax;
    }

    public void setVassalTax(double vassalTax) {
        this.vassalTax = vassalTax;
    }

    public void setDividendTax(double dividendTax) {
        this.dividendTax = dividendTax;
    }

    public double getTariffs() {
        return tariffs;
    }

    public double getCitizenTax() {
        return citizenTax;
    }
    public double getGuildTax() {
        return guildTax;
    }
    public double getVassalTax() {
        return vassalTax;
    }
    public double getDividendTax() {
        return dividendTax;
    }

    public double getTaxRate(TaxTarget target, String id) {
		double rate = 0;
		switch(target) {
			case CITIZENS:
				rate = citizenTax;
				break;
			case GUILDS:
                if(id != null && hasSpecificTax(target, id)) rate = getSpecificTax(target, id);
				else rate = guildTax;
				break;
			case VASSALS:
                if(id != null && hasSpecificTax(target, id)) rate = getSpecificTax(target, id);
                else rate = vassalTax;
				break;
			case DIVIDENDS:
				rate = dividendTax;
				break;
            case TARIFFS:
				rate = tariffs;
				break;
            case GUILD_ID:
                if(id != null && hasSpecificTax(target, id)) rate = getSpecificTax(target, id);
				else rate = guildTax;
                break;
            case VASSAL_ID:
                if(id != null && hasSpecificTax(target, id)) rate = getSpecificTax(target, id);
                else rate = vassalTax;
                break;
			default:
				rate = 0;
				break;
		}
		return rate;
	}

    public boolean hasSpecificTax(TaxTarget target, String id) {
        return specificTaxes.containsKey(target) && specificTaxes.get(target).containsKey(id);
    }

    public double getSpecificTax(TaxTarget target, String id) {
        if(!hasSpecificTax(target, id)) return -1.0;
        return specificTaxes.get(target).getOrDefault(id, -1.0);
    }

    //Brackets
    private double applyBracket(double value, Bracket bracket) {
        if (value < bracket.getMin()) return bracket.getMin();
        if (value > bracket.getMax()) return bracket.getMax();
        return value;
    }

    public void applyBracket(TaxTarget target, Bracket bracket) {

        switch (target) {

            case CITIZENS:
                citizenTax = applyBracket(citizenTax, bracket);
                break;

            case GUILDS:
                guildTax = applyBracket(guildTax, bracket);
                applySpecificBracket(target, bracket);
                break;

            case VASSALS:
                vassalTax = applyBracket(vassalTax, bracket);
                applySpecificBracket(target, bracket);
                break;

            case DIVIDENDS:
                dividendTax = applyBracket(dividendTax, bracket);
                break;

            case TARIFFS:
                tariffs = applyBracket(tariffs, bracket);
                break;

            default:
                break;
        }
    }

    public void setSpecificTax(TaxTarget target, String id, double rate) {
        double defaultRate = getDefaultRate(target);

        if (Double.compare(rate, defaultRate) == 0) {
            // Not specific anymore
            HashMap<String, Double> map = specificTaxes.get(target);
            if (map != null) {
                map.remove(id);
                if (map.isEmpty()) specificTaxes.remove(target);
            }
            return;
        }

        specificTaxes
            .computeIfAbsent(target, k -> new HashMap<>())
            .put(id, rate);
    }

    private void applySpecificBracket(TaxTarget target, Bracket bracket) {
        if (!specificTaxes.containsKey(target)) return;

        HashMap<String, Double> map = specificTaxes.get(target);
        double defaultRate = getDefaultRate(target);

        map.entrySet().removeIf(entry -> {
            double clamped = applyBracket(entry.getValue(), bracket);

            // If equal to default, remove the override
            if (Double.compare(clamped, defaultRate) == 0) {
                return true;
            }

            // Otherwise update value
            entry.setValue(clamped);
            return false;
        });

        // Clean up empty maps
        if (map.isEmpty()) {
            specificTaxes.remove(target);
        }
    }

    //State
    public void saveState() {
        HashMap<TaxTarget, HashMap<String, Double>> copiedSpecificTaxes = new HashMap<>();

        for (var entry : specificTaxes.entrySet()) {
            copiedSpecificTaxes.put(
                entry.getKey(),
                new HashMap<>(entry.getValue())
            );
        }

        savedSnapshot = new TaxSnapshot(
            citizenTax,
            guildTax,
            vassalTax,
            dividendTax,
            tariffs,
            copiedSpecificTaxes
        );
    }

    public void restoreState() {
        if (savedSnapshot == null) return;

        this.citizenTax = savedSnapshot.citizenTax;
        this.guildTax = savedSnapshot.guildTax;
        this.vassalTax = savedSnapshot.vassalTax;
        this.dividendTax = savedSnapshot.dividendTax;
        this.tariffs = savedSnapshot.tariffs;

        this.specificTaxes.clear();

        for (var entry : savedSnapshot.specificTaxes.entrySet()) {
            this.specificTaxes.put(
                entry.getKey(),
                new HashMap<>(entry.getValue())
            );
        }

        savedSnapshot = null; // optional: prevent double restore
    }

    private double getDefaultRate(TaxTarget target) {
        return switch (target) {
            case CITIZENS -> citizenTax;
            case GUILDS, GUILD_ID -> guildTax;
            case VASSALS, VASSAL_ID -> vassalTax;
            case DIVIDENDS -> dividendTax;
            case TARIFFS -> tariffs;
            default -> 0.0;
        };
    }
}
