package me.Plugins.SimpleFactions.Utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Diplomacy.RelationType;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Loaders.RelationLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.Tiers.Title;
import me.Plugins.SimpleFactions.installation.Installation;
import me.Plugins.SimpleFactions.installation.InstallationKind;
import me.Plugins.TLibs.Utils.TabCleaner;

public class TabCompletion implements TabCompleter{

	private boolean eitherCommand(Command cmd) {
		if(cmd.getName().equalsIgnoreCase("faction") || cmd.getName().equalsIgnoreCase("guild")) {
			return true;
		}
		return false;
	}

	private List<String> completeConstructKinds(String[] args) {
		List<String> completions = new ArrayList<>();
		for (InstallationKind kind : InstallationKind.values()) {
			completions.add(kind.getCommandName());
		}
		if (args.length >= 2) {
			String prefix = args[1].toLowerCase();
			completions.removeIf(kind -> !kind.toLowerCase().startsWith(prefix));
		}
		return completions;
	}

	private List<String> completeDeconstructIds(Player p, String[] args) {
		List<String> completions = new ArrayList<>();
		Faction f = FactionManager.getByLeader(p.getName());
		if (f == null) {
			return completions;
		}
		for (Installation installation : f.getInstallationHandler().getAll()) {
			completions.add(installation.getId());
		}
		if (f.getInstallationHandler().getPendingConstruction() != null) {
			completions.add(f.getInstallationHandler().getPendingConstruction().getId());
		}
		if (args.length >= 2) {
			String prefix = args[1].toLowerCase();
			completions.removeIf(id -> !id.toLowerCase().startsWith(prefix));
		}
		return completions;
	}

    @Override
    public List<String> onTabComplete (CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("guild") && args.length >= 0 && args.length < 2 ) {
			if(sender instanceof Player){
				Player p = (Player) sender;
				List<String> completions = new ArrayList<>();
				if(FactionManager.getGuildByMember(p.getName()) != null) completions.add("menu");
				completions.add("create");
				completions.add("join");
				completions.add("menu");
				completions.add("setbank");
				completions.add("deposit");
				completions.add("withdraw");
				completions.add("setcapital");
				if(FactionManager.getGuildByLeader(p.getName()) != null) {
					completions.add("invite");
					completions.add("setleader");
					completions.add("rename");
					completions.add("setbanner");
				}
				if(Permissions.isAdmin(sender)) {
					completions.add("dummify");
					completions.add("dummyleader");
				}
				return completions;
			}
		}
		else if(cmd.getName().equalsIgnoreCase("faction")
				&& args.length >= 1
				&& args.length <= 2
				&& args[0].equalsIgnoreCase("construct")) {
			if(sender instanceof Player) {
				Player p = (Player) sender;
				if(FactionManager.getByLeader(p.getName()) == null) {
					return new ArrayList<>();
				}
				return completeConstructKinds(args);
			}
		}
		else if(cmd.getName().equalsIgnoreCase("faction")
				&& args.length >= 1
				&& args.length <= 2
				&& args[0].equalsIgnoreCase("deconstruct")) {
			if(sender instanceof Player) {
				Player p = (Player) sender;
				return completeDeconstructIds(p, args);
			}
		}
		else if(cmd.getName().equalsIgnoreCase("faction") && args.length >= 0 && args.length < 2 ) {
			if(sender instanceof Player){
				Player p = (Player) sender;
				List<String> completions = new ArrayList<>();
				completions.add("list");
				if(FactionManager.getByMember(p.getName()) != null) completions.add("menu");
				completions.add("warlist");
				completions.add("create");
				completions.add("accept");
				completions.add("join");
				
				if(FactionManager.getByLeader(p.getName()) != null) {
					completions.add("claim");
					completions.add("construct");
					completions.add("deconstruct");
					completions.add("unclaim");
					completions.add("withdraw");
					completions.add("setbank");
					completions.add("delete");
					completions.add("invite");
					completions.add("kick");
					completions.add("setleader");
					completions.add("rename");
					completions.add("setculture");
					completions.add("setreligion");
					completions.add("setrulingsystem");
					completions.add("setrulertitle");
					completions.add("setbanner");
					completions.add("setcolour");
				}
				if(Permissions.isAdmin(sender)) {
					completions.add("forceleader");
					completions.add("forcejoin");
					completions.add("forcewithdraw");
					completions.add("addprestigemodifier");
					//completions.add("addwealthmodifier");
					completions.add("getglobalwealth");
					completions.add("queueallnations");
					completions.add("fullregen");
					completions.add("reloadtitles");
					completions.add("endwar");
					completions.add("warstatus");
					completions.add("warpath");
					completions.add("destroytitle");
					completions.add("granttitle");
					completions.add("transfersubject");
					completions.add("setrelation");
					completions.add("usurp");
					completions.add("startelection");
					completions.add("endelection");
				}
				TabCleaner.cleanTab(completions, args);
				return completions;
			}
		} else if(eitherCommand(cmd) && args.length == 2 && args[0].equalsIgnoreCase("create")){
			if(sender instanceof Player){
				List<String> completions = new ArrayList<String>();
				completions.add("<id>");
				
				return completions;
			}
		} else if(eitherCommand(cmd) && args.length == 2 && args[0].equalsIgnoreCase("setcapital")) {
			if(sender instanceof Player) {
				Player p = (Player) sender;
				Faction f = null;
				if(cmd.getName().equalsIgnoreCase("guild")) {
					Guild g = FactionManager.getGuildByLeader(p.getName());
					if(g != null) {
						f = g.getFaction();
					}
				} else {
					f = FactionManager.getByLeader(p.getName());
				}
				if(f == null) {
					return new ArrayList<>();
				}
				List<String> completions = new ArrayList<>();
				for(var s : f.getSettlementHandler().getAll()) {
					completions.add(s.getId());
				}
				TabCleaner.cleanTab(completions, args);
				return completions;
			}
		} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("delete")){
			if(sender instanceof Player){
				Player p = (Player) sender;
				Faction f = FactionManager.getByMember(p.getName());
				List<String> completions = new ArrayList<String>();
				if(f != null) {
					completions.add(f.getId());
				}
				return completions;
			}
		} else if(cmd.getName().equalsIgnoreCase("guild") && args.length == 2 && args[0].equalsIgnoreCase("delete")){
			if(sender instanceof Player){
				Player p = (Player) sender;
				Guild g = FactionManager.getGuildByMember(p.getName());
				List<String> completions = new ArrayList<String>();
				if(g != null && !g.isBase()) {
					completions.add(g.getId());
				}
				if(Permissions.isAdmin(sender)) {
					for(Faction fac : FactionManager.factions) {
						for(Guild guild : fac.getGuildHandler().getGuilds()) {
							if(!completions.contains(guild.getId()) && !guild.isBase()) {
								completions.add(guild.getId());
							}
						}
					}
				}
				return completions;
			}
		} else if(eitherCommand(cmd) && args.length == 2 && args[0].equalsIgnoreCase("invite")){
			if(sender instanceof Player){
				List<String> completions = new ArrayList<String>();
				for(Player p : Bukkit.getOnlinePlayers()) {
					completions.add(p.getName());
				}
				return completions;
			}
		} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("kick")){
			if(sender instanceof Player){
				Player p = (Player) sender;
				Faction f = FactionManager.getByMember(p.getName());
				List<String> completions = new ArrayList<String>();
				if(f != null) {
					completions.addAll(f.getMembers());
					completions.remove(f.getLeader());
				}
				return completions;
			}
		} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("setleader")){
			if(sender instanceof Player){
				Player p = (Player) sender;
				Faction f = FactionManager.getByMember(p.getName());
				List<String> completions = new ArrayList<String>();
				if(f != null) {
					for(String member : f.getMembers()) {
						if(f.canBecomeLeader(member)) completions.add(member);
					}
				}
				return completions;
			}
		} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("rename")){
			if(sender instanceof Player){
				List<String> completions = new ArrayList<String>();
				completions.add("<id>");
				
				return completions;
			}
		} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("setculture")){
			if(sender instanceof Player){
				List<String> completions = new ArrayList<String>();
				completions.add("<culture>");
				
				return completions;
			}
		} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("setreligion")){
			if(sender instanceof Player){
				List<String> completions = new ArrayList<String>();
				completions.add("<religion>");
				
				return completions;
			}
		} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("setrulingsystem")){
			if(sender instanceof Player){
				List<String> completions = new ArrayList<String>();
				completions.add("<ruling system>");
				
				return completions;
			}
		} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("setrulertitle")){
			if(sender instanceof Player){
				List<String> completions = new ArrayList<String>();
				completions.add("<ruler title>");
				
				return completions;
			}
		} else if(eitherCommand(cmd) && args.length == 2 && args[0].equalsIgnoreCase("setbanner")){
			if(sender instanceof Player){
				List<String> completions = new ArrayList<String>();
				
				return completions;
			}
		} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("claim")){
			if(sender instanceof Player){
				List<String> completions = new ArrayList<String>();
				
				return completions;
			}
		} else if(cmd.getName().equalsIgnoreCase("faction") && args.length >= 3 && args[0].equalsIgnoreCase("construct")){
			if(sender instanceof Player){
				List<String> completions = new ArrayList<>();
				completions.add("<name>");
				return completions;
			}
		} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("unclaim")){
			if(sender instanceof Player){
				List<String> completions = new ArrayList<String>();
				
				return completions;
			}
		} else if(eitherCommand(cmd) && args.length == 2 && args[0].equalsIgnoreCase("accept")){
			if(sender instanceof Player){
				List<String> completions = new ArrayList<String>();
				
				return completions;
			}
		} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("setcolour")){
			if(sender instanceof Player){
				List<String> completions = new ArrayList<String>();
				completions.add("R,G,B");
				return completions;
			}
		}
		if(Permissions.isAdmin(sender)) {
			if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("forceleader")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					for(Faction f : FactionManager.factions) {
						completions.add(f.getId());
					}
					
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 3 && args[0].equalsIgnoreCase("forceleader")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					
					Faction f = FactionManager.getByString(args[1]);
					if(f != null) {
						for(String member : f.getMembers()) {
							if(f.canBecomeLeader(member)) completions.add(member);
						}
					}
					return completions;
				}
			} else if (args.length == 2 && args[0].equalsIgnoreCase("forcejoin")) {
				List<String> completions = new ArrayList<String>();
				// Suggest faction names
				for (Faction f : FactionManager.factions) {
					if (f.getId().toLowerCase().startsWith(args[1].toLowerCase())) {
						completions.add(f.getId());
					}
				}
				return completions;
			} 
			else if (args.length == 3 && args[0].equalsIgnoreCase("forcejoin")) {
				List<String> completions = new ArrayList<String>();
				// Suggest players who are valid to be forcejoined
				Faction f = FactionManager.getByString(args[1]);
				if (f != null) {
					for (Player pl : Bukkit.getOnlinePlayers()) {
						String name = pl.getName();
						if (name.toLowerCase().startsWith(args[2].toLowerCase())
							&& !f.getMembers().contains(name)
							&& FactionManager.getByMember(name) == null) {
							completions.add(name);
						}
					}
				}
				return completions;
			} if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("forcewithdraw")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					for(Faction f : FactionManager.factions) {
						completions.add(f.getId());
					}
					
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 3 && args[0].equalsIgnoreCase("forcewithdraw")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					completions.add("1.0");
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("addprestigemodifier")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					completions.add("<type>");
					
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("forcedelete")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					for(Faction f : FactionManager.factions) {
						completions.add(f.getId());
					}
					
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 3 && args[0].equalsIgnoreCase("addprestigemodifier")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					completions.add("<amount>");
					
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("addwealthmodifier")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					completions.add("<type>");
					
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 3 && args[0].equalsIgnoreCase("addwealthmodifier")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					completions.add("<amount>");
					
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2
					&& (args[0].equalsIgnoreCase("endwar")
							|| args[0].equalsIgnoreCase("warstatus")
							|| args[0].equalsIgnoreCase("warpath"))) {
				if(sender instanceof Player){
					List<String> completions = new ArrayList<>();
					for (War war : WarManager.getActive()) {
						completions.add(String.valueOf(war.getId()));
					}
					TabCleaner.cleanTab(completions, args);
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("destroytitle")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					for(Title t : TitleManager.getAllOwnedTitles()) {
						completions.add(t.getId());
					}
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("granttitle")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					for(Faction f : FactionManager.factions) {
						completions.add(f.getId());
					}
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 3 && args[0].equalsIgnoreCase("granttitle")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					for(Title t : TitleManager.getAllUnownedTitles()) {
						completions.add(t.getId());
					}
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("transfersubject")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					for(Faction f : FactionManager.factions) {
						if(RelationManager.getOverlord(f) == null) continue;
						completions.add(f.getId());
					}
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 3 && args[0].equalsIgnoreCase("transfersubject")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					for(Faction f : FactionManager.factions) {
						if(f.getId().equalsIgnoreCase(args[1])) continue;
						completions.add(f.getId());
					}
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("setrelation")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					for(Faction f : FactionManager.factions) {
						completions.add(f.getId());
					}
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 3 && args[0].equalsIgnoreCase("setrelation")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					for(Faction f : FactionManager.factions) {
						if(f.getId().equalsIgnoreCase(args[1])) continue;
						completions.add(f.getId());
					}
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 4 && args[0].equalsIgnoreCase("setrelation")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					for(RelationType type : RelationLoader.getTypes()) {
						completions.add(type.getId());
					}
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("usurp")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					for(Faction f : FactionManager.factions) {
						completions.add(f.getId());
					}
					return completions;
				}
			} else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 3 && args[0].equalsIgnoreCase("usurp")){
				if(sender instanceof Player){
					List<String> completions = new ArrayList<String>();
					for(Faction f : FactionManager.factions) {
						if(f.getId().equalsIgnoreCase(args[1])) continue;
						if(f.getTitles().size() == 0) continue;
						completions.add(f.getId());
					}
					return completions;
				}
			}
		}
    	return null;
    }
}
