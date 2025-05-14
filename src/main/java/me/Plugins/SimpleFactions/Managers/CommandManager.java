package me.Plugins.SimpleFactions.Managers;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.DenarEconomy.DenarEconomy;
import net.tfminecraft.DenarEconomy.Data.Account;
import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Diplomacy.RelationType;
import me.Plugins.SimpleFactions.Events.FactionCreateEvent;
import me.Plugins.SimpleFactions.Events.FactionDeleteEvent;
import me.Plugins.SimpleFactions.Loaders.RelationLoader;
import me.Plugins.SimpleFactions.Loaders.TitleLoader;
import me.Plugins.SimpleFactions.Objects.Bank;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.Modifier;
import me.Plugins.SimpleFactions.REST.RestServer;
import me.Plugins.SimpleFactions.Tiers.Title;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.Utils.Permissions;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class CommandManager implements Listener, CommandExecutor{
	public String cmd1 = "faction";
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(sender instanceof Player) {
			Player p = (Player) sender;
			if(cmd.getName().equalsIgnoreCase(cmd1) && args.length < 1) {
				p.sendMessage("§a[SimpleFactions]§c Error with command format, use the gameplay guide for a list of commands");
				return true;
			}
			if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("create") && args.length == 2) {
				if(FactionManager.getByMember(p.getName()) != null) {
					p.sendMessage("§cYou already have a faction!");
					return true;
				}
				Faction f = new Faction(args[1], p.getName());
				FactionCreateEvent factionCreateEvent = new FactionCreateEvent(p, f);
				Bukkit.getPluginManager().callEvent(factionCreateEvent);
				if(!factionCreateEvent.isCancelled()) {
					FactionManager.addFaction(f);
					p.sendMessage("§aFaction "+f.getName()+" §acreated!");
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("claim") && args.length == 1) {
				if(FactionManager.getByLeader(p.getName()) != null) {
					Faction f = FactionManager.getByMember(p.getName());
					int claim = RestServer.claim(p, f);
					if(claim == -2) {
						p.sendMessage("could not connect");
					} else {
						FactionManager.getMap().claim(p, f, claim);
					}
				} else {
					p.sendMessage("§cYou need to be a faction leader to claim land");
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("unclaim") && args.length == 1) {
				if(FactionManager.getByLeader(p.getName()) != null) {
					Faction f = FactionManager.getByMember(p.getName());
					int claim = RestServer.claim(p, f);
					if(claim == -2) {
						p.sendMessage("could not connect");
					} else {
						FactionManager.getMap().unclaim(p, f, claim);
					}
				} else {
					p.sendMessage("§cYou need to be a faction leader to unclaim land");
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("accept") && args.length == 1) {
				if(FactionManager.getByLeader(p.getName()) != null) {
					if(!RequestManager.hasRequest(p)) {
						p.sendMessage("§cYou have no requests to accept");
						return true;
					}
					RequestManager.accept(p);
				} else {
					p.sendMessage("§cYou need to be a faction leader to accept requests");
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("setcolour") && args.length == 2) {
			    if(FactionManager.getByLeader(p.getName()) != null) {
			        String rgb = args[1];
			        int result = FactionManager.validateRGB(rgb);

			        if(result == 0) {
			            Faction f = FactionManager.getByLeader(p.getName());
			            FactionManager.getMap().enqueue("nation", f.getRGB());
			            f.setRGB(rgb);
			            FactionManager.getMap().enqueue("nation", f.getRGB());
			            p.sendMessage("§aFaction colour updated to §f" + rgb);
			        } else if(result == 1) {
			            p.sendMessage("§cInvalid format. Use: R,G,B (e.g. 255,0,0)");
			        } else if(result == 2) {
			            p.sendMessage("§cRGB values must be numbers (e.g. 128,128,128)");
			        } else if(result == 3) {
			            p.sendMessage("§cEach RGB value must be between 0 and 255");
			        }

			    } else {
			        p.sendMessage("§cYou need to be a faction leader to change colour");
			    }
			    return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("delete") && args.length == 2) {
				Faction f = FactionManager.getByString(args[1]);
				if(f == null) {
					p.sendMessage("§a[SimpleFactions]§c Error! faction does not exist!");
					return true;
				}
				if(!Permissions.isAdmin(sender)) {
					if(!f.getMembers().contains(p.getName())) {
						p.sendMessage("§cCannot delete a faction you are not part of!");
						return true;
					}
					if(!p.getName().equalsIgnoreCase(f.getLeader())) {
						p.sendMessage("§cOnly the faction leader can delete the faction!");
						return true;
					}
					if(f.getBank() != null && f.getBank().getWealth() > 0){
						p.sendMessage("§cCannot delete a faction while the bank balance is above 0");
						return true;
					}
				}
				FactionDeleteEvent factionDeleteEvent = new FactionDeleteEvent(p, f);
				Bukkit.getPluginManager().callEvent(factionDeleteEvent);
				if(!factionDeleteEvent.isCancelled()) {
					FactionManager.deleteFaction(f);
					if(f.getBank() != null) {
						BankManager.banks.remove(f.getBank());
					}
					for(Faction fac : FactionManager.factions) {
						if(fac.getRelations().containsKey(f.getId())) fac.getRelations().remove(f.getId());
					}
					p.sendMessage("§aFaction "+f.getName()+" §adeleted!");
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("list") && args.length == 1) {
				InventoryManager i = new InventoryManager();
				i.factionList(p);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("warlist") && args.length == 1) {
				InventoryManager i = new InventoryManager();
				i.warList(p);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("kick") && args.length == 2) {
				Faction f = FactionManager.getByMember(p.getName());
				if(f == null) {
					p.sendMessage("§cYou need to have a faction to kick someone");
					return true;
				}
				if(!p.getName().equalsIgnoreCase(f.getLeader())) {
					p.sendMessage("§cOnly the leader can kick players!");
					return true;
				}
				if(args[1].equalsIgnoreCase(f.getLeader())) {
					p.sendMessage("§cCant kick the leader!");
					return true;
				}
				if(!f.getMembers().contains(args[1])) {
					p.sendMessage("§cPlayer is not a member");
					return true;
				}
				f.removeMember(args[1]);;
				p.sendMessage("§aKicked "+args[1]);
				for(Player pl : Bukkit.getOnlinePlayers()) {
					if(pl.getName().equalsIgnoreCase(args[1])) {
						pl.sendMessage("§a"+p.getName()+ " kicked you from "+f.getName());
					}
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("setleader") && args.length == 2) {
				Faction f = FactionManager.getByMember(p.getName());
				if(f == null) {
					p.sendMessage("§cYou need to have a faction to change leader");
					return true;
				}
				if(!p.getName().equalsIgnoreCase(f.getLeader())) {
					p.sendMessage("§cOnly the leader can set a new leader!");
					return true;
				}
				if(!f.getMembers().contains(args[1])) {
					p.sendMessage("§cPlayer is not in the faction");
					return true;
				}
				if(args[1].equalsIgnoreCase(f.getLeader())) {
					p.sendMessage("§cPlayer is already the leader");
					return true;
				}
				f.setLeader(args[1]);
				for(Player pl : Bukkit.getOnlinePlayers()) {
					if(f.getMembers().contains(pl.getName())) {
						pl.sendMessage("§a"+args[1]+ " is the new faction leader!");
					}
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("invite") && args.length == 2) {
				Faction f = FactionManager.getByMember(p.getName());
				if(f == null) {
					p.sendMessage("§cYou need to have a faction to invite someone");
					return true;
				}
				if(!p.getName().equalsIgnoreCase(f.getLeader())) {
					p.sendMessage("§cOnly the leader can invite players!");
					return true;
				}
				if(f.getMembers().contains(args[1])) {
					p.sendMessage("§cPlayer is already a member");
					return true;
				}
				if(f.getMembers().size() == Cache.maxMembers) {
					p.sendMessage("§cFaction already has the maximum amount of members");
					return true;
				}
				f.getInvited().add(args[1]);
				p.sendMessage("§aInvited "+args[1]);
				for(Player pl : Bukkit.getOnlinePlayers()) {
					if(pl.getName().equalsIgnoreCase(args[1])) {
						pl.sendMessage("§a"+p.getName()+ " invited you to "+f.getName());
						pl.sendMessage("§aType /faction join "+f.getId() +"§a to join");
					}
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("join") && args.length == 2) {
				if(FactionManager.getByMember(p.getName()) != null) {
					p.sendMessage("§cAlready in a faction, leave your current faction first!");
					return true;
				}
				Faction f = FactionManager.getByString(args[1]);
				if(!f.getInvited().contains(p.getName())) {
					p.sendMessage("§cYou need to be invited to this faction by the leader first!");
					return true;
				}
				f.getInvited().remove(p.getName());
				f.addMember(p.getName());
				p.sendMessage("§aJoined "+f.getName());
				f.updatePrestige();
				for(Player pl : Bukkit.getOnlinePlayers()) {
					if(f.getMembers().contains(pl.getName())) {
						pl.sendMessage("§a"+p.getName()+ " joined the faction!");
					}
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("leave") && args.length == 1) {
				if(FactionManager.getByMember(p.getName()) == null) {
					p.sendMessage("§cYou are not in a faction");
					return true;
				}
				if(FactionManager.getByLeader(p.getName()) != null) {
					p.sendMessage("§cCant leave if you are the leader");
					p.sendMessage("§cUse /faction delete first");
					return true;
				}
				Faction f = FactionManager.getByMember(p.getName());
				f.removeMember(p.getName());;
				p.sendMessage("§aLeft "+f.getName());
				f.updatePrestige();
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("rename") && args.length == 2) {
				Faction f = FactionManager.getByLeader(p.getName());
				if(f == null) {
					p.sendMessage("§a[SimpleFactions]§c Error! You must be the leader of a faction to rename one!");
					return true;
				}
				Formatter format = new Formatter();
				f.setName(StringFormatter.formatHex(format.formatName(args[1])));
				FactionManager.getMap().enqueue("nation", f.getRGB());
				p.sendMessage("§aFaction renamed to "+f.getName());
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("setrulertitle") && args.length == 2) {
				Faction f = FactionManager.getByLeader(p.getName());
				if(f == null) {
					p.sendMessage("§cYou must be the leader of a faction to change ruler title");
					return true;
				}
				String s = args[1].replace("_", " ");
				f.setRulerTitle(s);
				p.sendMessage("§aFaction ruler title changed to "+f.getRulerTitle());
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("setrulingsystem") && args.length == 2) {
				Faction f = FactionManager.getByLeader(p.getName());
				if(f == null) {
					p.sendMessage("§cYou must be the leader of a faction to change ruling system!");
					return true;
				}
				String s = args[1].replace("_", " ");
				f.setGovernment(s);
				p.sendMessage("§aFaction ruling system changed to "+f.getGovernment());
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("setculture") && args.length == 2) {
				Faction f = FactionManager.getByLeader(p.getName());
				if(f == null) {
					p.sendMessage("§cYou must be the leader of a faction to change culture!");
					return true;
				}
				String s = args[1].replace("_", " ");
				f.setCulture(s);
				p.sendMessage("§aFaction culture changed to "+f.getCulture());
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("setreligion") && args.length == 2) {
				Faction f = FactionManager.getByLeader(p.getName());
				if(f == null) {
					p.sendMessage("§cYou must be the leader of a faction to change religion!");
					return true;
				}
				String s = args[1].replace("_", " ");
				f.setReligion(s);
				p.sendMessage("§aFaction religion changed to "+f.getReligion());
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("setbanner") && args.length == 1) {
				Faction f = FactionManager.getByLeader(p.getName());
				if(f == null) {
					p.sendMessage("§cYou must be the leader of a faction to change the banner!");
					return true;
				}
				ItemStack i = new ItemStack(p.getInventory().getItemInMainHand());
				if(i == null || !i.getType().toString().contains("BANNER")) {
					p.sendMessage("§a[SimpleFactions]§c Error! You must be holding a banner in your main hand!");
					return true;
				}
				i.setAmount(1);
				f.setBanner(i);;
				p.sendMessage("§aFaction banner changed!");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("setbank") && args.length == 1) {
				if(FactionManager.getByLeader(p.getName()) != null) {
					Faction f = FactionManager.getByMember(p.getName());
					if(f.getBank() != null) {
						Bank bank = f.getBank();
						bank.setChunk(p.getLocation().getChunk());
						p.sendMessage("§aBank Chunk Moved");
					} else {
						f.setBank(new Bank(f, 0, p.getLocation().getChunk()));
						p.sendMessage("§aBank Chunk Set");
					}
					p.playSound(p, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1f);
				} else {
					p.sendMessage("§cYou need to be a faction leader to place the bank location");
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("deposit") && args.length == 2) {
				if(FactionManager.getByMember(p.getName()) != null) {
					Faction f = FactionManager.getByMember(p.getName());
					Bank b = f.getBank();
					if(b == null) {
						p.sendMessage("§cYour faction has no bank chunk");
						return false;
					}
					if(!p.getLocation().getChunk().equals(f.getBank().getChunk())) {
						p.sendMessage("§cYou need to be in the Bank Chunk to deposit money");
						return false;
					}
					double amount = Double.parseDouble(args[1]);
					Account pouch = DenarEconomy.getPlayerManager().get(p).getPouch();
					if(pouch.getBal() < amount) {
						p.sendMessage("§cNot enough funds");
						return false;
					}
					pouch.change(amount*-1);
					f.getBank().deposit(amount);
					p.sendMessage("§e============§6[Bank Report]§e==============");
					p.sendMessage(StringFormatter.formatHex("#6ab05aDeposited: #b39122"+amount+"#dbaf1dd"));
					p.sendMessage(StringFormatter.formatHex("#3ce8c9New Faction Balance: #b39122"+b.getWealth()+"#dbaf1dd"));
					p.sendMessage(StringFormatter.formatHex("#3ce8c9New Pouch Balance: #b39122"+pouch.getBal()+"#dbaf1dd"));
					p.sendMessage("§e=====================================");
					p.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
				} else {
					p.sendMessage("§cYou need to be a in a faction to deposit money into the faction bank");
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("withdraw") && args.length == 2) {
				if(FactionManager.getByLeader(p.getName()) != null) {
					Faction f = FactionManager.getByMember(p.getName());
					Bank b = f.getBank();
					if(b == null) {
						p.sendMessage("§cYour faction has no bank chunk");
						return false;
					}
					if(!p.getLocation().getChunk().equals(b.getChunk())) {
						p.sendMessage("§cYou need to be in the Bank Chunk to withdraw money");
						return false;
					}
					double amount = Double.parseDouble(args[1]);
					Account pouch = DenarEconomy.getPlayerManager().get(p).getPouch();
					if(b.getWealth() < amount) {
						p.sendMessage("§cNot enough funds in the faction bank");
						return false;
					}
					pouch.change(amount);
					f.getBank().withdraw(amount);
					p.sendMessage("§e============§6[Bank Report]§e==============");
					p.sendMessage(StringFormatter.formatHex("#6ab05aWithdrew: #b39122"+amount+"#dbaf1dd"));
					p.sendMessage(StringFormatter.formatHex("#3ce8c9New Faction Balance: #b39122"+b.getWealth()+"#dbaf1dd"));
					p.sendMessage(StringFormatter.formatHex("#3ce8c9New Pouch Balance: #b39122"+pouch.getBal()+"#dbaf1dd"));
					p.sendMessage("§e=====================================");
					p.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
				} else {
					p.sendMessage("§cYou need to be a faction leader to withdraw from the faction bank");
				}
				return true;
			}
			if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("addprestigemodifier") && args.length == 4) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[SimpleFactions]§c You do not have access to this command");
					return true;
				}
				Faction f = FactionManager.getByString(args[1]);
				if(f == null) {
					p.sendMessage("§a[SimpleFactions]§c Error! Faction not found!");
					return true;
				}
				String type = args[2];
				Double amount = Double.parseDouble(args[3]);
				Modifier m = new Modifier(type, amount);
				f.addPersistentPrestigeModifier(m);
				f.updatePrestige();
				p.sendMessage("§aFaction prestige changed!");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("addwealthmodifier") && args.length == 4) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[SimpleFactions]§c You do not have access to this command");
					return true;
				}
				Faction f = FactionManager.getByString(args[1]);
				if(f == null) {
					p.sendMessage("§a[SimpleFactions]§c Error! Faction not found!");
					return true;
				}
				String type = args[2];
				Double amount = Double.parseDouble(args[3]);
				Modifier m = new Modifier(type, amount);
				f.addPersistentWealthModifier(m);
				f.updateWealth();
				p.sendMessage("§aFaction wealth changed!");
				return true;
			} 
			/*else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("loadall") && args.length == 1) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[SimpleFactions]§c You do not have access to this command");
					return true;
				}
				Database db = new Database();
				db.loadFactions();
				p.sendMessage("§a[SimpleFactions] §eLoading factions...");
				return true;
			}
			*/
			else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("forcedelete") && args.length == 2) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[SimpleFactions]§c You do not have access to this command");
					return true;
				}
				Faction f = FactionManager.getByString(args[1]);
				if(f == null) {
					p.sendMessage("§a[SimpleFactions]§c Error! faction does not exist!");
					return true;
				}
				FactionDeleteEvent factionDeleteEvent = new FactionDeleteEvent(p, f);
				Bukkit.getPluginManager().callEvent(factionDeleteEvent);
				if(!factionDeleteEvent.isCancelled()) {
					FactionManager.deleteFaction(f);
					if(f.getBank() != null) {
						BankManager.banks.remove(f.getBank());
					}
					p.sendMessage("§aFaction "+f.getName()+" §adeleted!");
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("forceleader") && args.length == 3) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[SimpleFactions]§c You do not have access to this command");
					return true;
				}
				Faction f = FactionManager.getByString(args[1]);
				if(f == null) {
					p.sendMessage("§a[SimpleFactions]§c Error! faction does not exist!");
					return true;
				}
				if(!f.getMembers().contains(args[2])) {
					p.sendMessage("§cPlayer is not in the faction");
					return true;
				}
				if(args[2].equalsIgnoreCase(f.getLeader())) {
					p.sendMessage("§cPlayer is already the leader");
					return true;
				}
				f.setLeader(args[2]);
				for(Player pl : Bukkit.getOnlinePlayers()) {
					if(f.getMembers().contains(pl.getName())) {
						pl.sendMessage("§a"+args[2]+ " is the new faction leader!");
					}
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("refresh") && args.length == 1) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[SimpleFactions]§c You do not have access to this command");
					return true;
				}
				for(Faction f : FactionManager.factions) {
					if(!f.getMembers().contains(f.getLeader())) {
						f.getMembers().add(f.getLeader());
					}
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("delbank") && args.length == 2) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[SimpleFactions]§c You do not have access to this command");
					return true;
				}
				Faction f = FactionManager.getByString(args[1]);
				if(f == null) {
					p.sendMessage("§cFaction does not exist!");
					return true;
				}
				BankManager.banks.remove(f.getBank());
				f.setBank(null);
				p.sendMessage("§eBank removed");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("getglobalwealth") && args.length == 1) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[SimpleFactions]§c You do not have access to this command");
					return true;
				}
				p.sendMessage("§f======================================");
				p.sendMessage("§eGlobal Wealth: §6"+FactionManager.getGlobalWealth()+"d");
				p.sendMessage("§aTaken up by Nodes: §6"+FactionManager.getGlobalNodeWealth()+"d");
				p.sendMessage("§aLiquid Capital: §6"+FactionManager.getGlobalLiquidWealth()+"d");
				p.sendMessage("§aNode Percentage: §f"+Math.round((FactionManager.getGlobalNodeWealth()/FactionManager.getGlobalWealth())*100)+"% §aof global wealth");
				p.sendMessage("§f======================================");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("queueallnations") && args.length == 1) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[SimpleFactions]§c You do not have access to this command");
					return true;
				}
				FactionManager.getMap().queueAllNations();
				p.sendMessage("§eQueued all nations and asked for regen");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("fullregen") && args.length == 2) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[SimpleFactions]§c You do not have access to this command");
					return true;
				}
				if(!args[1].equalsIgnoreCase("i_love_tfmc")) {
					p.sendMessage("§a[SimpleFactions]§c Authentication Failed, incorrect passcode");
					return true;
				}
				FactionManager.getMap().fullRegen();
				p.sendMessage("§eFull regen started, this might take some time...");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("endwar") && args.length == 2) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[SimpleFactions]§c You do not have access to this command");
					return true;
				}
				Integer warId = 0;
				try {
					Integer.parseInt(args[1]);
				} catch (Exception e) {
					p.sendMessage("§cWar id is a number");
					return false;
				}
				War w = WarManager.getById(warId);
				if(w == null){
					p.sendMessage("§cNo war by that id");
					return false;
				}
				WarManager.endWar(w);
				p.sendMessage("§aEnded war "+w.getName());
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("destroytitle") && args.length == 2) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[SimpleFactions]§c You do not have access to this command");
					return true;
				}
				Title title = TitleLoader.getById(args[1]);
				if(title == null){
					p.sendMessage("§cNo title by that id");
					return false;
				}
				Faction owner = TitleManager.getOwner(title);
				if(owner == null){
					p.sendMessage("§cNo faction owns that title");
					return false;
				}
				owner.removeTitle(title);
				p.sendMessage("§aDestroyed title "+title.getName()+" §7("+title.getId()+")");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("granttitle") && args.length == 3) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[SimpleFactions]§c You do not have access to this command");
					return true;
				}
				Faction reciever = FactionManager.getByString(args[1]);
				if(reciever == null) {
					p.sendMessage("§cNo faction by that id");
					return false;
				}
				Title title = TitleLoader.getById(args[2]);
				if(title == null){
					p.sendMessage("§cNo title by that id");
					return false;
				}
				if(TitleManager.getOwner(title) != null){
					p.sendMessage("§cA faction already owns that title, use usurp instead!");
					return false;
				}
				reciever.addTitle(title);
				p.sendMessage("§aGave "+reciever.getName()+" §athe title "+title.getName()+" §7("+title.getId()+")");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("transfersubject") && args.length == 3) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[SimpleFactions]§c You do not have access to this command");
					return true;
				}
				Faction subject = FactionManager.getByString(args[1]);
				if(subject == null) {
					p.sendMessage("§cNo faction by the id "+args[1]);
					return false;
				}
				Faction recieving = FactionManager.getByString(args[2]);
				if(recieving == null) {
					p.sendMessage("§cNo faction by the id "+args[2]);
					return false;
				}
				String overlord = RelationManager.getOverlord(subject);
				if(overlord == null){
					p.sendMessage(subject.getName()+" §cis not a subject");
					return false;
				}
				if(recieving.getId().equalsIgnoreCase(overlord)){
					p.sendMessage(subject.getName()+" §cis already a subject of "+recieving.getName());
					return false;
				}
				if(RelationManager.isOnOverlordPath(recieving, subject)) {
					p.sendMessage("§cThis transfer would cause a loop");
					return false;
				}
				RelationManager.transferSubject(subject, recieving);
				p.sendMessage("§aTransfered subject");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("setrelation") && args.length == 4) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[SimpleFactions]§c You do not have access to this command");
					return true;
				}
				Faction sending = FactionManager.getByString(args[1]);
				if(sending == null) {
					p.sendMessage("§cNo faction by the id "+args[1]);
					return false;
				}
				Faction recieving = FactionManager.getByString(args[2]);
				if(recieving == null) {
					p.sendMessage("§cNo faction by the id "+args[2]);
					return false;
				}
				RelationType type = RelationLoader.getType(args[3]);
				if(type == null) {
					p.sendMessage("§cNo relation with the id "+args[3]);
					return false;
				}
				RelationManager.setRelation(p, type, recieving, sending, false);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("usurp") && args.length == 3) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[SimpleFactions]§c You do not have access to this command");
					return true;
				}
				Faction usurping = FactionManager.getByString(args[1]);
				if(usurping == null) {
					p.sendMessage("§cNo faction by the id "+args[1]);
					return false;
				}
				Faction losing = FactionManager.getByString(args[2]);
				if(losing == null) {
					p.sendMessage("§cNo faction by the id "+args[2]);
					return false;
				}
				Title t = FactionManager.usurp(p, usurping, losing);
				if(t != null) p.sendMessage(usurping.getName()+" §ausurped "+t.getName());;
				return true;
			}
			p.sendMessage("§a[SimpleFactions]§c Error with command format, use the gameplay guide for a list of commands");
		}
		return false;
	}
}
