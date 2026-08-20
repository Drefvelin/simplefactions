package me.Plugins.SimpleFactions.War.battle.engine;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.War.battle.enums.LifeType;
import me.Plugins.SimpleFactions.War.battle.warband.Warband;

public class BattleSide {
	private String id;
	private Location spawn;
	private BossBar lifeBar;
	private List<Warband> bands = new ArrayList<Warband>();
	private LifeType lt;
	private int lives;
	private int maxLives;
	private List<LifeRecord> records = new ArrayList<LifeRecord>();
	private List<Location> respawnPoints = new ArrayList<Location>();
	private Location jail;
	
	public LifeType getLt() {
		return lt;
	}

	public void setLt(LifeType lt) {
		this.lt = lt;
	}

	public int getLives() {
		if(lt.equals(LifeType.PER_PLAYER)) {
			int l = 0;
			for(LifeRecord r : records) {
				l = l+r.getLives();
			}
			return l;
		}
		return lives;
	}
	public boolean hasPlayer(Player p) {
		for(Warband w : bands) {
			if(w.getPlayers().contains(p)) return true;
		}
		return false;
	}

	public void setLives(int lives) {
		lifeBar.setColor(BarColor.GREEN);
		lifeBar.setStyle(BarStyle.SOLID);
		lifeBar.setTitle(id);
		if(lt.equals(LifeType.PER_PLAYER)) {
			this.records.clear();
			for(Warband w : bands) {
				for(Player p : w.getPlayers()) {
					addRecord(p, lives);
				}
			}
		} else {
			this.lives = lives;
			this.maxLives = lives;
		}
	}
	public void updateBossBar(List<Player> list) {
		for(Player p : list) {
			if(!lifeBar.getPlayers().contains(p)) {
				lifeBar.addPlayer(p);
			}
		}
		lifeBar.setProgress(Double.valueOf(getLives())/Double.valueOf(maxLives));
		lifeBar.setTitle("§f"+id+": §e"+getLives());
	}
	public void removeBossBar() {
		if (lifeBar != null) {
			lifeBar.removeAll();
		}
	}

	public void addBossBarPlayer(Player p) {
		lifeBar.addPlayer(p);
	}

	public void removeBossBarPlayer(Player p) {
		lifeBar.removePlayer(p);
	}
	public Location getJail() {
		return jail;
	}

	public void setJail(Location jail) {
		this.jail = jail;
	}
	public Location getSpawn() {
		return spawn;
	}

	public void setSpawn(Location spawn) {
		this.spawn = spawn;
	}

	public List<Warband> getBands() {
		return bands;
	}
	public void addBand(Warband b) {
		bands.add(b);
	}
	public void removeBand(Warband b) {
		bands.remove(b);
	}
	public List<LifeRecord> getRecords() {
		return records;
	}
	public void addRecord(Player p, int amount) {
		LifeRecord r = new LifeRecord(p, amount);
		records.add(r);
		this.maxLives = maxLives+amount;
	}
	public boolean hasRecord(Player p) {
		for(LifeRecord r : records) {
			if(r.getPlayer().equals(p)) return true;
		}
		return false;
	}
	public LifeRecord getRecord(Player p) {
		for(LifeRecord r : records) {
			if(r.getPlayer().equals(p)) return r;
		}
		return null;
	}
	public void updateRecord(Player p, int change) {
		for(LifeRecord r : records) {
			if(r.getPlayer().equals(p)) {
				r.update(change);
			}
		}
	}
	public List<Location> getRespawnPoints() {
		return respawnPoints;
	}
	public void addRespawnPoint(Location l) {
		this.respawnPoints.add(l);
	}
	public String getId() {
		return id;
	}

	public BattleSide(String id, LifeType lifeType, int lives) {
		this.id = id;
		this.lt = lifeType;
		this.lives = lives;
		this.lifeBar = Bukkit.createBossBar(id, BarColor.GREEN, BarStyle.SOLID);
	}

	public void tickLife() {
		if(this.lives > 0) {
			this.lives--;
		}
	}

	public void checkRecords() {
		for(Warband w : bands) {
			for(Player p : w.getPlayers()) {
				if(!hasRecord(p)) {
					addRecord(p, this.lives);
				}
			}
		}
	}

	public Integer getAllParticipants() {
		int count = 0;
		for (Warband w : bands) {
			count = count + w.getMemberCount();
		}
		return count;
	}
	
	
}