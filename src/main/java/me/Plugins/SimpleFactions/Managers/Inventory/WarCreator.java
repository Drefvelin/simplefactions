package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Army.LevyEntry;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.Participant;
import me.Plugins.SimpleFactions.War.Side;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.WarGoal;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.Warbands.Managers.WarbandManager;

public class WarCreator {
	public ItemStack createMusterItem(Participant par) {
		ItemStack i = new ItemStack(Material.IRON_HELMET, 1);
		
		if(IconGetter.hasIcon("muster")) i = IconGetter.getIcon("muster");
		ItemMeta m = i.getItemMeta();
		m.setDisplayName(StringFormatter.formatHex("#7fbd73Muster Army"));
		m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		List<String> lore = new ArrayList<>();
		if(WarbandManager.getByString(par.getLeader().getId()) != null) {
			lore.add(StringFormatter.formatHex("#a39ba8Already mustered! View in #d4c9ae/warband list"));
		} else {
			lore.add(StringFormatter.formatHex("#d4c9aeClick to create a faction warband"));
		}
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}
	
	public ItemStack createWarGoalItem(WarGoal goal, Faction target, boolean main) {
		ItemStack i = new ItemStack(Material.EMERALD, 1);
		
		if(IconGetter.hasIcon(goal.getId())) i = IconGetter.getIcon(goal.getId());
		ItemMeta m = i.getItemMeta();
		m.setDisplayName(goal.getName());
		List<String> lore = new ArrayList<>();
		for(String s : goal.getDescription()) {
			lore.add(s);
		}
		lore.add("");
		lore.add(StringFormatter.formatHex("#ba4b3aClick to select this war goal"));
		if(!main) {
			lore.add("");
			lore.add(StringFormatter.formatHex(target.getName()+" #a39ba8is a #65e0bbSecondary Participant#a39ba8!"));
			lore.add(StringFormatter.formatHex("#a39ba8Adding a war goal on them will make them a #f5ef42Main Parcitipant"));
			lore.add(StringFormatter.formatHex("#a39ba8This means they can call their own allies into the war!"));
		}
		m.setLore(lore);
		NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
		m.getPersistentDataContainer().set(key, PersistentDataType.STRING, target.getId());
		key = new NamespacedKey(SimpleFactions.plugin, "goal");
		m.getPersistentDataContainer().set(key, PersistentDataType.STRING, goal.getId());
		i.setItemMeta(m);
		return i;
	}
	public ItemStack createWarItem(War w, boolean button) {
		ItemStack i = new ItemStack(Material.BLAZE_POWDER, 1);
		if(IconGetter.hasIcon("war")) i = IconGetter.getIcon("war");
		ItemMeta m = i.getItemMeta();
		m.setDisplayName(w.getName());
		List<String> lore = new ArrayList<>();
		if(button) {
			lore.add(StringFormatter.formatHex("#28ed70Click to view"));
		} else {
			lore.add(StringFormatter.formatHex("#535955ID: "+w.getId()));
		}
		m.setLore(lore);
		NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
		m.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, w.getId());
		i.setItemMeta(m);
		return i;
	}
	
	public ItemStack createSwitchItem() {
		ItemStack i = new ItemStack(Material.BLAZE_POWDER, 1);
		if(IconGetter.hasIcon("switch")) i = IconGetter.getIcon("switch");
		ItemMeta m = i.getItemMeta();
		m.setDisplayName(StringFormatter.formatHex("#cc6f23Switch Sides"));
		List<String> lore = new ArrayList<>();
		lore.add(StringFormatter.formatHex("#a1aba3(Independence Rebellion)"));
		lore.add(" ");
		lore.add(StringFormatter.formatHex("#c73818§lClick to switch sides"));
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}
	
	public ItemStack createSecondaryItem(Player p, Participant par, War w, Faction f, boolean subject, boolean called) {
		Faction pf = FactionManager.getByLeader(p.getName());
		ItemStack i = new ItemStack(f.getBanner());
		ItemMeta m = i.getItemMeta();
		m.setDisplayName(f.getName());
		NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
		m.getPersistentDataContainer().set(key, PersistentDataType.STRING, f.getId());
		List<String> lore = new ArrayList<>();
		lore.add(StringFormatter.formatHex("#65e0bbSecondary Participant"));
		lore.add(" ");
		if(!subject) {
			lore.add(StringFormatter.formatHex("#a39ba8Soldiers: #28ed70"+f.getMilitary().getManpower(true)));
		} else {
			Faction overlord = FactionManager.getByString(RelationManager.getOverlord(f));
			LevyEntry e = overlord.getMilitary().getRegiment("levy").getEntry(f);
			int amount = 0;
			if(e != null) {
				amount = e.getAmount();
			}
			lore.add(StringFormatter.formatHex("#a39ba8Contributes: #28ed70"+amount+" #a39ba8Soldiers"));
		}
		if(subject) {
			lore.add(" ");
			lore.add(StringFormatter.formatHex("#d4c9aeThis nation is a subject"));
			lore.add(StringFormatter.formatHex("#d4c9aeand is therefore automatically called"));
			
		}
		lore.add(" ");
		if(called) {
			lore.add(StringFormatter.formatHex("#2757cc§lCalled!"));
		} else {
			lore.add(StringFormatter.formatHex("#8a4152Not Called!"));
			if(pf != null && par.getLeader().getId().equalsIgnoreCase(pf.getId())) {
				lore.add(StringFormatter.formatHex("#28ed70§lClick to call!"));
			}
		}
		if(!w.getSide(pf).equals(w.getSide(f)) && w.getSide(f) != null) {
			lore.add(" ");
			lore.add(StringFormatter.formatHex("#8a4152§o§lClick to set a war goal!"));
		}
		
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}
	
	public ItemStack createParticipantItem(Player p, Participant par, String type, War w, boolean full, boolean warGoal) {
		Faction pf = FactionManager.getByLeader(p.getName());
		Faction f = par.getLeader();
		ItemStack i = new ItemStack(f.getBanner());
		ItemMeta m = i.getItemMeta();
		m.setDisplayName(f.getName());
		NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
		m.getPersistentDataContainer().set(key, PersistentDataType.STRING, par.getLeader().getId());
		List<String> lore = new ArrayList<>();
		if(type.equalsIgnoreCase("main_attacker")) {
			lore.add(StringFormatter.formatHex("#f5ef42Main Attacker"));
		} else if(type.equalsIgnoreCase("main_defender")) {
			lore.add(StringFormatter.formatHex("#f5ef42Main Defender"));
		} else {
			lore.add(StringFormatter.formatHex("#65e0bbSecondary Participant"));
		}
		lore.add(" ");
		Side s = w.getSide(f);
		if(type.equalsIgnoreCase("main_defender")) {
			if(!full && !warGoal) lore.add(StringFormatter.formatHex("#a39ba8Soldiers: #28ed70"+s.getTotalManpower(false)));
			else lore.add(StringFormatter.formatHex("#a39ba8Soldiers: #28ed70"+f.getMilitary().getManpower(false)));
		} else {
			if(!full && !warGoal) lore.add(StringFormatter.formatHex("#a39ba8Soldiers: #28ed70"+s.getTotalManpower(true)));
			else lore.add(StringFormatter.formatHex("#a39ba8Soldiers: #28ed70"+f.getMilitary().getManpower(true)));
		}
		if(full && !type.equalsIgnoreCase("secondary_participant")) {
			if(par.getAllies().size() > 0 || par.getSubjects().size() > 0) {
				lore.add(" ");
				lore.add(StringFormatter.formatHex("#65e0bbSecondary Participants:"));
				if(par.getAllies().size() > 0) lore.add(StringFormatter.formatHex("§7- #975bbdAllies: #bea1d1"+par.getAllies().size()));
				if(par.getSubjects().size() > 0) lore.add(StringFormatter.formatHex("§7- #768fccSubjects: #a3afcc"+par.getSubjects().size()));
				lore.add(" ");
				lore.add(StringFormatter.formatHex("#28ed70Click to view"));
			} else {
				lore.add(" ");
				lore.add(StringFormatter.formatHex("#9e4c4fNo Secondary Participants"));
			}
		}
		if(full || warGoal) {
			if(pf != null) {
				Participant pp = w.getParticipant(pf);
				if(pp != null) {
					if(!w.getSide(par).equals(w.getSide(pp))) {
						lore.add(" ");
						lore.add(StringFormatter.formatHex("#a39ba8Your War Goal§7:"));
						if(pp.hasWarGoal(f)) {
							lore.add(pp.getWarGoal(f).getName());
						} else {
							lore.add(StringFormatter.formatHex("#8a4152§oNot Set!"));
							if(warGoal) {
								lore.add(StringFormatter.formatHex("#8a4152§o§lClick to set a war goal!"));
							}
						}
					}
				}
				HashMap<Faction, WarGoal> otherGoals = w.getWarGoalsOn(f);
				otherGoals.remove(pf);
				if(otherGoals.size() > 0) {
					lore.add(" ");
					lore.add(StringFormatter.formatHex("#a39ba8Other War Goals§7:"));
					for(Faction g : otherGoals.keySet()) {
						lore.add(g.getName()+" §f- "+otherGoals.get(g).getName());
					}
				}
			}
		}
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}
}
