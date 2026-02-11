package me.Plugins.SimpleFactions.Objects;

import java.util.UUID;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.enums.FactionModifiers;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class FactionModifier {
	protected FactionModifiers type;
	protected double amount;
	protected Faction from;
	
	public FactionModifier(String m) {
	    try {
	        amount = Double.parseDouble(m.split("\\(")[1].replace(")", ""));
	        type = FactionModifiers.valueOf(m.split("\\(")[0].toUpperCase());
	    } catch(Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public FactionModifier(FactionModifiers type, double amount) {
		this.type = type;
		this.amount = amount;
	}
	
	public FactionModifier(Faction from, FactionModifier m) {
		this.from = from;
		this.type = m.getType();
		this.amount = m.getAmount();
	}
	public FactionModifier(Faction from, FactionModifiers type, double amount) {
		this.from = from;
		this.type = type;
		this.amount = amount;
	}
	
	public Faction getFrom() {
		return from;
	}
	
	public void edit(double d) {
		amount += d;
		fix();
	}
	
	private void fix() {
		amount = Math.round(amount*100)/100;
	}
	
	private String prefix() {
		String prefix = "";
		switch(type) {
			case LEVY:
				prefix = "#d45131Levy Contribution";
				break;
			case MILITARY_UPKEEP:
				prefix = "#b39088Military Upkeep";
				break;
			case NODE_SPEED:
				prefix = "#92d96cNode Speed";
				break;
			case PRESTIGE:
				prefix = "#3e7fb5Prestige to Overlord";
				break;
			case PRESTIGE_BONUS:
				prefix = "#409dc2Prestige Bonus";
				break;
			case TRIBUTE:
				prefix = "#d49024Tribute";
				break;
			case TAX_MULTIPLIER:
				prefix = "#5acca2Tax Multiplier";
				break;
			case DE_JURE:
				prefix = "#7bd481De Jure Requirement";
				break;
			case TRADE_POWER:
				prefix = "#92d665Trade Power";
				break;
			case PRODUCTION:
				prefix = "#f2c94cProduction";
				break;
			case DIPLOMATIC_CAPACITY_MULTIPLIER:
				prefix = "#56ccf2Diplomatic Capacity Multiplier";
				break;
			case ADMIN_POWER_MULTIPLIER:
				prefix = "#ebde54Admin Power Multiplier";
				break;
			case ADMIN_POWER_GAIN_MULTIPLIER:
				prefix = "#d1b347Admin Power Gain Multiplier";
				break;
			default:
				prefix = "#c7b381Unknown Modifier";
				break;
		}
		return StringFormatter.formatHex(prefix);
	}
	
	private String suffix() {
		String color = isBeneficial() ? "#87d65c" : "#d65c5c";

		return StringFormatter.formatHex(
			"§7(" + color
			+ (isMultiplier() && amount > 0 ? "+" : "")
			+ amount + "%§7)"
		);
	}


	public boolean isMultiplier() {
		return type == FactionModifiers.TAX_MULTIPLIER;
	}
	
	public String getString() {
		return prefix()+"§e: "+suffix();
	}
	
	public FactionModifiers getType() {
		return type;
	}
	
	public double getAmount() {
		if(type.equals(FactionModifiers.DE_JURE) && Cache.deJureRequirement+amount < 20) return 20-Cache.deJureRequirement;
		return amount;
	}
	
	private boolean isBeneficial() {
		boolean positive = amount > 0;
		boolean goodOutcome = type.isPositiveGood() ? positive : !positive;
		return goodOutcome;
	}

	@Override
	public String toString() {
		return type.name() + "{from=" + (from != null ? from.getId() : "null") + ", amount=" + amount + "}";
	}
}
