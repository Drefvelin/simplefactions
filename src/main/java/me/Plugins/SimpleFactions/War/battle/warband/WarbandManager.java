package me.Plugins.SimpleFactions.War.battle.warband;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.battle.engine.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.BattleManager;
import me.Plugins.SimpleFactions.War.battle.engine.BattleSide;

public class WarbandManager implements Listener {
	private static List<Warband> bands = new ArrayList<>();

	public static void resetForTests() {
		bands.clear();
	}

	public static Warband getByString(String s) {
		for (Warband w : bands) {
			if (w.getId().equalsIgnoreCase(s)) return w;
		}
		return null;
	}

	public static Warband getByPlayer(Player p) {
		return getByMemberId(p.getUniqueId());
	}

	public static Warband getByMemberId(java.util.UUID memberId) {
		for (Warband w : bands) {
			if (w.hasMember(memberId)) return w;
		}
		return null;
	}

	public static Warband getByLeader(Player p) {
		for (Warband w : bands) {
			if (w.getLeaderId().equals(p.getUniqueId())) return w;
		}
		return null;
	}

	public static List<Warband> get() {
		return bands;
	}

	public static void addWarband(Warband w) {
		bands.add(w);
	}

	public static void deleteWarband(Warband w) {
		bands.remove(w);
	}

	private Warband getWarbandByItem(ItemStack item) {
		ItemMeta m = item.getItemMeta();
		NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
		String id = m.getPersistentDataContainer().get(key, PersistentDataType.STRING);
		if (id != null) {
			return getByString(id);
		}
		return null;
	}

	public void start() {
		Bukkit.getLogger().info("[SimpleFactions] Starting Warband Manager");
		new BukkitRunnable() {
			public void run() {
				for (Warband w : bands) {
					List<Player> online = w.getOnlineMembers();
					if (online.isEmpty()) continue;
					friendIcon(online);
					allyIcon(online);
				}
			}
		}.runTaskTimer(SimpleFactions.plugin, 0L, 20L);
	}

	private void friendIcon(List<Player> list) {
		Particle.DustOptions dust = new Particle.DustOptions(Color.LIME, 1);
		for (Player p : list) {
			for (Player t : list) {
				if (p.equals(t)) continue;
				p.spawnParticle(Particle.DUST, t.getLocation().getX(), t.getLocation().getY() + 2, t.getLocation().getZ(), 1, dust);
			}
		}
	}

	private void allyIcon(List<Player> list) {
		Battle b = BattleManager.getBattleByPlayer(list.get(0));
		if (b == null) return;
		for (BattleSide s : b.getSides()) {
			if (!s.hasPlayer(list.get(0))) continue;
			Particle.DustOptions dust = new Particle.DustOptions(Color.BLUE, 1);
			for (Player p : list) {
				for (Warband w : s.getBands()) {
					if (w.hasMember(p)) continue;
					for (Player t : w.getOnlineMembers()) {
						p.spawnParticle(Particle.DUST, t.getLocation().getX(), t.getLocation().getY() + 2, t.getLocation().getZ(), 1, dust);
					}
				}
			}
		}
	}

	@EventHandler
	public void invenClick(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		if (e.getView().getTitle().equalsIgnoreCase("§7Warband List")) {
			e.setCancelled(true);
			if (e.getCurrentItem() == null) return;
			ItemStack warbandItem = e.getCurrentItem();
			Warband w = getWarbandByItem(warbandItem);
			if (w == null) return;
			if (w.hasMember(p)) {
				p.sendMessage("§cAlready in this warband, use §e/warband leave §cto leave it");
				p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
			} else if (WarbandManager.getByPlayer(p) != null) {
				p.sendMessage("§cAlready in a warband, leave your current warband first!");
				p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
				return;
			} else if (w.isFaction()) {
				Faction f = FactionManager.getByMember(p.getName());
				if (f == null) {
					p.sendMessage("§cThis is a faction warband, you need a faction to join");
					return;
				}
				if (!w.hasSlot(f)) {
					p.sendMessage("§cYour faction is not part of this warband");
					return;
				}
				WarbandSlot slot = w.getSlot(f);
				if (slot.isFull()) {
					p.sendMessage("§cYour faction has already filled all its slots!");
					return;
				}
				p.sendMessage("§aJoined §e" + w.getId());
				p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				w.addPlayer(p);
				slot.change(1);
			} else if (w.isLocked()) {
				if (w.isInvited(p)) {
					w.addPlayer(p);
					p.sendMessage("§aJoined §e" + w.getId());
					p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				} else {
					p.sendMessage("§cWarband is invite only");
					p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
				}
			} else {
				w.addPlayer(p);
				p.sendMessage("§aJoined §e" + w.getId());
				p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			}
		}
	}
}
