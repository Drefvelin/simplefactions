package me.Plugins.SimpleFactions.Army;

import me.Plugins.SimpleFactions.Objects.Faction;

public class LevyEntry {
	private Faction from;
	private int amount;
	
	public LevyEntry(Faction from, int amount) {
		this.from = from;
		this.amount = amount;
	}

	public Faction getFrom() {
		return from;
	}

	public int getAmount() {
		return amount;
	}
	
	public void setAmount(int i) {
		amount = i;
	}
	
	public void decrease() {
		if(amount > 0) amount--;
	}
	
	public void increase() {
		amount++;
	}
}
