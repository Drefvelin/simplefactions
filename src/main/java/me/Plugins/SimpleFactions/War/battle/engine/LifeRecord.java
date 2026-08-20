package me.Plugins.SimpleFactions.War.battle.engine;

import org.bukkit.entity.Player;

public class LifeRecord {
	private Player p;
	private int lives;
	
	public LifeRecord(Player p, int l) {
		this.p = p;
		this.lives = l;
	}
	public boolean isPlayer(Player pl) {
		if(p.equals(pl)) return true;
		return false;
	}
	public Player getPlayer() {
		return this.p;
	}
	public int getLives() {
		return lives;
	}
	public void update(int change) {
		lives = lives+change;
		if(lives < 0) {
			lives = 0;
		}
	}
}