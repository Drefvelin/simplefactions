package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.List;

import javax.management.relation.Relation;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.enums.GuildModifier;
import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Guild.Branch.Branch;
import me.Plugins.SimpleFactions.Guild.Branch.BranchModifier;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.Modifier;
import me.Plugins.SimpleFactions.Utils.FactionRanker;
import me.Plugins.SimpleFactions.enums.MenuItemType;
import me.Plugins.SimpleFactions.keys.Keys;
import me.Plugins.TLibs.Enums.APIType;
import me.Plugins.TLibs.Objects.API.ItemAPI;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import me.Plugins.TLibs.TLibs;

public class GuildCreator {

	FactionRanker r = new FactionRanker();

	public ItemStack createListItem(Player p, Guild guild) {
		ItemStack i = new ItemStack(guild.getBanner());
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§f"+guild.getName());
		List<String> lore = new ArrayList<String>();
		lore.add(StringFormatter.formatHex("#7a706a§lType: "+guild.getType().getName()));
		if(guild.hasCapital()) lore.add(StringFormatter.formatHex("#c45749§lSize: #d4c9ae"+guild.getSize()));
		lore.add(StringFormatter.formatHex("#b8ae61Part of: "+guild.getFaction().getName()));
		lore.add(" ");
		lore.add(StringFormatter.formatHex("#9c9775Leader: #c2bea7"+guild.getLeader()));
		lore.add(StringFormatter.formatHex("#b8ae61Members: #7fbd73"+guild.getMembers().size()));
		lore.add(" ");
		if(guild.hasCapital()) {
			lore.add(StringFormatter.formatHex("#41b541§lTrade Power: #a4bc5c"+guild.getTradeBreakdown().getTradePower()));
			lore.add(StringFormatter.formatHex("#74ba74Estimated Income: #5cbc5c"+guild.getTradeBreakdown().getNetIncome()));
		}
		lore.add(StringFormatter.formatHex("#d1b43fWealth: #ccbb76"+guild.getWealth()+"d #7a706a("+r.getWealthRank(guild)+")"));
		meta.setLore(lore);
		NamespacedKey id = new NamespacedKey(SimpleFactions.plugin, "id");
		meta.getPersistentDataContainer().set(id, PersistentDataType.STRING, guild.getId());
		i.setItemMeta(meta);
		return i;
	}

    @SuppressWarnings("deprecation")
	public ItemStack createMenuItem(Player p, Guild guild, MenuItemType t) {
		ItemStack i = new ItemStack(Material.DIRT, 1);
		if(t.equals(MenuItemType.BANNER)) {
			i = new ItemStack(guild.getBanner());
			ItemMeta m = i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#d4c9ae§lBanner of "+guild.getName()));
            List<String> lore = new ArrayList<>();
            if(guild.isBase()) lore.add(StringFormatter.formatHex(guild.getType().getName()+" #b8ae61Guild of "+guild.getFaction().getName()));
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
			m.setDisplayName(StringFormatter.formatHex("#d1b43fWealth: #ccbb76"+guild.getWealth()+"d"));
			List<String> lore = new ArrayList<String>();
			for(Modifier mod : guild.getWealthModifiers()) {
				lore.add(StringFormatter.formatHex("#93c9a7+"+mod.getAmount()+"d from "+mod.getType()));
			}
			m.setLore(lore);
			i.setItemMeta(m);
		} else if(t.equals(MenuItemType.TRADE_BREAKDOWN)) {
			i = new ItemStack(Material.EMERALD, 1);
			ItemMeta m = i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#338651Trade Breakdown"));
			List<String> lore = new ArrayList<String>();
			lore.add(StringFormatter.formatHex("#d4c9aeIncome from trade: #7fbd73"+guild.getTradeBreakdown().getIncome()));
			lore.add(StringFormatter.formatHex("#d4c9aeUpkeep from trade: #cb5b4f"+guild.getTradeBreakdown().getUpkeep()));
			lore.add(StringFormatter.formatHex("#d4c9aeTotal Trade Power: #a4bc5c"+guild.getTradeBreakdown().getTradePower()));
			lore.add("");
			lore.add(StringFormatter.formatHex("#73adbfIncome Contributors:"));
			int x = 0;
			for(Faction f : guild.getTradeBreakdown().getFactionsByIncomeDesc()) {
				if(x == 10) break;
				x++;
				lore.add(StringFormatter.formatHex("§f - "+f.getName()+"#d4c9ae: #7fbd73"+guild.getTradeBreakdown().getIncomeByFaction(f)));
			}
			lore.add(StringFormatter.formatHex("#73adbfOther cashflows:"));
			for(Faction f : FactionManager.factions) {
				if(f.getId().equalsIgnoreCase(guild.getFaction().getId())) continue;
				for(Guild g : f.getGuildHandler().getGuilds()) {
					lore.add(StringFormatter.formatHex("§f - "+g.getName()+" §7["+g.getSize()+"§7]#d4c9ae: #7fbd73"+SimpleFactions.getInstance().getProvinceManager().getIncome(g)));
				}
			}
			m.setLore(lore);
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

	public ItemStack createBranchItem(Player p, Guild guild, Branch branch) {
		ItemStack i = branch.getIconItem();
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(branch.getName());
		List<String> lore = new ArrayList<String>();
		lore.add(StringFormatter.formatHex("#575150[#d6cf69LVL "+branch.getLevel()+"#575150]"));
		lore.add("");
		lore.addAll(branch.getDescription());
		lore.add("");
		lore.add(StringFormatter.formatHex("#a6c793Effects:"));
		for(GuildModifier m : branch.getModifierKeys()) {
			BranchModifier mod = branch.getModifier(m);
			if(mod == null) continue;
			lore.add(StringFormatter.formatHex("§f - "+m.getName()+"#d6cf69:"+(m.isPositive() ? " #4fd945" : " #cf493a")+(mod.getCurrent(branch.getLevel())+ " #575150("+(m.isPositive() ? "#4fd945" : "#cf493a")+mod.getPerLevel()+"#87807f/level#575150)")));
		}
		if(guild.isLeader(p)) {
			lore.add("");
			lore.add(StringFormatter.formatHex("#73adbfUpgrade Cost#d6cf69: #ccbb76"+guild.getExpansionCost()+"d"));
			lore.add("");

			double deltaIncome =
					SimpleFactions.getInstance()
					.getProvinceManager()
					.previewUpgradeIncomeExact(guild, branch);
			lore.add("§aCurrent §6"+SimpleFactions.getInstance()
					.getProvinceManager()
					.getIncome(guild));
			lore.add(StringFormatter.formatHex(
				"#73adbfEstimated Income#d6cf69: "
				+ (deltaIncome >= 0 ? "#4fd945+" : "#cf493a")
				+ String.format("%.2f", deltaIncome)
				+ "d/day"
			));
			lore.add(StringFormatter.formatHex("#50e846§lClick to Upgrade"));
		}
		meta.getPersistentDataContainer().set(Keys.BRANCH_ID, PersistentDataType.STRING, branch.getId());
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
}
