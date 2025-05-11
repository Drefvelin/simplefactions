package me.Plugins.SimpleFactions.Diplomacy;

import me.Plugins.SimpleFactions.Loaders.RelationLoader;
import me.Plugins.SimpleFactions.Objects.FactionModifier;
import me.Plugins.SimpleFactions.enums.FactionModifiers;

public class Relation {
	private RelationType type;
	private Attitude attitude;
	private boolean base = false;
	
	private int opinion = 0;
	
	public Relation(RelationType type, Attitude a) {
		this.type = type;
		this.attitude = a;
		this.opinion = calculate();
	}
	
	public Relation(RelationType type, Attitude a, int opinion) {
		this.type = type;
		this.attitude = a;
		this.opinion = opinion;
	}
	
	public Relation(Relation another) {
		this.type = another.getType();
		this.attitude = another.getAttitude();
		this.opinion = another.getOpinion();
	}
	
	public Relation() {
		base = true;
		this.type = RelationLoader.getDefaultType();
		this.attitude = RelationLoader.getDefaultAttitude();
		this.opinion = calculate();
	}
	
	public boolean isDefault() {
		return base;
	}

	public RelationType getType() {
		return type;
	}

	public void setType(RelationType type) {
		this.type = type;
	}

	public Attitude getAttitude() {
		return attitude;
	}

	public void setAttitude(Attitude attitude) {
		this.attitude = attitude;
	}
	
	private int calculate() {
		return attitude.getTarget()+type.getTarget();
	}
	
	public int getOpinion() {
		return opinion;
	}
	
	public double getGiveModifier(FactionModifiers m) {
		for(FactionModifier mod : type.getGiveModifiers()) {
			if(mod.getType().equals(m)) return mod.getAmount();
		}
		return 0.0;
	}
	public double getRecieveModifier(FactionModifiers m) {
		for(FactionModifier mod : type.getRecieveModifiers()) {
			if(mod.getType().equals(m)) return mod.getAmount();
		}
		return 0.0;
	}
	
	public void tick() {
		int target = calculate();
		if(opinion == target) return;
		if(opinion < target) {
			opinion++;
		} else {
			opinion--;
		}
	}
}
