package me.Plugins.SimpleFactions.Managers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Database.Database;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.Request.WarRequest;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.core.Side;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.War.core.WarCommitment;
import me.Plugins.SimpleFactions.War.core.WarDeclareHelper;
import me.Plugins.SimpleFactions.War.campaign.runtime.BattleInstallationPickService;
import me.Plugins.SimpleFactions.War.campaign.raid.CampaignRaidService;
import me.Plugins.SimpleFactions.War.combat.WarCombatTeardownService;
import me.Plugins.SimpleFactions.War.campaign.WarCampaignService;
import me.Plugins.SimpleFactions.War.commitment.WarCommitmentService;
import me.Plugins.SimpleFactions.War.enums.WarEndReason;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;
import me.Plugins.SimpleFactions.War.campaign.progression.CampaignDeclareValidator;
import me.Plugins.SimpleFactions.War.declare.WarDeclareRequest;
import me.Plugins.SimpleFactions.War.declare.WarGoalValidator;
import me.Plugins.SimpleFactions.War.declare.WarValidationResult;

public class WarManager {
	private static List<War> wars = new ArrayList<>();
	private static String lastDeclareError;
	
	public static String getLastDeclareError() {
		return lastDeclareError;
	}
	
	public static War declareWar(
			Faction attacker,
			Faction defender,
			WarGoalType goal,
			String targetTitleId,
			String subjectFactionId) {
		WarDeclareRequest request = new WarDeclareRequest(attacker, defender, goal, targetTitleId, subjectFactionId);
		WarValidationResult validation = new WarGoalValidator().validate(request);
		if (!validation.isValid()) {
			lastDeclareError = validation.getMessage();
			return null;
		}

		lastDeclareError = null;

		WarValidationResult military = CampaignDeclareValidator.validateAttackerCanDeclare(attacker);
		if (!military.isValid()) {
			lastDeclareError = military.getMessage();
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
			lastDeclareError = "§cCould not declare war: no campaign route could be generated.";
			return null;
		}
		WarCommitmentService.commitAllParticipants(war);
		addWar(war);
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

	public static List<WarCommitment> getCommitmentsForWar(int warId) {
		return WarCommitmentService.getCommitmentsForWar(warId);
	}

	static void clearCommitments(int warId) {
		WarCommitmentService.clearCommitments(warId);
	}

	public static War addWar(War w) {
		wars.add(w);
		for(String m : w.getAttackers().getLeader().getMembers()){
			Player p = Bukkit.getPlayerExact(m);
			if(p != null && p.isOnline()){
				p.sendTitle("§cWar Declared!", "§e/war list §7to view", 10, 120, 10);
				p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_2, SoundCategory.MASTER, 10f, 0.6f);
			}
		}
		for(String m : w.getDefenders().getLeader().getMembers()){
			Player p = Bukkit.getPlayerExact(m);
			if(p != null && p.isOnline()){
				p.sendTitle("§cWar Declared!", "§e/war list §7to view", 10, 120, 10);
				p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_2, SoundCategory.MASTER, 10f, 0.6f);
			}
		}
		persist(w);
		return w;
	}

	public static void persist(War war) {
		new Database().saveWar(war);
	}
	
	public static War findSharedActiveWar(Faction a, Faction b) {
		if (a == null || b == null) {
			return null;
		}
		for (War war : wars) {
			if (!war.isActive()) {
				continue;
			}
			if (war.isParticipating(a) && war.isParticipating(b)) {
				return war;
			}
		}
		return null;
	}

	public static boolean exists(Faction attacker, Faction defender) {
		return findSharedActiveWar(attacker, defender) != null;
	}

	public static boolean existsHostile(Faction attacker, Faction defender) {
		War war = findSharedActiveWar(attacker, defender);
		if (war == null) {
			return false;
		}
		Side attackerSide = war.getSide(attacker);
		Side defenderSide = war.getSide(defender);
		return attackerSide != null && defenderSide != null && !attackerSide.equals(defenderSide);
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
		WarCombatTeardownService.teardownCombatForWar(w);
		w.end(reason);
		BattleInstallationPickService.clearForNewBattleDay(w);
		CampaignRaidService.clearForNewBattleDay(w);
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
			case WHITE_PEACE -> "§7The war has ended in white peace.";
			case ATTACKER_VICTORY -> "§7The war has ended. The attacker coalition wins.";
			case DEFENDER_VICTORY -> "§7The war has ended. The defender coalition wins.";
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
		WarCommitmentService.commitFaction(war, reciever);
		WarCommitmentService.snapshotLevyForFighter(war, reciever);
		Player sp = Bukkit.getPlayerExact(origin.getLeader());
		if(sp != null && sp.isOnline()) sp.sendMessage(reciever.getName()+" §aaccepted your call to arms");
		p.sendMessage("§aYour faction has joined the "+war.getName());
		persist(war);
	}
}
