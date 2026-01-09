package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.enums.SFGUI;
import me.Plugins.SimpleFactions.laws.LawGroup;

public class LawView {
	public InventoryManager inv;
	
	public LawCreator creator = new LawCreator();

	private static final List<Integer> LAW_SLOTS = List.of(
		10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25
	);
	
	public LawView(InventoryManager inv) {
        this.inv = inv;
    }

    public void lawView(Player player, Faction f, Inventory i) {
		if(i == null) i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.LAW_VIEW), 54, "§7Laws");
		i.clear();
		for(int x = 0; x<f.getLawHandler().getGroupList().size(); x++) {
			LawGroup group = f.getLawHandler().getGroupList().get(x);
			i.setItem(LAW_SLOTS.get(x), creator.createLawItem(player, f, group));
		}
		player.openInventory(i);
		i.setItem(53, inv.createBackButton(SFGUI.LAW_VIEW));
	}

	public void click(InventoryClickEvent e, Inventory inventory, Player p) {
		if (e.getView().getTitle().equalsIgnoreCase("§7Laws")) {
			e.setCancelled(true);
		}
	}
}
