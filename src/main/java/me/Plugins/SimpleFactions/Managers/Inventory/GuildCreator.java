package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.enums.MenuItemType;
import me.Plugins.TLibs.Enums.APIType;
import me.Plugins.TLibs.Objects.API.ItemAPI;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import me.Plugins.TLibs.TLibs;

public class GuildCreator {
    @SuppressWarnings("deprecation")
	public ItemStack createMenuItem(Player p, Guild guild, MenuItemType t) {
		ItemStack i = new ItemStack(Material.DIRT, 1);
		if(t.equals(MenuItemType.BANNER)) {
			i = new ItemStack(guild.getBanner());
			ItemMeta m = i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#d4c9ae§lBanner of "+guild.getName()));
            List<String> lore = new ArrayList<>();
            if(guild.isBase()) lore.add(StringFormatter.formatHex(guild.getType().getName()+"#b8ae61Guild of "+guild.getFaction().getName()));
			else lore.add(StringFormatter.formatHex("#b8ae61Part of: "+guild.getFaction().getName()));
            m.setLore(lore);
			i.setItemMeta(m);
		} else if(t.equals(MenuItemType.BANNER_GET)) {
			i = new ItemStack(Material.CHEST, 1);
			ItemMeta m = i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#82d461Get Banner"));
			List<String> lore = new ArrayList<>();
			lore.add("§7Click to get a banner");
			m.setLore(lore);
			i.setItemMeta(m);
		} else if(t.equals(MenuItemType.BANNER_RANDOM)) {
			ItemAPI api = (ItemAPI) TLibs.getApiInstance(APIType.ITEM_API);
			i = api.getCreator().getItemsAdderItem("mcicons:icon_refresh");
			ItemMeta m = i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#9161d4Randomise Banner"));
			List<String> lore = new ArrayList<>();
			lore.add("§7Click to randomise the banner patterns");
			m.setLore(lore);
			i.setItemMeta(m);
		} else if(t.equals(MenuItemType.LEADER)) {
			i = new ItemStack(Material.PLAYER_HEAD, 1);
			SkullMeta m = (SkullMeta) i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#9c9775§lLeader: #c2bea7"+guild.getLeader()));
			m.setOwningPlayer(Bukkit.getOfflinePlayer(guild.getLeader()));
			i.setItemMeta(m);
		} else if(t.equals(MenuItemType.WEALTH)) {
			i = new ItemStack(Material.GOLD_NUGGET, 1);
			ItemMeta m = i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#d1b43fWealth: #ccbb76N/A (not implemented)"));
			i.setItemMeta(m);
		} else if(t.equals(MenuItemType.MEMBERS)) {
				i = new ItemStack(Material.PLAYER_HEAD, 1);
				ItemMeta m = i.getItemMeta();
				m.setDisplayName(StringFormatter.formatHex("#b8ae61Members: #7fbd73"+guild.getMembers().size()+"/"+Cache.maxMembers));
				List<String> lore = new ArrayList<>();
				for(String s : guild.getMembers()) {
					lore.add(StringFormatter.formatHex("#d4c9ae"+s));
				}
				m.setLore(lore);
				i.setItemMeta(m);
		}
		if(IconGetter.hasIcon(t.toString())) {
			ItemStack icon = IconGetter.getIcon(t.toString());
			i.setType(icon.getType());
			ItemMeta m = i.getItemMeta();
			m.setCustomModelData(icon.getItemMeta().getCustomModelData());
			i.setItemMeta(m);
		}
		return i;
	}
}
