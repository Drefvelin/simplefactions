package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Diplomacy.Relation;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Loaders.RankLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.FactionModifier;
import me.Plugins.SimpleFactions.Objects.Modifier;
import me.Plugins.SimpleFactions.Objects.PrestigeRank;
import me.Plugins.SimpleFactions.Tiers.Title;
import me.Plugins.SimpleFactions.Utils.FactionRanker;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.Utils.OpinionColourMapper;
import me.Plugins.SimpleFactions.enums.FactionModifiers;
import me.Plugins.SimpleFactions.enums.MenuItemType;
import me.Plugins.SimpleFactions.enums.RankType;
import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Enums.APIType;
import me.Plugins.TLibs.Objects.API.ItemAPI;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class FactionCreator {
	Formatter format = new Formatter();
	FactionRanker r = new FactionRanker();
	
	public ItemStack createListItem(Player p, Faction f) {
		ItemStack i = new ItemStack(f.getBanner());
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(f.getName());
		List<String> lore = new ArrayList<String>();
		lore.add(f.getRank().getName());
		if(f.getTitles().size() > 0) lore.add(StringFormatter.formatHex("#b84c44§lPrimary Title: #7a706a"+f.getHighestTitle().getName()));
		lore.add(StringFormatter.formatHex("#c45749§lTier: "+f.getTier().getFormattedName()));
		int realmSize = TitleManager.getRealmSize(f);
		if(realmSize > 0 && realmSize-f.getProvinces().size() > 0) lore.add(StringFormatter.formatHex("#d4c9aeRealm Size: #7a706a"+realmSize+" #a39ba8("+(realmSize-f.getProvinces().size())+" from subjects)"));
		else if(realmSize > 0) lore.add(StringFormatter.formatHex("#d4c9aeRealm Size: #7a706a"+realmSize));
		lore.add(" ");
		lore.add(StringFormatter.formatHex("#9c9775"+f.getRulerTitle()+": #c2bea7"+f.getLeader()));
		lore.add(StringFormatter.formatHex("#b8ae61Ruling System: #d4c9ae"+f.getGovernment()));
		lore.add(StringFormatter.formatHex("#b8ae61Culture: #d4c9ae"+f.getCulture()));
		lore.add(StringFormatter.formatHex("#b8ae61Religion: #d4c9ae"+f.getReligion()));
		lore.add(StringFormatter.formatHex("#b8ae61Members: #7fbd73"+f.getMembers().size()));
		lore.add(" ");
		lore.add(StringFormatter.formatHex("#4793bfPrestige: #6eafba"+f.getPrestige()+" #7a706a("+r.getPrestigeRank(f)+")"));
		lore.add(StringFormatter.formatHex("#d1b43fWealth: #ccbb76"+f.getWealth()+"d #7a706a("+r.getWealthRank(f)+")"));
		double prosperity = f.getProsperity();
		if(prosperity > 0) lore.add(StringFormatter.formatHex("#4bb244Prosperity: #4fd945"+f.getProsperity()));
		if(!f.getMembers().contains(p.getName())) {
			Faction origin = FactionManager.getByMember(p.getName());
			if(origin != null) {
				Relation r = origin.getRelation(f.getId());
				Relation ofR = f.getRelation(origin.getId());
				lore.add(" ");
				lore.add(StringFormatter.formatHex("#7fbd73§lDiplomacy:"));
				if(r.getType().equals(ofR.getType())) {
					lore.add(StringFormatter.formatHex("#d4bb98Relation: "+r.getType().getName()+" #a39ba8(mutual)"));
				} else {
					lore.add(StringFormatter.formatHex("#d4bb98Relation: "+r.getType().getName()+" #a39ba8(outgoing)"));
					lore.add(StringFormatter.formatHex("#d4bb98Relation: "+ofR.getType().getName()+" #a39ba8(incoming)"));
				}
				lore.add(StringFormatter.formatHex("#a39ba8Our opinion of them: "+OpinionColourMapper.getOpinionColor(r.getOpinion())+r.getOpinion()));
				lore.add(StringFormatter.formatHex("#a39ba8Their opinion of us: "+OpinionColourMapper.getOpinionColor(ofR.getOpinion())+ofR.getOpinion()));
			}
		}
		List<Guild> guilds = f.getGuildHandler().getGuilds();
		if(guilds.size() > 0){
			lore.add("");
			lore.add(StringFormatter.formatHex("#d6a376Guilds:"));
			for(Guild guild : guilds) {
				lore.add(StringFormatter.formatHex("#bccbd1- "+guild.getName()+" #a39ba8("+guild.getType().getName()+"#a39ba8)"));
			}
		}
		List<Faction> subjects = RelationManager.getSubjects(f);
		if(subjects.size() > 0){
			lore.add("");
			lore.add(StringFormatter.formatHex("#5eadccSubjects:"));
			for(Faction subject : subjects) {
				lore.add(StringFormatter.formatHex("#bccbd1- "+subject.getName()));
			}
		}
		List<Faction> allies = RelationManager.getAllies(f);
		if(allies.size() > 0){
			lore.add("");
			lore.add(StringFormatter.formatHex("#975bbdAllies:"));
			for(Faction ally : allies) {
				lore.add(StringFormatter.formatHex("#bccbd1- "+ally.getName()));
			}
		}
		meta.setLore(lore);
		NamespacedKey id = new NamespacedKey(SimpleFactions.plugin, "id");
		meta.getPersistentDataContainer().set(id, PersistentDataType.STRING, f.getId());
		i.setItemMeta(meta);
		return i;
	}
	
	public ItemStack createRankButton(RankType t) {
		ItemStack i = new ItemStack(Material.BLAZE_POWDER, 1);
		if(t.equals(RankType.PRESTIGE)) {
			i.setType(Material.DIAMOND);
		} else if(t.equals(RankType.WEALTH)) {
			i.setType(Material.GOLD_NUGGET);
		} else {
			i.setType(Material.PLAYER_HEAD);
		}
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§aCurrently ranking based on §e"+t.toString());
		List<String> lore = new ArrayList<String>();
		lore.add("§7Click to change");
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	
	@SuppressWarnings("deprecation")
	public ItemStack createMenuItem(Player p, Faction f, MenuItemType t) {
		ItemStack i = new ItemStack(Material.DIRT, 1);
		if(t.equals(MenuItemType.BANNER)) {
			i = new ItemStack(f.getBanner());
			ItemMeta m = i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#d4c9ae§lBanner of "+f.getName()));
			List<String> lore = new ArrayList<String>();
			lore.add(StringFormatter.formatHex("#b8ae61Culture: #d4c9ae"+f.getCulture()));
			lore.add(StringFormatter.formatHex("#b8ae61Religion: #d4c9ae"+f.getReligion()));
			m.setLore(lore);
			i.setItemMeta(m);
		} else if(t.equals(MenuItemType.BANNER_GET)) {
			i = new ItemStack(Material.CHEST, 1);
			ItemMeta m = i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#82d461Get Banner"));
			List<String> lore = new ArrayList<String>();
			lore.add("§7Click to get a banner");
			m.setLore(lore);
			i.setItemMeta(m);
		} else if(t.equals(MenuItemType.BANNER_RANDOM)) {
			ItemAPI api = (ItemAPI) TLibs.getApiInstance(APIType.ITEM_API);
			i = api.getCreator().getItemsAdderItem("mcicons:icon_refresh");
			ItemMeta m = i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#9161d4Randomise Banner"));
			List<String> lore = new ArrayList<String>();
			lore.add("§7Click to randomise the banner patterns");
			m.setLore(lore);
			i.setItemMeta(m);
		} else if(t.equals(MenuItemType.LEADER)) {
			i = new ItemStack(Material.PLAYER_HEAD, 1);
			SkullMeta m = (SkullMeta) i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#9c9775§l"+f.getRulerTitle()+": #c2bea7"+f.getLeader()));
			m.setOwningPlayer(Bukkit.getOfflinePlayer(f.getLeader()));
			List<String> lore = new ArrayList<String>();
			lore.add(StringFormatter.formatHex("#b8ae61Ruling System: #d4c9ae"+f.getGovernment()));
			m.setLore(lore);
			i.setItemMeta(m);
		} else if(t.equals(MenuItemType.WEALTH)) {
			i = new ItemStack(Material.GOLD_NUGGET, 1);
			ItemMeta m = i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#d1b43fWealth: #ccbb76"+f.getWealth()+"d"));
			List<String> lore = new ArrayList<String>();
			for(Modifier mod : f.getWealthModifiers()) {
				lore.add(StringFormatter.formatHex("#93c9a7+"+mod.getAmount()+"d from "+mod.getType()));
			}
			m.setLore(lore);
			i.setItemMeta(m);
		} else if(t.equals(MenuItemType.PRESTIGE)) {
			i = new ItemStack(Material.DIAMOND, 1);
			ItemMeta m = i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#4793bfPrestige: #6eafba"+f.getPrestige()));
			List<String> lore = new ArrayList<String>();
			for(Modifier mod : f.getPrestigeModifiers()) {
				lore.add(StringFormatter.formatHex("#93c9a7+"+mod.getAmount()+" from "+mod.getType()));
			}
			lore.add(" ");
			lore.add(StringFormatter.formatHex("#7fbd73Current Rank: "+f.getRank().getName()));
			if(f.getRank().getLevel() < RankLoader.getRanks().size()) {
				PrestigeRank rank = RankLoader.getByLevel(f.getRank().getLevel()+1);
				if(rank.getAn()) {
					lore.add(StringFormatter.formatHex("#d4c9aeFaction needs at least #7fbd73"+FactionManager.getRankUpAmount(rank)+" #4793bfPrestige"));
					lore.add(StringFormatter.formatHex("#d4c9aeto become an "+rank.getName()));
				} else {
					lore.add(StringFormatter.formatHex("#d4c9aeFaction needs at least #7fbd73"+FactionManager.getRankUpAmount(rank)+" #4793bfPrestige"));
					lore.add(StringFormatter.formatHex("#d4c9aeto become a "+rank.getName()));
				}
			}
			if(f.getRank().getLevel() != 1) {
				lore.add(" ");
				PrestigeRank rank = RankLoader.getByLevel(f.getRank().getLevel()-1);
				if(rank.getAn()) {
					lore.add(StringFormatter.formatHex("#d4c9aeIf the Faction falls below #7fbd73"+(FactionManager.getRankUpAmount(RankLoader.getByLevel(f.getRank().getLevel()))*0.95)+" #4793bfPrestige"));
					lore.add(StringFormatter.formatHex("#d4c9aethe faction will become an "+rank.getName()));
				} else {
					lore.add(StringFormatter.formatHex("#d4c9aeIf the Faction falls below #7fbd73"+(format.formatDouble(FactionManager.getRankUpAmount(RankLoader.getByLevel(f.getRank().getLevel()))*0.95))+" #4793bfPrestige"));
					lore.add(StringFormatter.formatHex("#d4c9aethe faction will become a "+rank.getName()));
				}
			}
			m.setLore(lore);
			i.setItemMeta(m);
		} else if(t.equals(MenuItemType.MEMBERS)) {
				i = new ItemStack(Material.PLAYER_HEAD, 1);
				ItemMeta m = i.getItemMeta();
				m.setDisplayName(StringFormatter.formatHex("#b8ae61Members: #7fbd73"+f.getMembers().size())); //TODO full subject population as well cause cool
				List<String> lore = new ArrayList<String>();
				for(String s : f.getMembers()) {
					lore.add(StringFormatter.formatHex("#d4c9ae"+s));
				}
				m.setLore(lore);
				i.setItemMeta(m);
		} else if(t.equals(MenuItemType.MILITARY)) {
				i = new ItemStack(Material.IRON_SWORD, 1);
				ItemMeta m = i.getItemMeta();
				m.setDisplayName(StringFormatter.formatHex("#a6659fMilitary"));
				List<String> lore = new ArrayList<String>();
				lore.add("§7Click to view Military");
				m.setLore(lore);
				NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
				m.getPersistentDataContainer().set(key, PersistentDataType.STRING, f.getId());
				i.setItemMeta(m);
		} else if(t.equals(MenuItemType.DIPLOMACY)) {
			i = new ItemStack(Material.WRITABLE_BOOK, 1);
			ItemMeta m = i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#35f2bdDiplomacy"));
			List<String> lore = new ArrayList<String>();
			lore.add("§7Click to view Diplomacy");
			m.setLore(lore);
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
			m.getPersistentDataContainer().set(key, PersistentDataType.STRING, f.getId());
			i.setItemMeta(m);
		} else if(t.equals(MenuItemType.TIER)) {
			ItemMeta m = i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#c45749§lTier: "+f.getTier().getFormattedName()));
			if(f.getLeader().equalsIgnoreCase(p.getName())) {
				List<String> lore = new ArrayList<String>();
				lore.add("§7Click to edit Tier");
				m.setLore(lore);
			}
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
			m.getPersistentDataContainer().set(key, PersistentDataType.STRING, f.getId());
			i.setItemMeta(m);
		} else if(t.equals(MenuItemType.TITLES)) {
			ItemMeta m = i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#8acc6eTitles"));
			List<String> lore = new ArrayList<String>();
			if(f.getLeader().equalsIgnoreCase(p.getName())) {
				lore.add("§7Click to view Titles");
			} else {
				Faction pf = FactionManager.getByLeader(p.getName());
				String overlord = RelationManager.getOverlord(f);
				if(pf != null && overlord != null && overlord.equalsIgnoreCase(pf.getId())) {
					lore.add("§7Click to view Titles to grant to "+f.getName());
				} else {
					for(Title title : f.getRankedTitles()) {
						lore.add(StringFormatter.formatHex("§7- #d4bb98"+title.getName()+" §7("+title.getTier().getFormattedName()+"§7)"));
					}
				}
			}
			m.setLore(lore);
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
			m.getPersistentDataContainer().set(key, PersistentDataType.STRING, f.getId());
			i.setItemMeta(m);
		} else if(t.equals(MenuItemType.MODIFIERS)) {
			i = new ItemStack(Material.GOLDEN_APPLE, 1);
			ItemMeta m = i.getItemMeta();
			m.setDisplayName(StringFormatter.formatHex("#c49760Modifiers"));
			List<String> lore = new ArrayList<String>();
			for(FactionModifier mod : f.getCombinedModifiers()) {
				if(mod.getAmount() != 0.0) lore.add(StringFormatter.formatHex("#d4c9ae- "+mod.getString()));
			}
			m.setLore(lore);
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
			m.getPersistentDataContainer().set(key, PersistentDataType.STRING, f.getId());
			i.setItemMeta(m);
		} else if(t.equals(MenuItemType.TAX)) {
			i = new ItemStack(Material.GOLD_INGOT, 1);
			ItemMeta m = i.getItemMeta();
			double foreignTax = f.getTotalForeignTaxRate();
			m.setDisplayName(StringFormatter.formatHex("#c77c32Tax Rate§e: #a39a84"+(f.getTaxRate()+foreignTax)+"%"));
			List<String> lore = new ArrayList<String>();
			lore.add(StringFormatter.formatHex("#d4c9aeDomestic Taxes§e: #a39a84"+f.getTaxRate()+"%"));
			if(foreignTax > 0) lore.add(StringFormatter.formatHex("#d4c9aeForeign Taxes§e: #a39a84"+foreignTax+"%"));
			if(RelationManager.getSubjects(f).size() > 0) {
				lore.add("");
				if(f.getMembers().contains(p.getName())) lore.add(StringFormatter.formatHex("#c49760§lWe Impose:"));
				else lore.add(StringFormatter.formatHex("#c49760§lThey Impose:"));
				lore.add(StringFormatter.formatHex("#6c93bdVassal Taxes§e: #a39a84"+f.getVassalTaxRate()+"% of their type"));
				lore.add(StringFormatter.formatHex("#ccac41§oInfo: #4c5250§oIf a vassal has 5% tax from their relation"));
				lore.add(StringFormatter.formatHex("#4c5250§oand the overlord has a 50% vassal tax rate"));
				lore.add(StringFormatter.formatHex("#4c5250§othe vassal has a 2.5% effective tax rate"));
			}
			if(f.getLeader().equalsIgnoreCase(p.getName())) {
				lore.add("");
				lore.add(StringFormatter.formatHex("§7Click to change #7a915eDomestic#d4c9ae/#6c93bdVassal §7tax rate"));
			}
			m.setLore(lore);
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
			m.getPersistentDataContainer().set(key, PersistentDataType.STRING, f.getId());
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
