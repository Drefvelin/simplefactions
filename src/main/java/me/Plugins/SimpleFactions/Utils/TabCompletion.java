package me.Plugins.SimpleFactions.Utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.TLibs.Utils.TabCleaner;

public class TabCompletion implements TabCompleter{
    @Override
    public List<String> onTabComplete (CommandSender sender, Command cmd, String label, String[] args){
	        if(cmd.getName().equalsIgnoreCase("faction") && args.length >= 0 && args.length < 2 ) {
	            if(sender instanceof Player){
	            	Player p = (Player) sender;
	                List<String> completions = new ArrayList<>();
	                completions.add("list");
	                completions.add("warlist");
	                completions.add("create");
	                completions.add("accept");
	                
	                if(FactionManager.getByLeader(p.getName()) != null) {
	                	completions.add("claim");
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
		                completions.add("addprestigemodifier");
		                completions.add("addwealthmodifier");
		                completions.add("getglobalwealth");
						completions.add("queueallnations");
						completions.add("fullregen");
						completions.add("endwar");
	                }
	                TabCleaner.cleanTab(completions, args);
	                return completions;
	            }
	        } else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("create")){
	            if(sender instanceof Player){
	            	List<String> completions = new ArrayList<String>();
	            	completions.add("<id>");
	                
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
	        } else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("invite")){
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
	            		completions.addAll(f.getMembers());
		            	completions.remove(f.getLeader());
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
	        } else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("setbanner")){
	            if(sender instanceof Player){
	            	List<String> completions = new ArrayList<String>();
	                
	                return completions;
	            }
	        } else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("claim")){
	            if(sender instanceof Player){
	            	List<String> completions = new ArrayList<String>();
	                
	                return completions;
	            }
	        } else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("unclaim")){
	            if(sender instanceof Player){
	            	List<String> completions = new ArrayList<String>();
	                
	                return completions;
	            }
	        } else if(cmd.getName().equalsIgnoreCase("faction") && args.length == 2 && args[0].equalsIgnoreCase("accept")){
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
		            		completions.addAll(f.getMembers());
			            	completions.remove(f.getLeader());
		            	}
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
	        	}
	        }
    	return null;
    }
}
