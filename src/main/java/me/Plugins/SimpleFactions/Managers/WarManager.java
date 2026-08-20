package me.Plugins.SimpleFactions.Managers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Army.Regiment;
import me.Plugins.SimpleFactions.Database.Database;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.Request.WarRequest;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.Side;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.WarCommitment;
import me.Plugins.SimpleFactions.War.WarDeclareHelper;
import me.Plugins.SimpleFactions.War.campaign.WarCampaignService;
import me.Plugins.SimpleFactions.War.enums.WarEndReason;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;
import me.Plugins.SimpleFactions.War.validation.WarDeclareRequest;
import me.Plugins.SimpleFactions.War.validation.WarGoalValidator;
import me.Plugins.SimpleFactions.War.validation.WarValidationResult;

public class WarManager {
	private static List<War> wars = new ArrayList<>();
	private static final Map<Integer, List<WarCommitment>> commitmentsByWar = new HashMap<>();
	
	public static War declareWar(
			Faction attacker,
			Faction defender,
			WarGoalType goal,
			String targetTitleId,
			String subjectFactionId) {
		WarDeclareRequest request = new WarDeclareRequest(attacker, defender, goal, targetTitleId, subjectFactionId);
		WarValidationResult validation = new WarGoalValidator().validate(request);
		if (!validation.isValid()) {
			return null;
		}

		boolean civilWar = RelationManager.endVassalage(attacker, defender, true);
		WarType warType = WarDeclareHelper.warTypeForGoal(goal);
		String storedTitleId = goal == WarGoalType.DE_JURE_ANNEX ? targetTitleId : null;
		War war = new War(
				newId(),
				new Side(attacker),
				new Side(defender),
				goal,
				warType,
				storedTitleId,
				null,
				Instant.now());
		if (goal == WarGoalType.TRANSFER_SUBJECT) {
			war.setSubjectFactionId(subjectFactionId);
		}
		if (!populateCampaignIfNeeded(war)) {
			return null;
		}
		addWar(war);
		commitForWar(war.getId(), attacker);
		commitForWar(war.getId(), defender);
		if (civilWar) {
			war.getParticipant(attacker).setCivilWar(true);
			war.getParticipant(defender).setCivilWar(true);
			persist(war);
		}
		return war;
	}

	static boolean populateCampaignIfNeeded(War war) {
		if (war.getWarType() == WarType.RAID) {
			return true;
		}
		ProvinceManager pm = resolveProvinceManager();
		if (pm == null) {
			return false;
		}
		return new WarCampaignService(pm).populateCampaign(war);
	}

	public static boolean regenerateCampaign(War war) {
		ProvinceManager pm = resolveProvinceManager();
		if (pm == null) {
			return false;
		}
		return regenerateCampaign(war, pm);
	}

	static boolean regenerateCampaign(War war, ProvinceManager pm) {
		if (war == null || !war.isActive() || pm == null) {
			return false;
		}
		if (war.getWarType() == WarType.RAID) {
			return false;
		}
		if (!new WarCampaignService(pm).populateCampaign(war)) {
			return false;
		}
		persist(war);
		return true;
	}

	private static ProvinceManager resolveProvinceManager() {
		SimpleFactions plugin = SimpleFactions.getInstance();
		if (plugin == null) {
			return null;
		}
		return plugin.getProvinceManager();
	}

	public static List<WarCommitment> commitForWar(int warId, Faction faction) {
		if (getById(warId) == null || faction == null) {
			return List.of();
		}

		List<WarCommitment> existing = getCommitmentsForFaction(warId, faction.getId());
		if (!existing.isEmpty()) {
			return existing;
		}

		Instant committedAt = Instant.now();
		List<WarCommitment> commitments = new ArrayList<>();
		for (Regiment regiment : faction.getMilitary().getRegiments()) {
			commitments.add(new WarCommitment(
					warId,
					faction.getId(),
					regiment.getId(),
					0,
					committedAt));
		}

		commitmentsByWar.computeIfAbsent(warId, ignored -> new ArrayList<>()).addAll(commitments);
		return List.copyOf(commitments);
	}

	public static List<WarCommitment> getCommitmentsForWar(int warId) {
		List<WarCommitment> commitments = commitmentsByWar.get(warId);
		if (commitments == null || commitments.isEmpty()) {
			return List.of();
		}
		return Collections.unmodifiableList(commitments);
	}

	private static List<WarCommitment> getCommitmentsForFaction(int warId, String factionId) {
		List<WarCommitment> all = commitmentsByWar.get(warId);
		if (all == null || all.isEmpty()) {
			return List.of();
		}
		List<WarCommitment> factionCommitments = new ArrayList<>();
		for (WarCommitment commitment : all) {
			if (commitment.factionId().equalsIgnoreCase(factionId)) {
				factionCommitments.add(commitment);
			}
		}
		return factionCommitments;
	}

	static void clearCommitments(int warId) {
		commitmentsByWar.remove(warId);
	}

	public static War addWar(War w) {
		wars.add(w);
		for(String m : w.getAttackers().getLeader().getMembers()){
			Player p = Bukkit.getPlayerExact(m);
			if(p != null && p.isOnline()){
				p.sendTitle("§cWar Declared!", "§e/faction warlist §7to view", 10, 120, 10);
				p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_2, SoundCategory.MASTER, 10f, 0.6f);
			}
		}
		for(String m : w.getDefenders().getLeader().getMembers()){
			Player p = Bukkit.getPlayerExact(m);
			if(p != null && p.isOnline()){
				p.sendTitle("§cWar Declared!", "§e/faction warlist §7to view", 10, 120, 10);
				p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_2, SoundCategory.MASTER, 10f, 0.6f);
			}
		}
		persist(w);
		return w;
	}

	public static void persist(War war) {
		new Database().saveWar(war);
	}
	
	public static boolean exists(Faction attacker, Faction defender) {
		for (War w : wars) {
			if (!w.isActive()) continue;
			if (w.isMainParticipant(attacker) && w.isMainParticipant(defender)) return true;
			Side s = w.getSide(attacker);
			if (s == null) continue;
			if (s.equals(w.getSide(defender))) return true;
		}
		return false;
	}
	
	public static boolean existsHostile(Faction attacker, Faction defender) {
		for(War w : wars) {
			Side s = w.getSide(attacker);
			if(s == null) continue;
			Side d = w.getSide(defender);
			if(d == null) continue;
		    if(!s.equals(d)) return true;
		}
		return false;
	}

	public static void start() {
		wars = (new Database()).loadWars();
	}

	public static void endWar(War w) {
		endWar(w, WarEndReason.ADMIN_END);
	}

	public static void endWar(War w, WarEndReason reason) {
		if (w == null || reason == null) {
			return;
		}
		w.end(reason);
		clearCommitments(w.getId());
		for (War war : wars) {
			if (war.getId() == w.getId()) {
				wars.remove(war);
				(new Database()).deleteWar(w);
				break;
			}
		}
		notifyWarEnded(w, reason);
	}

	private static void notifyWarEnded(War w, WarEndReason reason) {
		String message = switch (reason) {
			case WHITE_PEACE, AUTO_WHITE_PEACE -> "§7The war has ended in white peace.";
			default -> "§7The war has ended.";
		};
		for (String member : w.getAttackers().getLeader().getMembers()) {
			Player p = Bukkit.getPlayerExact(member);
			if (p != null && p.isOnline()) {
				p.sendMessage(message);
			}
		}
		for (String member : w.getDefenders().getLeader().getMembers()) {
			Player p = Bukkit.getPlayerExact(member);
			if (p != null && p.isOnline()) {
				p.sendMessage(message);
			}
		}
	}

	public static List<War> getActive() {
		List<War> active = new ArrayList<>();
		for (War war : wars) {
			if (war.isActive()) active.add(war);
		}
		return active;
	}
	
	public static List<War> get(){
		return wars;
	}
	
	public static War getById(int i) {
		for(War w : wars) {
			if(w.getId() == i) return w;
		}
		return null;
	}
	
	public static int newId() {
		int i = 0;
		while(getById(i) != null) i++;
		return i;
	}
	
	public static War getByFaction(Faction f) {
		for(War w : wars) {
			if(w.getSide(f) != null) return w;
		}
		return null;
	}
	
	public static void sendRequest(Player sender, Faction origin, Faction target, War w) {
		if(!w.canBeCalled(target)) {
			sender.sendMessage("§cTarget faction is already part of the war");
			return;
		}
		Player p = Bukkit.getPlayerExact(target.getLeader());
		if(p == null || !p.isOnline()) {
			sender.sendMessage("§cCannot send request, target faction leader is not online!");
			return;
		}
		sender.sendMessage("§aSent a call to arms to "+target.getName());
		p.sendMessage(FactionManager.getByLeader(sender.getName()).getName()+" §7is requesting that you aid them in their war against "+w.getEnemy(origin).getName());
		p.sendMessage("§7Type §a/faction accept §7to accept");
		p.sendMessage("§7Request will time out in 60 seconds");
		RequestManager.addRequest(sender, p, new WarRequest(FactionManager.getByLeader(sender.getName()).getOrCreateMainGuild(), w));
	}
	
	public static void acceptRequest(Player p) {
		WarRequest req = (WarRequest) RequestManager.getRequest(p);
		Faction reciever = FactionManager.getByLeader(p.getName());
		if(reciever == null) {
			p.sendMessage("§cYou do not have a faction");
			return;
		}
		Faction origin = req.getFaction();
		War war = req.getWar();
		if (!war.call(origin, reciever)) {
			p.sendMessage("§cCould not join the war.");
			return;
		}
		Player sp = Bukkit.getPlayerExact(origin.getLeader());
		if(sp != null && sp.isOnline()) sp.sendMessage(reciever.getName()+" §aaccepted your call to arms");
		p.sendMessage("§aYour faction has joined the "+war.getName());
		persist(war);
	}
}
