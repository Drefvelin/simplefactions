package me.Plugins.SimpleFactions.Managers.Inventory;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Diplomacy.Attitude;
import me.Plugins.SimpleFactions.Diplomacy.Relation;
import me.Plugins.SimpleFactions.Diplomacy.RelationType;
import me.Plugins.SimpleFactions.Loaders.RelationLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Tiers.Tier;
import me.Plugins.SimpleFactions.enums.SFGUI;

public class RelationView {
	public InventoryManager inv;
	
	public RelationCreator creator = new RelationCreator();
	
	public RelationView(InventoryManager inv) {
		this.inv = inv;
	}
	
	public void diplomacyView(Inventory i, Player player, Faction f, boolean open) {
		if(open) {
			i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.DIPLOMACY_VIEW), 54, "§7Diplomacy View");
		}
		if(!f.getMembers().contains(player.getName()) && FactionManager.getByMember(player.getName()) != null) {
			Faction pf = FactionManager.getByMember(player.getName());
			Relation r = pf.getRelation(f.getId());
			i.setItem(21, creator.createRelationItem(f, pf));
			RelationType trade = pf.getDiplomacyHandler().getTradeRelation(f.getId());
			if(trade != null) {
				i.setItem(20, creator.createTradeAgreementTypeItem(player, trade, f, pf, false));
			} else {
				i.setItem(20, creator.createNoTradeAgreementItem());
			}
			i.setItem(12, creator.createRelationTypeItem(r.getType(), f, pf, false));
			i.setItem(24, creator.createWarButton(f, pf));
			i.setItem(30, creator.createAttitudeItem(r.getAttitude()));
			
		}
		i.setItem(53, inv.createBackButton(SFGUI.DIPLOMACY_VIEW));
		if(open) player.openInventory(i);
	}
	public void attitudeView(Inventory i, Player player, Faction f, boolean open) {
		if(open) {
			i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.ATTITUDE_VIEW), 27, "§7Change Attitude");
		}
		for(int x = 0; x<RelationLoader.getAttitudes().size(); x++) {
			int slot = x+10;
			i.setItem(slot, creator.createAttitudeItem(RelationLoader.getAttitudes().get(x)));
		}
		i.setItem(26, inv.createBackButton(SFGUI.ATTITUDE_VIEW));
		if(open) player.openInventory(i);
	}
	public void relationView(Inventory i, Player player, Faction f, boolean open) {
		if(open) {
			i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.RELATION_VIEW), 27, "§7Change Relation");
		}
		int slot = 9;
		for(int x = 0; x<RelationLoader.getTypes().size(); x++) {
			RelationType t = RelationLoader.getTypes().get(x);
			if(!t.isSettable()) continue;
			if(t.isTradeAgreement()) continue;
			i.setItem(slot, creator.createRelationTypeItem(RelationLoader.getTypes().get(x), f, FactionManager.getByMember(player.getName()), true));
			slot++;
		}
		i.setItem(26, inv.createBackButton(SFGUI.RELATION_VIEW));
		if(open) player.openInventory(i);
	}

	public void tradeAgreementView(Inventory i, Player player, Faction f, boolean open) {
		if(open) {
			i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.TRADE_AGREEMENT_VIEW), 27, "§7Change Trade Agreement");
		}
		int slot = 9;
		for(int x = 0; x<RelationLoader.getTypes().size(); x++) {
			RelationType t = RelationLoader.getTypes().get(x);
			if(!t.isSettable()) continue;
			if(!t.isTradeAgreement()) continue;
			i.setItem(slot, creator.createTradeAgreementTypeItem(player, RelationLoader.getTypes().get(x), f, FactionManager.getByMember(player.getName()), true));
			slot++;
		}
		i.setItem(26, inv.createBackButton(SFGUI.TRADE_AGREEMENT_VIEW));
		if(open) player.openInventory(i);
	}
	
	public void click(InventoryClickEvent e, Inventory inventory, Player p) {
		ItemStack item = e.getCurrentItem();
        if (item == null) return;
        
        SFInventoryHolder holder = (SFInventoryHolder) inventory.getHolder();
        if (holder == null) return;
        
        e.setCancelled(true);
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        SFGUI gui = holder.getType();
		if(e.getView().getTitle().equalsIgnoreCase("§7Diplomacy View")) {
			e.setCancelled(true);
			if(!(inventory.getHolder() instanceof SFInventoryHolder)) return;
			SFInventoryHolder h = (SFInventoryHolder) inventory.getHolder();
			Faction f = FactionManager.getByString(h.getId());
			if(!h.getType().equals(SFGUI.DIPLOMACY_VIEW)) return;
			if(e.getSlot() == 30) {
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				attitudeView(null, p, f, true);
			} else if(e.getSlot() == 12) {
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				relationView(null, p, f, true);
			} else if(e.getSlot() == 20) {
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				tradeAgreementView(null, p, f, true);
			} else if(e.getSlot() == 24) {
				Faction attacker = FactionManager.getByLeader(p.getName());
				if (attacker == null) return;
				if (WarManager.exists(attacker, f)) {
					p.sendMessage("§cYour faction is already in a war with that faction.");
					return;
				}
				// DEBUG: other declare pre-checks disabled for war testing — re-enable before production
				/*
				if (Cache.warRequireDeclareCode) {
					p.sendMessage("§cWar declaration requires a staff code (not enabled yet).");
					return;
				}
				if(attacker.getRelation(f.getId()).getOpinion() > Cache.warDeclareOpinionThreshold) {
					p.sendMessage("§cYour opinion of the target is too high");
					return;
				}
				if(f.numOnline() < 1){
					p.sendMessage("§cCannot declare war while none of the target faction members are online!");
					return;
				}
				*/
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				inv.openDeclareWarGoalPicker(p, attacker, f);
			}
			
		} else if(e.getView().getTitle().equalsIgnoreCase("§7Change Attitude")) {
			e.setCancelled(true);
			if(!(inventory.getHolder() instanceof SFInventoryHolder)) return;
			SFInventoryHolder h = (SFInventoryHolder) inventory.getHolder();
			Faction f = FactionManager.getByString(h.getId());
			if(!h.getType().equals(SFGUI.ATTITUDE_VIEW)) return;
			p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			Faction origin = FactionManager.getByMember(p.getName());
			
			Attitude a = RelationLoader.getAttitudes().get(e.getSlot()-10);
			
			RelationManager.setAttitude(p, a, f, origin);
			
			diplomacyView(null, p, f, true);
		} else if(e.getView().getTitle().equalsIgnoreCase("§7Change Relation")) {
			e.setCancelled(true);
			if(e.getCurrentItem().getType().equals(Material.BARRIER)) return;
			if(!(inventory.getHolder() instanceof SFInventoryHolder)) return;
			SFInventoryHolder h = (SFInventoryHolder) inventory.getHolder();
			Faction f = FactionManager.getByString(h.getId());
			if(!h.getType().equals(SFGUI.RELATION_VIEW)) return;
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
			String rid = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
			p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			Faction origin = FactionManager.getByMember(p.getName());
			
			RelationType r = RelationLoader.getType(rid);
			
			if(origin.getRelation(f.getId()).getType().hasLock()) {
				p.sendMessage("§cYou are not allowed to change your relationship with "+f.getName()+"§c!");
				return;
			}
			if(r.isVassalage()) {
				if(!origin.canHaveVassals()) {
					p.sendMessage("§cYour faction cannot have vassals!");
					return;
				}
				Tier ot = origin.getTier();
				Tier tt = f.getTier();
				if(ot.getTier() < tt.getTier()) {
					//p.sendMessage("§cYour nation has the tier "+ot.getFormattedName()+ " §cwhile "+f.getName()+ " §chas the tier "+tt.getFormattedName());
					p.sendMessage("§cYour tier must be equal to or higher than the target tier to vassalise them!");
					return;
				}
			}

			if(RelationManager.atLimit(origin, r)) {
				p.sendMessage("§cYou have reached the limit for this relation type");
				return;
			}

			double ourCost = RelationManager.getDiplomaticCost(origin, f, r);
			double theirCost = r.hasLink() ? RelationManager.getDiplomaticCost(f, origin, r.getLink()) : 0;
			if(origin.getDiplomacyHandler().getAvailableCapacity() < ourCost && !origin.getRelation(f.getId()).getType().equals(r) || f.getDiplomacyHandler().getAvailableCapacity() < theirCost && !f.getRelation(origin.getId()).getType().equals(r.getLink())) {
				if(origin.getDiplomacyHandler().getAvailableCapacity() < ourCost && !origin.getRelation(f.getId()).getType().equals(r)) 
					p.sendMessage("§cYou lack diplomatic capacity for this relation!");
				if(f.getDiplomacyHandler().getAvailableCapacity() < theirCost && !f.getRelation(origin.getId()).getType().equals(r.getLink())) 
					p.sendMessage("§cThey lack diplomatic capacity for this relation!");
				return;
			}
			
			RelationManager.setRelation(p, r, f, origin, true);
			
			diplomacyView(null, p, f, true);
		} else if(gui.equals(SFGUI.TRADE_AGREEMENT_VIEW)) {
			e.setCancelled(true);
			if(e.getCurrentItem().getType().equals(Material.BARRIER)) return;
			Faction f = FactionManager.getByString(holder.getId());
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
			String rid = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
			p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			Faction origin = FactionManager.getByMember(p.getName());
			
			RelationType r = RelationLoader.getType(rid);
			RelationType current = origin.getDiplomacyHandler().getTradeRelation(f.getId());
			RelationType theirCurrent = f.getDiplomacyHandler().getTradeRelation(origin.getId());
			if(current != null && current.hasLock()) {
				p.sendMessage("§cYou are not allowed to change your relationship with "+f.getName()+"§c!");
				return;
			}

			double ourCost = RelationManager.getDiplomaticCost(origin, f, r);
			double theirCost = r.hasLink() ? RelationManager.getDiplomaticCost(f, origin, r.getLink()) : 0;
			if(origin.getDiplomacyHandler().getAvailableCapacity() < ourCost && (current == null || !current.equals(r)) || f.getDiplomacyHandler().getAvailableCapacity() < theirCost && (theirCurrent == null || !theirCurrent.equals(r.getLink()))) {
				if(origin.getDiplomacyHandler().getAvailableCapacity() < ourCost && (current == null || !current.equals(r))) 
					p.sendMessage("§cYou lack diplomatic capacity for this relation!");
				if(f.getDiplomacyHandler().getAvailableCapacity() < theirCost && (theirCurrent == null || !theirCurrent.equals(r.getLink()))) 
					p.sendMessage("§cThey lack diplomatic capacity for this relation!");
				return;
			}
			
			RelationManager.setTradeRelation(p, r, f, origin, true);
			
			diplomacyView(null, p, f, true);
		}
	}
}
