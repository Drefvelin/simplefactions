package me.Plugins.SimpleFactions.Objects;


import org.bukkit.Chunk;

import net.tfminecraft.DenarEconomy.Data.Account;

public class Bank {
	private Faction faction;
	
	private Account bank;
	
	private Chunk chunk;
	
	public Bank(Faction f, Chunk c) {
		faction = f;
		bank = new Account(0, false);
		chunk = c;
	}
	
	public Bank(Faction f, double amount, Chunk c) {
		faction = f;
		bank = new Account(amount, false);
		chunk = c;
	}
	
	public Double getWealth() {
		return bank.getBal();
	}
	public void setWealth(Double wealth) {
		bank.setBal(wealth);
	}
	public void deposit(Double a) {
		bank.change(a);
		faction.updateWealth();
	}
	public void withdraw(Double a) {
		bank.change(a*-1);
		faction.updateWealth();
	}
	
	public Chunk getChunk() {
		return chunk;
	}
	
	public void setChunk(Chunk c) {
		chunk = c;
	}
}
