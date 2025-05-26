package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class TaxCreator {
    public ItemStack createTaxButton(Faction f, boolean domestic) {
		ItemStack i = new ItemStack(Material.GOLD_INGOT, 1);
		ItemMeta meta = i.getItemMeta();
        if(domestic) {
            meta.setDisplayName(StringFormatter.formatHex("#7a915eDomestic §7Tax§e: #a39a84"+f.getTaxRate()+"%"));
        } else {
            meta.setDisplayName(StringFormatter.formatHex("#6c93bdVassal §7Tax§e: #a39a84"+f.getVassalTaxRate()+"%"));
        }
		List<String> lore = new ArrayList<String>();
        if(!domestic) {
			lore.add(StringFormatter.formatHex("#ccac41§oInfo: #4c5250§oIf a vassal has 5% tax from their relation"));
			lore.add(StringFormatter.formatHex("#4c5250§oand the overlord has a 50% vassal tax rate"));
			lore.add(StringFormatter.formatHex("#4c5250§othe vassal has a 2.5% effective tax rate"));
            lore.add("");
        }
		lore.add("§7Click to change");
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
}
