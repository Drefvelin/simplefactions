package me.Plugins.SimpleFactions.Objects.Handler;

import java.util.HashMap;

import me.Plugins.SimpleFactions.government.proposal.TaxTarget;

public class TaxHandler {
    private double citizenTax;
    private double guildTax;
    private double vassalTax;
    private double dividendTax;

    private HashMap<String, Double> specificTaxes = new HashMap<>();

    public TaxHandler(double citizenTax, double guildTax, double vassalTax, double dividendTax) {
        this.citizenTax = citizenTax;
        this.guildTax = guildTax;
        this.vassalTax = vassalTax;
        this.dividendTax = dividendTax;
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
				rate = guildTax;
				break;
			case VASSALS:
				rate = vassalTax;
				break;
			case DIVIDENDS:
				rate = dividendTax;
				break;
            case GUILD_ID:
                rate = getSpecificTax(id);
                break;
            case VASSAL_ID:
                rate = getSpecificTax(id);
                break;
			default:
				rate = 0;
				break;
		}
		return rate;
	}

    public boolean hasSpecificTax(String id) {
        return specificTaxes.containsKey(id);
    }

    public double getSpecificTax(String id) {
        return specificTaxes.getOrDefault(id, -1.0);
    }
}
