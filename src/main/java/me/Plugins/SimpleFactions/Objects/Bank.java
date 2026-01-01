package me.Plugins.SimpleFactions.Objects;


import org.bukkit.Chunk;

import me.Plugins.SimpleFactions.Guild.Guild;
import net.tfminecraft.DenarEconomy.Data.Account;

public class Bank {
	private Guild guild;
	
	private Account bank;
	
	private Chunk chunk;
	
	public Bank(Guild g, Chunk c) {
		guild = g;
		bank = new Account(0, false);
		chunk = c;
	}
	
	public Bank(Guild g, double amount, Chunk c) {
		guild = g;
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
		guild.updateWealth();
	}
	public void withdraw(Double a) {
		bank.change(a*-1);
		guild.updateWealth();
	}
	
	public Chunk getChunk() {
		return chunk;
	}
	
	public void setChunk(Chunk c) {
		chunk = c;
	}
}
