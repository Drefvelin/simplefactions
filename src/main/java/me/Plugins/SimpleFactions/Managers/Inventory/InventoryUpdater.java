package me.Plugins.SimpleFactions.Managers.Inventory;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Managers.Holder.SFCombinedInventoryHolder;
import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.Managers.Holder.WarInventoryHolder;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.enums.SFGUI;

public class InventoryUpdater {
	InventoryManager inv;
	
	public InventoryUpdater(InventoryManager inv) {
		this.inv = inv;
	}
	
	public void updateInventory() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(p.getOpenInventory().getTopInventory() == null) continue;
			Inventory i = p.getOpenInventory().getTopInventory();
			if(i.getHolder() instanceof SFInventoryHolder) {
				SFInventoryHolder h = (SFInventoryHolder) i.getHolder();
				Faction f = FactionManager.getByString(h.getId());
				if(f == null) continue;
				if(h.getType().equals(SFGUI.MILITARY_VIEW)) {
					inv.militaryView(i, p, f, false);
				} else if(h.getType().equals(SFGUI.DIPLOMACY_VIEW)) {
					inv.diplomacyView(i, p, f, false);
				}
			} else if(i.getHolder() instanceof WarInventoryHolder) {
				WarInventoryHolder h = (WarInventoryHolder) i.getHolder();
				War w = WarManager.getById(h.getId());
				if(h.getType().equals(SFGUI.WAR_VIEW)) {
					inv.warView(i, p, w, false);
				}
			} else if(i.getHolder() instanceof SFCombinedInventoryHolder) {
				SFCombinedInventoryHolder h = (SFCombinedInventoryHolder) i.getHolder();
				Faction f = FactionManager.getByString(h.getFactionId());
				War w = WarManager.getById(h.getWarId());
				if(h.getType().equals(SFGUI.PARTICIPANT_VIEW)) {
					inv.participantView(i, p, w, w.getParticipant(f), false);
				}
			}
		}
	}
	
	public void inventorySound(String sound, SFGUI gui) {
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(p.getOpenInventory().getTopInventory() == null) continue;
			Inventory i = p.getOpenInventory().getTopInventory();
			if(!(i.getHolder() instanceof SFInventoryHolder)) continue;
			SFInventoryHolder h = (SFInventoryHolder) i.getHolder();
			if(h.getType().equals(SFGUI.MILITARY_VIEW)) {
				p.playSound(p, sound, 1f, 1f);
			}
		}
	}
}
