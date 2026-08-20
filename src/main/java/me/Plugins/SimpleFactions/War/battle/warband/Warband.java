package me.Plugins.SimpleFactions.War.battle.warband;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Army.LevyEntry;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.Participant;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class Warband {
	private String id;
	private String name;
	private UUID leaderId;
	private Set<UUID> memberIds = new LinkedHashSet<>();
	private Set<UUID> invitedIds = new HashSet<>();
	private boolean locked;
	private boolean faction;
	private HashMap<Faction, WarbandSlot> slots = new HashMap<>();

	public static Warband createWithMemberIds(String id, UUID leaderId, boolean locked, UUID... members) {
		Warband warband = new Warband();
		warband.id = id;
		warband.name = id;
		warband.leaderId = leaderId;
		warband.locked = locked;
		warband.faction = false;
		warband.memberIds.add(leaderId);
		for (UUID memberId : members) {
			if (!memberId.equals(leaderId)) {
				warband.memberIds.add(memberId);
			}
		}
		return warband;
	}

	private Warband() {
	}

	public Warband(String id, Player l) {
		this.id = id;
		this.name = id;
		this.leaderId = l.getUniqueId();
		this.memberIds.add(l.getUniqueId());
		this.locked = true;
		this.faction = false;
	}

	public Warband(War w, Participant par, boolean offense) {
		this.id = par.getLeader().getId();
		this.name = par.getLeader().getName() + " §7Host";
		Player l = Bukkit.getPlayerExact(par.getLeader().getLeader());
		if (l != null) {
			this.leaderId = l.getUniqueId();
			this.memberIds.add(l.getUniqueId());
		} else {
			this.leaderId = UUID.nameUUIDFromBytes(("warband:" + this.id).getBytes());
		}
		this.locked = true;
		this.faction = true;
		WarbandSlot slot = new WarbandSlot(par.getLeader().getMilitary().getManpowerNoLevy(offense));
		slot.change(1);
		slots.put(par.getLeader(), slot);
		for (LevyEntry entry : par.getLeader().getMilitary().getRegiment("levy").getEntries()) {
			if (w.isMainParticipant(entry.getFrom())) continue;
			slots.put(entry.getFrom(), new WarbandSlot(entry.getAmount()));
		}
		for (Faction f : par.getAllies().keySet()) {
			if (par.getAllies().get(f)) {
				slots.put(f, new WarbandSlot(f.getMilitary().getManpower(true)));
			}
		}
		if (l != null) {
			for (Faction f : slots.keySet()) {
				for (String p : f.getMembers()) {
					Player m = Bukkit.getPlayerExact(p);
					if (m != null && m.isOnline() && !m.equals(l)) {
						m.sendTitle(
								StringFormatter.formatHex("#449459Muster Call!"),
								StringFormatter.formatHex("#a89d80Your faction is mustering an army, join in #c4904b/warband list"),
								5, 80, 20);
						m.playSound(m, Sound.ITEM_GOAT_HORN_SOUND_2, SoundCategory.MASTER, 10f, 0.6f);
					}
				}
			}
		}
	}

	public void addPlayer(Player p) {
		memberIds.add(p.getUniqueId());
		invitedIds.remove(p.getUniqueId());
	}

	public void addMember(UUID memberId) {
		memberIds.add(memberId);
	}

	public void removeMember(UUID memberId) {
		memberIds.remove(memberId);
	}

	public void removePlayer(Player p) {
		if (p != null) {
			removeMember(p.getUniqueId());
		}
	}

	public void uninvite(Player p) {
		if (p != null) {
			invitedIds.remove(p.getUniqueId());
		}
	}

	public void invite(Player p) {
		invitedIds.add(p.getUniqueId());
	}

	public boolean isInvited(Player p) {
		return invitedIds.contains(p.getUniqueId());
	}

	public boolean hasMember(Player p) {
		return memberIds.contains(p.getUniqueId());
	}

	public boolean hasMember(UUID memberId) {
		return memberIds.contains(memberId);
	}

	public int getMemberCount() {
		return memberIds.size();
	}

	public java.util.Collection<UUID> getMemberIds() {
		return java.util.Collections.unmodifiableSet(memberIds);
	}

	public List<Player> getOnlineMembers() {
		List<Player> online = new ArrayList<>();
		for (UUID memberId : memberIds) {
			Player p = Bukkit.getPlayer(memberId);
			if (p != null && p.isOnline()) {
				online.add(p);
			}
		}
		return online;
	}

	public String getId() {
		return this.id;
	}

	public String getName() {
		return name;
	}

	public Player getLeader() {
		return Bukkit.getPlayer(leaderId);
	}

	public UUID getLeaderId() {
		return leaderId;
	}

	public List<Player> getPlayers() {
		return getOnlineMembers();
	}

	public List<Player> getInvited() {
		List<Player> invited = new ArrayList<>();
		for (UUID invitedId : invitedIds) {
			Player p = Bukkit.getPlayer(invitedId);
			if (p != null && p.isOnline()) {
				invited.add(p);
			}
		}
		return invited;
	}

	public boolean isLocked() {
		return locked;
	}

	public boolean isFaction() {
		return faction;
	}

	public void setLocked(boolean b) {
		this.locked = b;
	}

	public void setLeader(Player p) {
		this.leaderId = p.getUniqueId();
	}

	public HashMap<Faction, WarbandSlot> getSlots() {
		return slots;
	}

	public boolean hasSlot(Faction f) {
		return slots.containsKey(f);
	}

	public WarbandSlot getSlot(Faction f) {
		if (!hasSlot(f)) return null;
		return slots.get(f);
	}
}
