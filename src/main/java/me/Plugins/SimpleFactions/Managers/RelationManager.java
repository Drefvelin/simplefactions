package me.Plugins.SimpleFactions.Managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.Plugins.SimpleFactions.Diplomacy.Attitude;
import me.Plugins.SimpleFactions.Diplomacy.Relation;
import me.Plugins.SimpleFactions.Diplomacy.RelationType;
import me.Plugins.SimpleFactions.Diplomacy.Threshold;
import me.Plugins.SimpleFactions.Loaders.RelationLoader;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.Request.RelationRequest;
import me.Plugins.SimpleFactions.Utils.OpinionColourMapper;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class RelationManager {
	
	private static int tick = 0;
	
	public static void tick() {
		tick++;
		if(tick >= 10) {
			tick = 0;
			for(Faction f : FactionManager.factions) {
				f.updateRelations();
			}
		}
	}
	
	public static void reset(Faction origin, Faction target, boolean hostile) {
		Relation relation = new Relation(origin.getRelation(target.getId()));
		Relation reverse = new Relation(target.getRelation(origin.getId()));
		relation.setType(RelationLoader.getDefaultType());
		reverse.setType(RelationLoader.getDefaultType());
		if(hostile) {
			relation.setAttitude(RelationLoader.getAttitude("hostile"));
			reverse.setAttitude(RelationLoader.getAttitude("hostile"));
		} else {
			relation.setAttitude(RelationLoader.getDefaultAttitude());
			reverse.setAttitude(RelationLoader.getDefaultAttitude());
		}
		origin.setRelation(target, relation);
		target.setRelation(origin, reverse);
	}

	public static boolean endVassalage(Faction origin, Faction target, boolean hostile) {
		if(isOverlord(origin, target) || isOverlord(target, origin)) {
			reset(origin, target, hostile);
			return true;
		}
		return false;
	}

	public static boolean isOverlord(Faction origin, Faction target) {
		String overlord = getOverlord(origin);
		if(overlord == null) return false;
		return overlord.equalsIgnoreCase(target.getId());
	}
	
	public static void setRelation(Player p, RelationType r, Faction target, Faction origin, boolean check) {
		Relation relation = new Relation(origin.getRelation(target.getId()));
		Relation reverse = new Relation(target.getRelation(origin.getId()));
		boolean reverseChange = reverseChange(target, origin, r);
		if(r.isVassalage()) {
			if(!vassalCheck(target, origin)) {
				p.sendMessage("§cThis faction is alredy a subject of someone else");
				return;
			}
			if(getTopLiege(origin).equalsIgnoreCase(target.getId())) {
				p.sendMessage("§cThis faction is your top overlord");
				return;
			}
		}
		if(r.hasThreshold()) {
			Threshold h = r.getThreshold();
			int opinion = origin.getRelation(target.getId()).getOpinion();
			boolean fulfilled = true;
			String plus = "";
			if(h.getOpinion() > 0) plus = "+";
			if(!h.fulfilled(opinion)) {
				p.sendMessage(StringFormatter.formatHex("§cYou need an opinion "+h.getFormattedType()+" "+OpinionColourMapper.getOpinionColor(h.getOpinion())+plus+h.getOpinion()+ "§c of them §7(currently "+opinion+")"));
				fulfilled = false;
			}
			if(h.isMutual()) {
				int reverseOpinion = target.getRelation(origin.getId()).getOpinion();
				if(!h.fulfilled(reverseOpinion)) {
					p.sendMessage(StringFormatter.formatHex("§cThey need an opinion "+h.getFormattedType()+" "+OpinionColourMapper.getOpinionColor(h.getOpinion())+plus+h.getOpinion()+ "§c of us §7(currently "+reverseOpinion+")"));
					fulfilled = false;
				}
			}
			if(!fulfilled) {
				return;
			}
		}
		if(r.isMutual() && check) {
			sendRequest(p, target, r);
			return;
		}
		if(r.shouldUpdateMap() || relation.getType().shouldUpdateMap()) {
			FactionManager.getMap().enqueue("nation", origin.getRGB());
			FactionManager.getMap().enqueue("nation", target.getRGB());
		}
		relation.setType(r);
		origin.setRelation(target, relation);;
		if(reverseChange) {
			Player l = Bukkit.getPlayerExact(target.getLeader());
			if(l != null && l.isOnline()) {
				l.sendMessage(StringFormatter.formatHex("#a89977Your relation with "+origin.getName()+" #a89977has been changed to "+r.getLink().getName()));
			}
			reverse.setType(r.getLink());
			target.setRelation(origin, reverse);
		} else if(r.willReset()) {
			reverse.setType(r.getLink());
			target.setRelation(origin, reverse);
		}
		p.sendMessage(StringFormatter.formatHex("#a89977Set relation to "+r.getName()));
	}
	
	public static boolean reverseChange(Faction target, Faction origin, RelationType t) {
		RelationType linked = t.getLink() != null ? t.getLink() : RelationLoader.getDefaultType();
		RelationType outgoing = origin.getRelation(target.getId()).getType();
		RelationType incoming = target.getRelation(origin.getId()).getType();
		return (outgoing.willReset() || incoming.willReset()) && !incoming.getId().equalsIgnoreCase(linked.getId());
	}
	
	public static String getOverlord(Faction f) {
		for(Map.Entry<String, Relation> entry : f.getRelations().entrySet()) {
			if(entry.getValue().getType().isOverlord()) return entry.getKey();
		}
		return null;
	}
	
	public static List<Faction> getAllies(Faction f){
		List<Faction> allies = new ArrayList<>();
		for(Map.Entry<String, Relation> entry : f.getRelations().entrySet()) {
			if(entry.getValue().getType().getId().equalsIgnoreCase("ally")) allies.add(FactionManager.getByString(entry.getKey()));
		}
		return allies;
	}
	
	public static List<Faction> getSubjects(Faction f){
		List<Faction> subjects = new ArrayList<>();
		for(Map.Entry<String, Relation> entry : f.getRelations().entrySet()) {
			if(entry.getValue().getType().isVassalage()) subjects.add(FactionManager.getByString(entry.getKey()));
		}
		return subjects;
	}
	
	public static String getTopLiege(Faction f) {
	    String liege = getOverlord(f);

	    while (liege != null) {
	        Faction overlord = FactionManager.getByString(liege);
	        if (overlord == null) {
	            break;
	        }

	        String nextLiege = getOverlord(overlord);
	        if (nextLiege == null) {
	            break;
	        }

	        liege = nextLiege;
	    }

	    return liege;
	}

	
	public static boolean vassalCheck(Faction target, Faction origin) {
		if(getOverlord(target) == null) return true;
		if(getOverlord(target).equalsIgnoreCase(origin.getId())) return true;
		return false;
	}
	
	public static void setAttitude(Player p, Attitude a, Faction target, Faction origin) {
		Relation r = origin.getRelation(target.getId());
		r.setAttitude(a);
		origin.setRelation(target, r);
		p.sendMessage(StringFormatter.formatHex("#a89977Set attitude to "+a.getName()));
	}
	
	private static void sendRequest(Player sender, Faction f, RelationType type) {
		Player p = Bukkit.getPlayerExact(f.getLeader());
		if(p == null || !p.isOnline()) {
			sender.sendMessage("§cCannot send request, target faction leader is not online!");
			return;
		}
		sender.sendMessage("§aSent a request to "+f.getName()+" §afor them to become your "+type.getName());
		p.sendMessage(FactionManager.getByLeader(sender.getName()).getName()+" §7is requesting that you become their "+type.getName());
		p.sendMessage("§7Type §a/faction accept §7to accept");
		p.sendMessage("§7Request will time out in 60 seconds");
		RequestManager.addRequest(sender, p, new RelationRequest(FactionManager.getByLeader(sender.getName()), type));
	}
	
	public static void acceptRequest(Player p) {
		RelationRequest req = (RelationRequest) RequestManager.getRequest(p);
		Faction reciever = FactionManager.getByLeader(p.getName());
		if(reciever == null) {
			p.sendMessage("§cYou do not have a faction");
			return;
		}
		Faction sender = req.getSender();
		Player sp = Bukkit.getPlayerExact(sender.getLeader());
		if(sp != null && sp.isOnline()) sp.sendMessage(reciever.getName()+" §aaccepted your request and became your "+req.getType().getName());
		setRelation(p, req.getType(), reciever, sender, false);
	}
}
