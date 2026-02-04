package me.Plugins.SimpleFactions.Managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import me.Plugins.SimpleFactions.Database.Database;
import me.Plugins.SimpleFactions.Diplomacy.Attitude;
import me.Plugins.SimpleFactions.Diplomacy.Relation;
import me.Plugins.SimpleFactions.Diplomacy.RelationType;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Loaders.RelationLoader;
import me.Plugins.SimpleFactions.Loaders.TitleLoader;
import me.Plugins.SimpleFactions.Map.MapSystem;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.Modifier;
import me.Plugins.SimpleFactions.Objects.PrestigeRank;
import me.Plugins.SimpleFactions.Objects.Request.RelationRequest;
import me.Plugins.SimpleFactions.Objects.Request.RelocateRequest;
import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Tiers.Title;
import me.Plugins.SimpleFactions.Utils.DailyGuildTransfers;
import me.Plugins.SimpleFactions.Utils.FactionCleanup;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.government.Government;
import me.Plugins.TLibs.TLibs;
import net.tfminecraft.DenarEconomy.DenarEconomy;
import net.tfminecraft.DenarEconomy.Enum.Accounts;

public class FactionManager implements Listener{
	public static int timer = 0;

	public static List<Faction> factions = new ArrayList<Faction>();
	
	public static HashMap<Faction, List<String>> dbRelations = new HashMap<>();

	public static int getTimer(){
		return timer;
	}
	
	public static void addDBRelation(Faction f, String s) {
		List<String> list = new ArrayList<>();
		if(dbRelations.containsKey(f)) {
			list = dbRelations.get(f);
		}
		list.add(s);
		dbRelations.put(f, list);
	}
	
	public static void loadRelations() {
		for(Map.Entry<Faction, List<String>> entry : dbRelations.entrySet()) {
			Faction f = entry.getKey();
			List<String> relations = entry.getValue();
			for(String s : relations) {
				Faction target = getByString(s.split("\\(")[0]);
				if(target == null) continue;
				String info = s.split("\\(")[1].replace(")", "");
				RelationType r = RelationLoader.getType(info.split("\\.")[0]);
				Attitude a = RelationLoader.getAttitude(info.split("\\.")[1]);
				if(r == null || a == null) continue;
				int opinion = Integer.parseInt(info.split("\\.")[2]);
				f.setRelation(target, new Relation(r, a, opinion));
				if(target.getRelation(f.getId()).isDefault() && r.isVassalage()) {
					target.setRelation(f, new Relation(r.getLink(), RelationLoader.getDefaultAttitude(), 0));
				}
			}
		}
		dbRelations.clear();
	}
	
	public static void reloadTitles() {
		for(Faction f : factions) {
			List<Title> newTitles = new ArrayList<>();
			for(Title t : f.getTitles()) {
				Title title = TitleLoader.getById(t.getId());
				if(title == null) continue;
				newTitles.add(title);
			}
			f.resetTitles(newTitles);
		}
	}

	public void fixRelations() {
		//Allies
		for(Faction f : factions) {
			List<Faction> allies = RelationManager.getAllies(f);
			for(Faction ally : allies) {
				if(!ally.getRelation(f.getId()).getType().getId().equalsIgnoreCase("ally")) {
					Relation r = ally.getRelation(f.getId());
					r.setType(RelationLoader.getType("ally"));
					ally.setRelation(f, r);
				}
			}
		}
	}
	
	public static Double globalWealth = 0.0;
	
	public static MapSystem map = new MapSystem();
	
	public static InventoryManager inv = new InventoryManager();
	
	public static InventoryManager getInv() {
		return inv;
	}
	
	public static MapSystem getMap() {
		return map;
	}
	
	public static Faction getTitleOwner(Title t) {
		for(Faction f : factions) {
			if(f.hasTitle(t)) return f;
		}
		return null;
	}
	
	public static Faction getByProvince(int i) {
		for(Faction f : factions) {
			if(f.getProvinces().contains(i)) return f;
		}
		return null;
	}
	
	private void tickCycle() {
		new BukkitRunnable() {
			@Override
	        public void run() {
				for(Faction f : factions) {
					f.tick();
				}
				map.tick();
				RelationManager.tick();
				inv.getUpdater().updateInventory();
				time();
	        }
	    }.runTaskTimer(SimpleFactions.plugin, 0L, 20L);
	}

	public void time() {
		timer++;
		if(timer%10 == 0) {
			for(Faction f : factions) {
				f.getGovernment().powerTick();
			}
		}
		if(timer%300 == 0) {
			for(Faction f : factions) {
				if(f.getProvinces().size() == 0) continue;
				Player leader = Bukkit.getPlayerExact(f.getLeader());
				if(leader == null) continue;
				if(TitleManager.overProvinceCap(f)) {
					leader.sendMessage("§cYou are over your province cap! Other nations can steal your provinces from you!");
					leader.sendMessage("§cGet more prestige or unclaim provinces to counteract this!");
				}
			}
		}
		if (timer >= 86400) {
			for(Faction f : factions){
				f.newDay();
			}
			FactionCleanup.kickInactiveMembers(factions);
			settleIncome();
			timer = 0;
		}
	}

	public void settleIncome() {
		DailyGuildTransfers buffer = new DailyGuildTransfers();

		// Phase 1: collect transfers & external deltas
		for (Guild g : getAllGuilds()) {
			g.getLedger().populateDailyTransfers(buffer);
		}

		// Phase 2: compute net deltas
		Map<Guild, Double> deltas = new HashMap<>();

		// Guild -> Guild transfers
		for (var fromEntry : buffer.getTransfers().entrySet()) {
			Guild from = fromEntry.getKey();
			for (var toEntry : fromEntry.getValue().entrySet()) {
				Guild to = toEntry.getKey();
				double amount = toEntry.getValue();

				deltas.merge(from, -amount, Double::sum);
				deltas.merge(to, amount, Double::sum);
			}
		}

		// External deposits / withdrawals
		for (var entry : buffer.getExternalDeltas().entrySet()) {
			Guild guild = entry.getKey();
			double delta = entry.getValue();

			deltas.merge(guild, delta, Double::sum);
		}

		// Phase 3: apply atomically
		for (var entry : deltas.entrySet()) {
			double amount = Formatter.formatDouble(entry.getValue());
			if (amount == 0.0) continue;
			if(entry.getKey().getBank() == null) continue;
			entry.getKey().getBank().deposit(amount);
		}

		buffer.clear();
	}

	
	public void run() {
		timer = (new Database()).getTimer();	
		loadRelations();
		tickCycle();	
		for(Faction f : factions) {
			f.updatePrestige();
			f.countyCheck();
			f.ping();
		}
		fixRelations();
	}

	public static boolean guildExists(String id) {
		for(Faction f : factions) {
			for(Guild guild : f.getGuildHandler().getGuilds()) {
				if(guild.getId().equalsIgnoreCase(id)) return true;
			}
		}
		return false;
	}

	public static boolean canJoinGuild(Player p) {
		Guild guild = getGuildByMember(p.getName());
		if(guild == null) return true;
		if(guild.getLeader().equalsIgnoreCase(p.getName())) return false;
		return guild.getType().isBase();
	}

	public static Guild getGuildByMember(String player) {
		for(Faction f : factions) {
			for(Guild guild : f.getGuildHandler().getGuilds()) {
				if(guild.isMember(player)) return guild;
			}
		}
		return null;
	}

	public static Guild getGuildByLeader(String leader) {
		for(Faction f : factions) {
			for(Guild guild : f.getGuildHandler().getGuilds()) {
				if(guild.isLeader(leader)) return guild;
			}
		}
		return null;
	}

	public static Guild getGuildByString(String id) {
		for(Faction f : factions) {
			for(Guild guild : f.getGuildHandler().getGuilds()) {
				if(guild.getId().equalsIgnoreCase(id)) return guild;
			}
		}
		return null;
	}

	public static List<Guild> getAllGuilds() {
		List<Guild> guilds = new ArrayList<>();
		for(Faction f : factions) {
			guilds.addAll(f.getGuildHandler().getGuilds());
		}
		return guilds;
	}
	
	public void start(List<Faction> l) {
		factions = l;
	}
	public static void addFaction(Faction f) {
		factions.add(f);
	}
	public static void deleteFaction(Faction f){
		map.enqueue("nation", f.getRGB());
		factions.remove(f);
		Database db = new Database();
		db.deleteFaction(f);
	}
	public static void updateAllPrestige() {
		for(Faction f : factions) {
			f.updatePrestige();
		}
	}
	public static Double getRankUpAmount(PrestigeRank rank) {
		List<Faction> ranked = new ArrayList<Faction>();
		for(Faction f : factions) {
			if(f.getRank().getId().equalsIgnoreCase(rank.getId())) {
				ranked.add(f);
			}
		}
		if(ranked.size() < 1) {
			return rank.getMin();
		}
		Collections.sort(ranked, new Comparator<Faction>() {
		    @Override
		    public int compare(Faction c1, Faction c2) {
		        return Double.compare(c1.getPrestige(), c2.getPrestige());
		    }
		});
		Collections.reverse(ranked);
		Double amount = ranked.get(0).getPrestige()*(rank.getPercentage()/100);
		if(amount > rank.getMin()) {
			return amount;
		}
		return rank.getMin();
	}
	public static double getPouchWealth() {
		Formatter format = new Formatter();
		return format.formatDouble(DenarEconomy.getMoneyManager().getServerBal(Accounts.POUCH));
	}
	public static double getBankWealth() {
		Formatter format = new Formatter();
		return format.formatDouble(DenarEconomy.getMoneyManager().getServerBal(Accounts.BANK));
	}
	public static Double getGlobalWealth() {
		Formatter format = new Formatter();
		Double amount = 0.0;
		for(Faction f : factions) {
			amount = amount + f.getWealth();
		}
		return format.formatDouble(amount);
	}
	public static Double getGlobalLiquidWealth() {
		Formatter format = new Formatter();
		Double amount = 0.0;
		for(Faction f : factions) {
			for(Modifier m : f.getWealthModifiers()) {
				if(m.getType().equalsIgnoreCase("bank")) {
					amount = amount + m.getAmount();
				}
			}
		}
		return format.formatDouble(amount);
	}
	public static Double getTotalGuildIncome() {
		Formatter format = new Formatter();
		Double amount = 0.0;
		for(Guild g : getAllGuilds()) {
			amount+=g.getLedger().getInflationDelta();
		}
		return format.formatDouble(amount);
	}
	public static Double getGuildLiquidWealth() {
		Formatter format = new Formatter();
		Double amount = 0.0;
		for(Guild g : getAllGuilds()) {
			if(g.isBase()) continue;
			for(Modifier m : g.getWealthModifiers()) {
				if(m.getType().equalsIgnoreCase("bank")) {
					amount = amount + m.getAmount();
				}
			}
		}
		return format.formatDouble(amount);
	}
	public static Double getGlobalGuildExpansions() {
		Formatter format = new Formatter();
		Double amount = 0.0;
		for(Faction f : factions) {
			for(Guild guild : f.getGuildHandler().getGuilds()) {
				amount+=guild.getTotalExpansionSpent();
			}
		}
		return format.formatDouble(amount);
	}
	public static Double getGlobalNodeWealth() {
		Formatter format = new Formatter();
		Double amount = 0.0;
		for(Faction f : factions) {
			for(Modifier m : f.getWealthModifiers()) {
				if(m.getType().equalsIgnoreCase("nodes")) {
					amount = amount + m.getAmount();
				}
			}
		}
		return format.formatDouble(amount);
	}
	public static Faction getByString(String s) {
		for(Faction f : factions) {
			if(f.getId().equalsIgnoreCase(s)) return f;
		}
		return null;
	}
	public static List<Faction> getCopy(){
		List<Faction> c = new ArrayList<Faction>();
		for(Faction f : factions) {
			c.add(f);
		}
		return c;
	}
	public static Faction getByLeader(String name) {
		for(Faction f : factions) {
			if(f.getLeader().equalsIgnoreCase(name)) return f;
		}
		return null;
	}
	public static Faction getByMember(String name) {
		for(Faction f : factions) {
			if(f.getMembers().contains(name)) return f;
			if(f.getLeader().equalsIgnoreCase(name)) {
				f.addMember(name);
				return f;
			}
		}
		return null;
	}
	
	public static Faction getByRGB(String rgb) {
		for(Faction f : factions) {
			if(f.getRGB().equalsIgnoreCase(rgb)) return f;
		}
		return null;
	}
	
	/**
	 * Validates an RGB string in the format "R,G,B"
	 * 
	 * @param rgb The input RGB string
	 * @return 0 if valid, or error code:
	 *         1 - Incorrect number of components
	 *         2 - Component is not a number
	 *         3 - Component is out of range (not between 0-255)
	 */
	public static int validateRGB(String rgb) {
	    if (rgb == null) return 1;

	    String[] parts = rgb.trim().split(",");
	    if (parts.length != 3) return 1;

	    try {
	        for (String part : parts) {
	            int value = Integer.parseInt(part.trim());
	            if (value < 0 || value > 255) return 3;
	        }
	        return 0; // All good
	    } catch (NumberFormatException e) {
	        return 2;
	    }
	}

    public static Title usurp(Player p, Faction usurping, Faction losing) {
        Title t = losing.getHighestTitle();
		if(t == null){
			if(p != null) p.sendMessage("§ctarget has no titles");
			return null;
		}
		usurping.addTitle(t);
		for(Faction subject : RelationManager.getSubjects(losing)) {
			if(subject.getId().equalsIgnoreCase(usurping.getId())) continue;
			RelationManager.transferSubject(subject, usurping);
		}
		String o = RelationManager.getOverlord(losing);
		if(o != null){
			Faction overlord = FactionManager.getByString(o);
			RelationManager.endVassalage(overlord, losing, false);
			RelationManager.transferSubject(usurping, overlord);
		}
		RelationManager.endVassalage(usurping, losing, true);
		RelationManager.setRelation(p, RelationLoader.getType("subject"), losing, usurping, false);
		losing.removeTitle(t);
		return t;
    }

	//Guild relocation

	public static void requestRelocation(Player sender, Guild g, Faction target, int capital) {
		Player p = Bukkit.getPlayerExact(target.getLeader());
		if(p == null) {
			sender.sendMessage("§cTarget faction leader is not online");
			return;
		}
		p.sendMessage(g.getName()+" §7is requesting to relocate to your faction");
		p.sendMessage("§7Type §a/faction accept §7to accept");
		p.sendMessage("§7Request will time out in 60 seconds");
		sender.sendMessage("§aRelocation request sent to "+target.getName());
		RequestManager.addRequest(sender, p, new RelocateRequest(g, capital));
	}

	public static void acceptRequest(Player p) {
		RelocateRequest req = (RelocateRequest) RequestManager.getRequest(p);
		Faction reciever = FactionManager.getByLeader(p.getName());
		if(reciever == null) {
			p.sendMessage("§cYou do not have a faction");
			return;
		}
		Guild sender = req.getSender();
		double cost = sender.getRelocationCost(req.getNewCapital());
		Player sp = Bukkit.getPlayerExact(sender.getLeader());
		if(sender.getBank().getWealth() < cost) {
			p.sendMessage("§c"+sender.getName()+" does not have enough funds to relocate (Cost: "+Formatter.formatDouble(cost)+"d)");
			if(sp != null && sp.isOnline()) sp.sendMessage("§cYour guild does not have enough funds to relocate (Cost: "+Formatter.formatDouble(cost)+"d)");
			return;
		}
		sender.getBank().withdraw(cost);
		if(sp != null && sp.isOnline()) sp.sendMessage(reciever.getName()+" §aaccepted your request to relocate to their faction");
		sender.relocate(reciever, req.getNewCapital());
		p.sendMessage(sender.getName()+"§a has been relocated to your faction");
	}

	//Elections and stuff

	public Faction getByVotingBooth(Block b) {
		for(Faction f : factions) {
			Government gov = f.getGovernment();
			if(gov.isVotingBooth(b.getLocation())) return f;
		}
		return null;
	}

	@EventHandler
	public void openBooth(PlayerInteractEvent e) {
		if(!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
		Block b = e.getClickedBlock();
		Player p = e.getPlayer();
		Faction f = getByVotingBooth(b);
		if(f == null) return;
		e.setCancelled(true);
		if(!f.canVote(p)) {
			p.sendMessage("§cYou have no voting rights in this faction");
			p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
			return;
		}
		InventoryManager inv = new InventoryManager();
		inv.electionView(p, f);
	}

	@EventHandler
	public void placeVotingBooth(BlockPlaceEvent e) {
		Block b = e.getBlock();
		Player p = e.getPlayer();
		if(!TLibs.getBlockAPI().getChecker().checkBlock(b, Cache.votingBlock)) return;
		Faction f = FactionManager.getByMember(p.getName());
		if(f == null) return;
		Government gov = f.getGovernment();
		if(gov.isVotingBooth(b.getLocation())) return;
		gov.addVotingBooth(b.getLocation());
		p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
		p.sendMessage("§aVoting booth added!");
	}

	@EventHandler
	public void breakVotingBooth(BlockBreakEvent e) {
		Block b = e.getBlock();
		Player p = e.getPlayer();
		if(!TLibs.getBlockAPI().getChecker().checkBlock(b, Cache.votingBlock)) return;
		Faction f = getByVotingBooth(b);
		if(f == null) return;
		Government gov = f.getGovernment();
		if(!gov.isVotingBooth(b.getLocation())) return;
		gov.removeVotingBooth(b.getLocation());
		p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
		p.sendMessage("§cVoting booth removed!");
	}
}
