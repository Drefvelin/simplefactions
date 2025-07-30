package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Diplomacy.Attitude;
import me.Plugins.SimpleFactions.Diplomacy.Relation;
import me.Plugins.SimpleFactions.Diplomacy.RelationType;
import me.Plugins.SimpleFactions.Diplomacy.Threshold;
import me.Plugins.SimpleFactions.Loaders.RelationLoader;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.FactionModifier;
import me.Plugins.SimpleFactions.Utils.OpinionColourMapper;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class RelationCreator {
	public void addThreshold(List<String> lore, Threshold h) {
		lore.add(" ");
		String plus = "";
		if(h.getOpinion() > 0) plus = "+";
		if(h.isMutual()) {
			lore.add(StringFormatter.formatHex("#a39ba8Requires opinion "+h.getFormattedShort()+OpinionColourMapper.getOpinionColor(h.getOpinion())+" "+plus+h.getOpinion()+"#a39ba8 (mutual)"));
		} else {
			lore.add(StringFormatter.formatHex("#a39ba8Requires opinion "+h.getFormattedShort()+OpinionColourMapper.getOpinionColor(h.getOpinion())+" "+plus+h.getOpinion()));
		}
	}
	
	public ItemStack createWarButton(Faction target, Faction origin) {
		RelationType r = RelationLoader.getType("war");
		if(r == null) return new ItemStack(Material.AIR, 1);
		ItemStack i = IconGetter.getIcon("war");
		ItemMeta m = i.getItemMeta();
		m.setDisplayName(StringFormatter.formatHex("#d42300§lDeclare War"));
		List<String> lore = new ArrayList<>();
		if(r.hasThreshold()) {
			addThreshold(lore, r.getThreshold());
			lore.add("");
			lore.add("§4Only click this if you have");
			lore.add("§4an approved War Ticket in the discord!");
		}
		lore.add(" ");
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}
	
	public ItemStack createRelationItem(Faction target, Faction origin) {
		ItemStack i = target.getBanner();
		Relation r = origin.getRelation(target.getId());
		Relation ofR = target.getRelation(origin.getId());
		ItemMeta m = i.getItemMeta();
		m.setDisplayName(target.getName());
		List<String> lore = new ArrayList<String>();
		if(r.getType().isVisible()) lore.add(r.getType().getFull());
		lore.add(" ");
		if(r.getType().equals(ofR.getType())) {
			lore.add(StringFormatter.formatHex("#d4bb98§lRelation: "+r.getType().getName()+" #a39ba8(mutual)"));
		} else {
			lore.add(StringFormatter.formatHex("#d4bb98§lRelation: "+r.getType().getName()+" #a39ba8(outgoing)"));
			lore.add(StringFormatter.formatHex("#d4bb98§lRelation: "+ofR.getType().getName()+" #a39ba8(incoming)"));
		}
		lore.add(" ");
		lore.add(StringFormatter.formatHex("#a39ba8Our opinion of them: "+OpinionColourMapper.getOpinionColor(r.getOpinion())+r.getOpinion()));
		lore.add(StringFormatter.formatHex("#a39ba8Our attitude towards them: "+r.getAttitude().getName()));
		lore.add(" ");
		lore.add(StringFormatter.formatHex("#a39ba8Their opinion of us: "+OpinionColourMapper.getOpinionColor(ofR.getOpinion())+ofR.getOpinion()));
		lore.add(StringFormatter.formatHex("#a39ba8Their attitude towards us: "+ofR.getAttitude().getName()));
		lore.add(" ");
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}
	
	public ItemStack createAttitudeItem(Attitude a) {
		ItemStack i = new ItemStack(Material.EMERALD, 1);
		if(IconGetter.hasIcon(a.getId())) {
			i = IconGetter.getIcon(a.getId());
		}
		ItemMeta m = i.getItemMeta();
		m.setDisplayName(StringFormatter.formatHex("#d4bb98§lAttitude: "+a.getName()));
		List<String> lore = new ArrayList<String>();
		lore.add(" ");
		if(a.getTarget() > 0) {
			lore.add(StringFormatter.formatHex("#a89977This modifies the opinion target by: "+OpinionColourMapper.getOpinionColor(a.getTarget())+"+"+a.getTarget()));
		} else {
			lore.add(StringFormatter.formatHex("#a89977This modifies the opinion target by: "+OpinionColourMapper.getOpinionColor(a.getTarget())+a.getTarget()));
		}
		lore.add(" ");
		lore.add(StringFormatter.formatHex("#28ed70Click to change"));
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}
	
	public ItemStack createRelationTypeItem(RelationType t, Faction target, Faction origin, boolean full) {
		ItemStack i = new ItemStack(Material.PAPER, 1);
		if(IconGetter.hasIcon(t.getId())) {
			i = IconGetter.getIcon(t.getId());
		}
		ItemMeta m = i.getItemMeta();
		m.setDisplayName(StringFormatter.formatHex("#d4bb98§lRelation: "+t.getName()));
		List<String> lore = new ArrayList<String>();
		if(t.isVisible()) {
			lore.add(" ");
			if(t.getTarget() > 0) {
				lore.add(StringFormatter.formatHex("#a89977This modifies the opinion target by: "+OpinionColourMapper.getOpinionColor(t.getTarget())+"+"+t.getTarget()));
			} else {
				lore.add(StringFormatter.formatHex("#a89977This modifies the opinion target by: "+OpinionColourMapper.getOpinionColor(t.getTarget())+t.getTarget()));
			}
		}	
		if(full) {
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
			m.getPersistentDataContainer().set(key, PersistentDataType.STRING, t.getId());
			if(t.isMutual()) {
				lore.add(" ");
				lore.add(StringFormatter.formatHex("#8e50baRequires Mutual Agreement"));
				lore.add(StringFormatter.formatHex("#a39ba8(60s request)"));
			}
			if(t.isVassalage()) {
				lore.add(" ");
				lore.add(StringFormatter.formatHex("#d4bb98This is a "+t.getName()+"#d4bb98/"+t.getLink().getName()+" #d4bb98relationship"));
			}
			if(t.hasThreshold()) {
				addThreshold(lore, t.getThreshold());
			}
		}
		if(t.hasRecieveModifiers()) {
			lore.add(" ");
			lore.add(StringFormatter.formatHex("#a39ba8We recieve modifiers§e:"));
			for(FactionModifier mod : t.getRecieveModifiers()) {
				lore.add("§7- "+mod.getString());
			}
		}
		if(t.hasGiveModifiers()) {
			lore.add(" ");
			lore.add(StringFormatter.formatHex("#a39ba8They recieve modifiers§e:"));
			for(FactionModifier mod : t.getGiveModifiers()) {
				lore.add("§7- "+mod.getString());
			}
		}
		lore.add(" ");
		if(full) {
			RelationType current = origin.getRelation(target.getId()).getType();
			if(current.hasLock()) {
				lore.add(StringFormatter.formatHex("#d4bb98You have the relation "+current.getName()+" #d4bb98which you cannot change freely!"));
				lore.add(" ");
				lore.add(StringFormatter.formatHex("#ba3439Unavailable"));
			} else {
				RelationType linked = t.getLink() != null ? t.getLink() : RelationLoader.getDefaultType();
				current = target.getRelation(origin.getId()).getType();
				if(RelationManager.reverseChange(target, origin, t)) {
					lore.add(StringFormatter.formatHex("#ba3439Notice!"));
					lore.add(StringFormatter.formatHex("#d4bb98Since we have a "+target.getRelation(origin.getId()).getType().getName()+"#d4bb98/"+origin.getRelation(target.getId()).getType().getName()+" #d4bb98relationship"));
					RelationType link = RelationLoader.getDefaultType();
					if(t.isMutual() || t.hasLink()) {
						link = linked;
					}
					if(link.isDefault()) {
						lore.add(StringFormatter.formatHex("#d4bb98Changing would reset their relationship with us to "+link.getName()));
					} else {
						lore.add(StringFormatter.formatHex("#d4bb98Changing would set their relationship with us to "+link.getName()));
					}	
					lore.add(" ");
				}
				if(origin.getRelation(target.getId()).getType().equals(t)) {
					lore.add(StringFormatter.formatHex("#28ed70Current"));
					m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
					m.addEnchant(Enchantment.DURABILITY, 1, true);
				} else if(t.isMutual()) {
					lore.add(StringFormatter.formatHex("#28ed70Click to request"));
				} else {
					lore.add(StringFormatter.formatHex("#28ed70Click to change"));
				}
			}
		} else {
			lore.add(StringFormatter.formatHex("#28ed70Click for more information"));
		}
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}
}
