package me.Plugins.SimpleFactions.Managers.Inventory;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Army.Regiment;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.enums.MenuItemType;
import me.Plugins.SimpleFactions.enums.SFGUI;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class TaxView {
    private InventoryManager inv;

    private TaxCreator creator = new TaxCreator();

    public TaxView(InventoryManager inv) {
        this.inv = inv;
    }

    public void taxView(Player player) {
        Faction f = FactionManager.getByLeader(player.getName());
        if(f == null) return;
		Inventory i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.TAX_VIEW), 9, "§7Tax View");
		i.setItem(0, creator.createTaxButton(f, true));
        i.setItem(1, creator.createTaxButton(f, false));
		i.setItem(8, inv.createBackButton(SFGUI.TAX_VIEW));
		player.openInventory(i);
	}

    public void click(InventoryClickEvent e, Inventory inventory, Player p) {
		e.setCancelled(true);
        Faction f = FactionManager.getByLeader(p.getName());
        if(f == null) {
            p.closeInventory();
            return;
        }
		if(e.getSlot() == 8) {
            inv.factionView(p, f);
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
            return;
        } else if(e.getSlot() == 0) {
            if(inv.isChanging(p)) {
                return;
            }
            inv.setChanging(p, true);
            p.sendTitle("", StringFormatter.formatHex("#74ccb3Type the new rate in chat"), 10, 60, 10);
            p.closeInventory();
        } else if(e.getSlot() == 1) {
            if(inv.isChanging(p)) {
                return;
            }
            inv.setChanging(p, false);
            p.sendTitle("", StringFormatter.formatHex("#74ccb3Type the new rate in chat"), 10, 60, 10);
            p.closeInventory();
        }
	}
}
