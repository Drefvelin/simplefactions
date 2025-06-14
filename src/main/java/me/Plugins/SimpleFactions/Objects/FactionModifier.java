package me.Plugins.SimpleFactions.Objects;

import java.util.UUID;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.enums.FactionModifiers;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class FactionModifier {
	protected String id;
	protected FactionModifiers type;
	protected double amount;
	protected int time;
	protected Faction from;
	
	public FactionModifier(String m) {
		id = UUID.randomUUID().toString();
	    if (m.contains(";")) {
	        String[] parts = m.split(";");
	        time = Integer.parseInt(parts[1]);
	        m = parts[0]; // Keep only the part before the semicolon
	    } else {
	        time = -1;
	    }

	    try {
	        amount = Double.parseDouble(m.split("\\(")[1].replace(")", ""));
	        type = FactionModifiers.valueOf(m.split("\\(")[0].toUpperCase());
	    } catch(Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public FactionModifier(FactionModifiers type, double amount) {
		id = UUID.randomUUID().toString();
		this.type = type;
		this.amount = amount;
		this.time = -1;
	}
	
	public FactionModifier(FactionModifiers type, double amount, int time) {
		id = UUID.randomUUID().toString();
		this.type = type;
		this.amount = amount;
		this.time = time;
	}
	
	public FactionModifier(Faction from, FactionModifier m) {
		this.from = from;
		id = m.getId();
		this.type = m.getType();
		this.amount = m.getAmount();
		this.time = m.getTime();
	}
	public FactionModifier(Faction from, FactionModifiers type, double amount, int time) {
		this.from = from;
		id = UUID.randomUUID().toString();
		this.type = type;
		this.amount = amount;
		this.time = time;
	}
	
	public String getId() {
		return id;
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
	
	public int getTime() {
		return time;
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
			case TAX:
				prefix = "#d49024Tax Contribution";
				break;
			case DE_JURE:
				prefix = "#7bd481De Jure Requirement";
				break;
			default:
				prefix = "#c7b381Unknown Modifier";
				break;
		}
		return StringFormatter.formatHex(prefix);
	}
	
	private String suffix() {
		String suffix = StringFormatter.formatHex("§7(#8bb36d"+amount+"%§7)");
		return suffix;
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
	
	public boolean isTimed() {
		return time != -1;
	}
	
	public boolean tick() {
		time--;
		if(time == 0) return true;
		return false;
	}
	
	
	@Override
	public boolean equals(Object obj) {
	    if (this == obj) return true;
	    if (obj == null || getClass() != obj.getClass()) return false;
	    
	    FactionModifier other = (FactionModifier) obj;
	    return this.id.equals(other.id);
	}

	@Override
	public int hashCode() {
	    return id.hashCode();
	}
}
