package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Managers.Holder.SFCombinedInventoryHolder;
import me.Plugins.SimpleFactions.Managers.Holder.WarInventoryHolder;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.core.Participant;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.War.enums.WarType;
import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.enums.SFGUI;

public class WarView {
	public InventoryManager inv;
	public WarCreator creator = new WarCreator();
	
	public WarView(InventoryManager inv) {
		this.inv = inv;
	}
	
	public void warList(Player player) {
		Inventory i = SimpleFactions.plugin.getServer().createInventory(
				new SFInventoryHolder("", SFGUI.WAR_LIST), 54, "§7War List");
		List<War> activeWars = WarManager.getActive();
		for(int x = 0; x < activeWars.size(); x++) {
			i.setItem(x, creator.createWarItem(activeWars.get(x), true));
		}
		player.openInventory(i);
	}
	
	public void warView(Inventory i, Player player, War w, boolean open) {
		w.update();
		if(open) {
			i = SimpleFactions.plugin.getServer().createInventory(new WarInventoryHolder(w.getId(), SFGUI.WAR_VIEW), 54, w.getName());
		}
		List<Integer> gray = Arrays.asList(0, 1, 2, 6, 7, 8, 45, 46, 47, 48, 50, 51, 52);
		List<Integer> red = Arrays.asList(13, 22, 31, 40, 49);
		
		List<Integer> attackerSide = Arrays.asList(9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30, 36, 37, 38, 39);
		List<Integer> defenderSide = Arrays.asList(14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35, 41, 42, 43, 44);
		
		i.setItem(3, creator.createParticipantItem(player, w.getAttackers().getMainParticipants().get(0), "main_attacker", w, false, false));
		i.setItem(4, creator.createWarItem(w, false));
		i.setItem(5, creator.createParticipantItem(player, w.getDefenders().getMainParticipants().get(0), "main_defender", w, false, false));
		
		for(int x = 0; x < w.getAttackers().getMainParticipants().size(); x++) {
			i.setItem(attackerSide.get(x), creator.createParticipantItem(player, w.getAttackers().getMainParticipants().get(x), "main_attacker", w, true, false));
		}
		
		for(int x = 0; x < w.getDefenders().getMainParticipants().size(); x++) {
			i.setItem(defenderSide.get(x), creator.createParticipantItem(player, w.getDefenders().getMainParticipants().get(x), "main_defender", w, true, false));
		}
		
		for(int x : gray) {
			i.setItem(x, inv.getFiller(Material.GRAY_STAINED_GLASS_PANE));
		}
		boolean showCampaign = canShowCampaignView(w, player);
		for(int x : red) {
			if (x == 49 && showCampaign) {
				continue;
			}
			i.setItem(x, inv.getFiller(Material.RED_STAINED_GLASS_PANE));
		}
		if (showCampaign) {
			i.setItem(49, creator.createCampaignButton(w));
		}
		i.setItem(53, inv.createBackButton(SFGUI.WAR_VIEW));
		if(open) player.openInventory(i);
	}
	
	public void participantView(Inventory i, Player player, War w, Participant p, boolean open) {
		w.update();
		if(open) {
			i = SimpleFactions.plugin.getServer().createInventory(new SFCombinedInventoryHolder(w.getId(), p.getLeader().getId(), SFGUI.PARTICIPANT_VIEW), 54, w.getName());
		}
		List<Integer> gray = Arrays.asList(0, 1, 2, 3, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52);
		
		i.setItem(4, creator.createParticipantItem(player, p, w.getType(p.getLeader()), w, false, true));
		int slots = 0;
		int offset = 9;
		for(int x = 0; x < p.getSubjects().size(); x++) {
			int slot = x+offset;
			i.setItem(slot, creator.createSecondaryItem(player, p, w, p.getSubjects().get(x), true, true));
			slots++;
		}
		offset += slots;
		List<Faction> allies = new ArrayList<>(p.getAllies().keySet());
		for(int x = 0; x < allies.size(); x++) {
			int slot = x+offset;
			i.setItem(slot, creator.createSecondaryItem(player, p, w, allies.get(x), false, p.getAllies().get(allies.get(x))));
			slots++;
		}
		for(int x : gray) {
			i.setItem(x, inv.getFiller(Material.GRAY_STAINED_GLASS_PANE));
		}
		i.setItem(53, inv.createBackButton(SFGUI.PARTICIPANT_VIEW));
		if(open) player.openInventory(i);
	}
	
	public void click(InventoryClickEvent e, Inventory inventory, Player p) {
		if(e.getView().getTitle().equalsIgnoreCase("§7War List")) {
			e.setCancelled(true);
			if (e.getCurrentItem() == null || e.getCurrentItem().getItemMeta() == null) return;
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
			Integer id = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
			if(id == null) return;
			War w = WarManager.getById(id);
			if (w == null) return;
			p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			warView(null, p, w, true);
		} else if(inventory.getHolder() instanceof WarInventoryHolder && ((WarInventoryHolder) inventory.getHolder()).getType().equals(SFGUI.WAR_VIEW)) {
			e.setCancelled(true);
			WarInventoryHolder h = (WarInventoryHolder) inventory.getHolder();
			War w = WarManager.getById(h.getId());
			if(e.getSlot() == 53) {
				warList(p);
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				return;
			} else if(e.getSlot() == 49) {
				if (!canShowCampaignView(w, p)) {
					return;
				}
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				inv.openCampaignView(p, w);
				return;
			}
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
			String id = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
			if(id == null) return;
			Faction f = FactionManager.getByString(id);
			if(f == null) return;
			Participant par = w.getParticipant(f);
			if(par == null) return;
			p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			participantView(null, p, w, par, true);
		} else if(inventory.getHolder() instanceof SFCombinedInventoryHolder && ((SFCombinedInventoryHolder) inventory.getHolder()).getType().equals(SFGUI.PARTICIPANT_VIEW)) {
			e.setCancelled(true);
			SFCombinedInventoryHolder h = (SFCombinedInventoryHolder) inventory.getHolder();
			War w = WarManager.getById(h.getWarId());
			if(e.getSlot() == 53) {
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				warView(null, p, w, true);
				return;
			}
			Faction pf = FactionManager.getByLeader(p.getName());
			if(pf == null) return;
			Participant par = w.getParticipant(pf);
			if(par == null) return;
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
			String id = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
			if(id == null) return;
			Faction f = FactionManager.getByString(id);
			if(f == null) return;
			if(par.getAllies().containsKey(f)) {
				if(par.getAllies().get(f)) return;
				if(!w.canBeCalled(f)) return;
				WarManager.sendRequest(p, pf, f, w);
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			}
		}
	}

	private boolean canShowCampaignView(War w, Player player) {
		if (w == null || !w.isActive() || w.getWarType() == WarType.RAID) {
			return false;
		}
		List<Integer> axis = w.getCampaignProvinces();
		if (axis == null || axis.isEmpty()) {
			return false;
		}
		Faction faction = FactionManager.getByLeader(player.getName());
		if (faction == null) {
			faction = FactionManager.getByMember(player.getName());
		}
		return faction != null && w.getParticipant(faction) != null;
	}
}
