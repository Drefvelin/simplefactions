package me.Plugins.SimpleFactions.Managers.Inventory;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.enums.SFGUI;
import me.Plugins.SimpleFactions.keys.Keys;

public class GovernmentView {
    public InventoryManager inv;
	
	public GovernmentCreator creator = new GovernmentCreator();

	
	public GovernmentView(InventoryManager inv) {
        this.inv = inv;
    }

    public void governmentView(Player player, Faction f, Inventory i) {
        boolean open = i == null;
		if(i == null) i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.GOVERNMENT_VIEW), 54, "§7Government View");
		i.clear();
		i.setItem(10, creator.createGovernmentItem(f));
		i.setItem(11, creator.createStabilityItem(f));
		i.setItem(12, creator.createCouncilItem(f));
		Guild g = FactionManager.getGuildByMember(player.getName());
		if(g != null && !g.isBase()) {
			i.setItem(28, creator.createStanceItem(f, g));
		}
		i.setItem(53, inv.createBackButton(SFGUI.GOVERNMENT_VIEW));
        if(open) player.openInventory(i);
	}

	public void click(InventoryClickEvent e, Inventory inventory, Player p) {
		if (e.getView().getTitle().equalsIgnoreCase("§7Government View")) {
			e.setCancelled(true);
			int slot = e.getSlot();
			ItemStack item = e.getCurrentItem();
			Faction f = FactionManager.getByString(((SFInventoryHolder)e.getInventory().getHolder()).getId());
			if(slot == 28) {
				String id = item.getItemMeta().getPersistentDataContainer().get(Keys.STRING_KEY, PersistentDataType.STRING);
				if(id != null) {
					Guild g = FactionManager.getGuildByString(id);
					if(g != null) {
						g.switchStance();
						governmentView(p, f, inventory);
						p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
					}
				}
			}
		}
	}
}
