package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Loaders.WarGoalLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Managers.Holder.SFCombinedInventoryHolder;
import me.Plugins.SimpleFactions.Managers.Holder.WarInventoryHolder;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.Participant;
import me.Plugins.SimpleFactions.War.Side;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.WarGoal;
import me.Plugins.SimpleFactions.enums.SFGUI;
import net.tfminecraft.Warbands.Managers.WarbandManager;
import net.tfminecraft.Warbands.Objects.Warband;

public class WarView {
	public InventoryManager inv;
	public WarCreator creator = new WarCreator();
	
	public WarView(InventoryManager inv) {
		this.inv = inv;
	}
	
	public void warList(Player player) {
		Inventory i = SimpleFactions.plugin.getServer().createInventory(null, 54, "§7War List");
		for(int x = 0; x<WarManager.get().size(); x++) {
			i.setItem(x, creator.createWarItem(WarManager.get().get(x), true));
		}
		player.openInventory(i);
	}
	
	public void warView(Inventory i, Player player, War w, boolean open) {
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
		for(int x : red) {
			i.setItem(x, inv.getFiller(Material.RED_STAINED_GLASS_PANE));
		}
		Faction f = FactionManager.getByLeader(player.getName());
		if(f != null) {
			if(w.getParticipant(f) != null) {
				i.setItem(13, creator.createMusterItem(w.getParticipant(f)));
			}
			if(w.canSwitchSides(f)) i.setItem(31, creator.createSwitchItem());
		}
		i.setItem(53, inv.createBackButton(SFGUI.WAR_VIEW));
		if(open) player.openInventory(i);
	}
	
	public void warGoalView(Inventory i, Player player, War w, Faction target, Faction page, boolean open) {
		if(open) {
			i = SimpleFactions.plugin.getServer().createInventory(new SFCombinedInventoryHolder(w.getId(), page.getId(), SFGUI.WARGOAL_VIEW), 27, w.getName());
		}
		Faction from = FactionManager.getByLeader(player.getName());
		if(from == null) return;
		int slot = 0;
		boolean main = w.isMainParticipant(target);
		for(WarGoal goal : WarGoalLoader.get()) {
			if(!goal.canTarget(w, from, target)) continue;
			i.setItem(slot, creator.createWarGoalItem(goal, target, main));
			slot++;
		}
		i.setItem(26, inv.createBackButton(SFGUI.WARGOAL_VIEW));
		if(open) player.openInventory(i);
	}
	
	public void participantView(Inventory i, Player player, War w, Participant p, boolean open) {
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
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
			Integer id = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
			if(id == null) return;
			War w = WarManager.getById(id);
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
			} else if(e.getSlot() == 13) {
				Faction pf = FactionManager.getByLeader(p.getName());
				if(pf == null) return;
				Participant par = w.getParticipant(pf);
				if(par == null) return;
				if(WarbandManager.getByString(par.getLeader().getId()) != null) return;
				p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_2, SoundCategory.MASTER, 10f, 0.6f);
				boolean offense = false;
				if(w.getType(pf).equalsIgnoreCase("main_attacker")) offense = true;
				if(par.isCivilWar()) offense = false;
				WarbandManager.addWarband(new Warband(par, offense));
				net.tfminecraft.Warbands.Managers.InventoryManager warinv = new net.tfminecraft.Warbands.Managers.InventoryManager();
				warinv.warbandList(p);
			} else if(e.getSlot() == 31) {
				Faction pf = FactionManager.getByLeader(p.getName());
				if(pf == null) return;
				Participant par = w.getParticipant(pf);
				if(par != null) return;
				String o = RelationManager.getOverlord(pf);
				if(o == null) return;
				Faction overlord = FactionManager.getByString(o);
				if(w.getParticipant(overlord) != null) {
					Participant oPar = w.getParticipant(overlord);
					RelationManager.reset(pf, overlord, true);
					Participant subject = w.getOppositeSide(overlord).addNewParticipant(pf, oPar);
					subject.setCivilWar(true);
					subject.addWarGoal(overlord, WarGoalLoader.getByString("independence"));
					oPar.setCivilWar(true);
					oPar.addWarGoal(pf, WarGoalLoader.getByString("subjugate"));
					p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
					warView(inventory, p, w, false);
					return;
				}
				Faction top = FactionManager.getByString(RelationManager.getTopLiege(pf));
				if(top.getId().equalsIgnoreCase(overlord.getId())) return;
				if(w.getParticipant(top) != null) {
					Participant newPar = w.getSide(top).addNewParticipant(overlord, w.getParticipant(top));
					newPar.setCivilWar(true);
					RelationManager.reset(pf, overlord, true);
					Participant subject = w.getOppositeSide(top).addNewParticipant(pf, newPar);
					subject.setCivilWar(true);
					subject.addWarGoal(overlord, WarGoalLoader.getByString("independence"));
					newPar.addWarGoal(pf, WarGoalLoader.getByString("subjugate"));
					p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
					warView(inventory, p, w, false);
					return;
				}
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
			if(!w.getSide(f).equals(w.getSide(pf))) {
				Faction page = FactionManager.getByString(h.getFactionId());
				warGoalView(null, p, w, f, page, true);
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			}
		} else if(inventory.getHolder() instanceof SFCombinedInventoryHolder && ((SFCombinedInventoryHolder) inventory.getHolder()).getType().equals(SFGUI.WARGOAL_VIEW)) {
			e.setCancelled(true);
			SFCombinedInventoryHolder h = (SFCombinedInventoryHolder) inventory.getHolder();
			War w = WarManager.getById(h.getWarId());
			Faction page = FactionManager.getByString(h.getFactionId());
			if(e.getSlot() == 26) {
				participantView(null, p, w, w.getParticipant(page), true);
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				return;
			}
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
			String id = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
			if(id == null) return;
			key = new NamespacedKey(SimpleFactions.plugin, "goal");
			String goal = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
			if(goal == null) return;
			Faction f = FactionManager.getByString(id);
			Faction pf = FactionManager.getByLeader(p.getName());
			if(pf == null) return;
			Participant par = w.getParticipant(pf);
			if(par == null) return;
			Side s = w.getOppositeSide(pf);
			if(s == null) return;
			WarGoal warGoal = WarGoalLoader.getByString(goal);
			if(!warGoal.canTarget(w, pf, f)) return;
			if(!w.isMainParticipant(f)) {
				s.addNewParticipant(f, w.getParticipant(page));
			}
			par.addWarGoal(f, warGoal);
			
			participantView(null, p, w, w.getParticipant(page), true);
			p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
		}
	}
}
