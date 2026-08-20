package me.Plugins.SimpleFactions.War;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.ObjectiveHolder;
import me.Plugins.SimpleFactions.War.enums.WarEndReason;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarStatus;
import me.Plugins.SimpleFactions.War.enums.WarType;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class War {
	private int id;
	private int schemaVersion = 2;
	private Side attackers;
	private Side defenders;
	private WarGoalType goal;
	private WarType warType;
	private WarStatus status = WarStatus.ACTIVE;
	private String attackerLeaderId;
	private String defenderLeaderId;
	private String targetTitleId;
	private String subjectFactionId;
	private Integer objectiveProvinceId;
	private Integer campaignStartProvinceId;
	private List<Integer> campaignProvinces;
	private int cursorIndex;
	private int initiativeAttacker;
	private int initiativeDefender;
	private List<Integer> occupiedByAttacker;
	private List<Integer> occupiedByDefender;
	private List<Integer> lastBattleOccupied;
	private CampaignPhase campaignPhase = CampaignPhase.INVASION;
	private ObjectiveHolder objectiveHeldBy = ObjectiveHolder.DEFENDER;
	private boolean whitePeaceProposedByAttacker;
	private boolean whitePeaceProposedByDefender;
	private int campaignBattlesFought;
	private BattleSchedulePhase battleSchedulePhase = BattleSchedulePhase.IDLE;
	private LocalDate battleDay;
	private Instant scheduledBattleAt;
	private int scheduledBattleHour;
	private Integer scheduledBattleProvinceId;
	private Map<UUID, Set<Integer>> battleVotes = new HashMap<>();
	private boolean autoresolveProposedByAttacker;
	private boolean autoresolveProposedByDefender;
	private int postponementsThisCycle;
	private boolean defenderChoiceResolved;
	private Instant startedAt;
	private Instant endedAt;
	private WarEndReason endReason;

	public War(Faction attacker, Faction defender) {
		this(WarManager.newId(), attacker, defender);
	}

	public War(int i, Faction attacker, Faction defender) {
		id = i;
		attackers = new Side(attacker);
		defenders = new Side(defender);
		attackerLeaderId = attacker.getId();
		defenderLeaderId = defender.getId();
		status = WarStatus.ACTIVE;
		startedAt = Instant.now();
		schemaVersion = 2;
	}

	public War(
			int id,
			Side attackers,
			Side defenders,
			WarGoalType goal,
			WarType warType,
			String targetTitleId,
			Integer objectiveProvinceId,
			Instant startedAt) {
		this.id = id;
		this.attackers = attackers;
		this.defenders = defenders;
		this.goal = goal;
		this.warType = warType;
		this.targetTitleId = targetTitleId;
		this.objectiveProvinceId = objectiveProvinceId;
		this.cursorIndex = 0;
		this.attackerLeaderId = attackers.getLeader().getId();
		this.defenderLeaderId = defenders.getLeader().getId();
		this.status = WarStatus.ACTIVE;
		this.startedAt = startedAt != null ? startedAt : Instant.now();
		this.schemaVersion = 2;
	}

	public int getId() {
		return id;
	}

	/** Battle-system identifier; same as {@link #getId()}. */
	public int getWarId() {
		return id;
	}

	public int getSchemaVersion() {
		return schemaVersion;
	}

	void setSchemaVersion(int schemaVersion) {
		this.schemaVersion = schemaVersion;
	}

	public Side getAttackers() {
		return attackers;
	}

	public Side getDefenders() {
		return defenders;
	}

	public WarGoalType getGoal() {
		return goal;
	}

	public void setGoal(WarGoalType goal) {
		this.goal = goal;
	}

	public WarType getWarType() {
		return warType;
	}

	public void setWarType(WarType warType) {
		this.warType = warType;
	}

	public WarStatus getStatus() {
		return status;
	}

	void setStatus(WarStatus status) {
		this.status = status;
	}

	public String getAttackerLeaderId() {
		return attackerLeaderId;
	}

	void setAttackerLeaderId(String attackerLeaderId) {
		this.attackerLeaderId = attackerLeaderId;
	}

	public String getDefenderLeaderId() {
		return defenderLeaderId;
	}

	void setDefenderLeaderId(String defenderLeaderId) {
		this.defenderLeaderId = defenderLeaderId;
	}

	public String getTargetTitleId() {
		return targetTitleId;
	}

	public void setTargetTitleId(String targetTitleId) {
		this.targetTitleId = targetTitleId;
	}

	public String getSubjectFactionId() {
		return subjectFactionId;
	}

	public void setSubjectFactionId(String subjectFactionId) {
		this.subjectFactionId = subjectFactionId;
	}

	public Integer getObjectiveProvinceId() {
		return objectiveProvinceId;
	}

	public void setObjectiveProvinceId(Integer objectiveProvinceId) {
		this.objectiveProvinceId = objectiveProvinceId;
	}

	public Integer getCampaignStartProvinceId() {
		return campaignStartProvinceId;
	}

	public void setCampaignStartProvinceId(Integer campaignStartProvinceId) {
		this.campaignStartProvinceId = campaignStartProvinceId;
	}

	public List<Integer> getCampaignProvinces() {
		return campaignProvinces;
	}

	public void setCampaignProvinces(List<Integer> campaignProvinces) {
		this.campaignProvinces = campaignProvinces;
	}

	public int getCursorIndex() {
		return cursorIndex;
	}

	public void setCursorIndex(int cursorIndex) {
		this.cursorIndex = cursorIndex;
	}

	public int getInitiativeAttacker() {
		return initiativeAttacker;
	}

	public void setInitiativeAttacker(int initiativeAttacker) {
		this.initiativeAttacker = initiativeAttacker;
	}

	public int getInitiativeDefender() {
		return initiativeDefender;
	}

	public void setInitiativeDefender(int initiativeDefender) {
		this.initiativeDefender = initiativeDefender;
	}

	public List<Integer> getOccupiedByAttacker() {
		return occupiedByAttacker;
	}

	public void setOccupiedByAttacker(List<Integer> occupiedByAttacker) {
		this.occupiedByAttacker = occupiedByAttacker;
	}

	public List<Integer> getOccupiedByDefender() {
		return occupiedByDefender;
	}

	public void setOccupiedByDefender(List<Integer> occupiedByDefender) {
		this.occupiedByDefender = occupiedByDefender;
	}

	public List<Integer> getLastBattleOccupied() {
		return lastBattleOccupied;
	}

	public void setLastBattleOccupied(List<Integer> lastBattleOccupied) {
		this.lastBattleOccupied = lastBattleOccupied;
	}

	public CampaignPhase getCampaignPhase() {
		return campaignPhase;
	}

	public void setCampaignPhase(CampaignPhase campaignPhase) {
		this.campaignPhase = campaignPhase;
	}

	public ObjectiveHolder getObjectiveHeldBy() {
		return objectiveHeldBy;
	}

	public void setObjectiveHeldBy(ObjectiveHolder objectiveHeldBy) {
		this.objectiveHeldBy = objectiveHeldBy;
	}

	public boolean isWhitePeaceProposedByAttacker() {
		return whitePeaceProposedByAttacker;
	}

	public void setWhitePeaceProposedByAttacker(boolean whitePeaceProposedByAttacker) {
		this.whitePeaceProposedByAttacker = whitePeaceProposedByAttacker;
	}

	public boolean isWhitePeaceProposedByDefender() {
		return whitePeaceProposedByDefender;
	}

	public void setWhitePeaceProposedByDefender(boolean whitePeaceProposedByDefender) {
		this.whitePeaceProposedByDefender = whitePeaceProposedByDefender;
	}

	public int getCampaignBattlesFought() {
		return campaignBattlesFought;
	}

	public void setCampaignBattlesFought(int campaignBattlesFought) {
		this.campaignBattlesFought = campaignBattlesFought;
	}

	public BattleSchedulePhase getBattleSchedulePhase() {
		return battleSchedulePhase;
	}

	public void setBattleSchedulePhase(BattleSchedulePhase battleSchedulePhase) {
		this.battleSchedulePhase = battleSchedulePhase != null ? battleSchedulePhase : BattleSchedulePhase.IDLE;
	}

	public LocalDate getBattleDay() {
		return battleDay;
	}

	public void setBattleDay(LocalDate battleDay) {
		this.battleDay = battleDay;
	}

	public Instant getScheduledBattleAt() {
		return scheduledBattleAt;
	}

	public void setScheduledBattleAt(Instant scheduledBattleAt) {
		this.scheduledBattleAt = scheduledBattleAt;
	}

	public int getScheduledBattleHour() {
		return scheduledBattleHour;
	}

	public void setScheduledBattleHour(int scheduledBattleHour) {
		this.scheduledBattleHour = scheduledBattleHour;
	}

	public Integer getScheduledBattleProvinceId() {
		return scheduledBattleProvinceId;
	}

	public void setScheduledBattleProvinceId(Integer scheduledBattleProvinceId) {
		this.scheduledBattleProvinceId = scheduledBattleProvinceId;
	}

	public Map<UUID, Set<Integer>> getBattleVotes() {
		if (battleVotes == null) {
			battleVotes = new HashMap<>();
		}
		return battleVotes;
	}

	public void setBattleVotes(Map<UUID, Set<Integer>> battleVotes) {
		this.battleVotes = battleVotes != null ? battleVotes : new HashMap<>();
	}

	public boolean isAutoresolveProposedByAttacker() {
		return autoresolveProposedByAttacker;
	}

	public void setAutoresolveProposedByAttacker(boolean autoresolveProposedByAttacker) {
		this.autoresolveProposedByAttacker = autoresolveProposedByAttacker;
	}

	public boolean isAutoresolveProposedByDefender() {
		return autoresolveProposedByDefender;
	}

	public void setAutoresolveProposedByDefender(boolean autoresolveProposedByDefender) {
		this.autoresolveProposedByDefender = autoresolveProposedByDefender;
	}

	public int getPostponementsThisCycle() {
		return postponementsThisCycle;
	}

	public void setPostponementsThisCycle(int postponementsThisCycle) {
		this.postponementsThisCycle = postponementsThisCycle;
	}

	public boolean isDefenderChoiceResolved() {
		return defenderChoiceResolved;
	}

	public void setDefenderChoiceResolved(boolean defenderChoiceResolved) {
		this.defenderChoiceResolved = defenderChoiceResolved;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	public Instant getEndedAt() {
		return endedAt;
	}

	void setEndedAt(Instant endedAt) {
		this.endedAt = endedAt;
	}

	public WarEndReason getEndReason() {
		return endReason;
	}

	void setEndReason(WarEndReason endReason) {
		this.endReason = endReason;
	}

	public boolean isActive() {
		return status == WarStatus.ACTIVE;
	}

	public void end(WarEndReason reason) {
		status = WarStatus.ENDED;
		endReason = reason;
		endedAt = Instant.now();
	}

	public void update() {
		for (Participant p : getParticipants()) {
			p.update(this);
		}
	}

	public List<Participant> getParticipants() {
		List<Participant> list = new ArrayList<>();
		list.addAll(attackers.getMainParticipants());
		list.addAll(defenders.getMainParticipants());
		return list;
	}

	public String getName() {
		Faction attacker = attackers.getLeader();
		Faction defender = defenders.getLeader();
		return StringFormatter.formatHex(attacker.getName() + " #a83116vs. " + defender.getName());
	}

	public Participant getParticipant(Faction f) {
		for (Participant p : attackers.getMainParticipants()) {
			if (p.getLeader().getId().equalsIgnoreCase(f.getId())) return p;
		}
		for (Participant p : defenders.getMainParticipants()) {
			if (p.getLeader().getId().equalsIgnoreCase(f.getId())) return p;
		}
		return null;
	}

	public Side getSide(Faction f) {
		for (Participant p : attackers.getMainParticipants()) {
			if (p.getLeader().getId().equalsIgnoreCase(f.getId())) return attackers;
			if (p.getSubjects().contains(f)) return attackers;
			if (p.getAllies().containsKey(f) && p.getAllies().get(f)) return attackers;
		}
		for (Participant p : defenders.getMainParticipants()) {
			if (p.getLeader().getId().equalsIgnoreCase(f.getId())) return defenders;
			if (p.getSubjects().contains(f)) return defenders;
			if (p.getAllies().containsKey(f) && p.getAllies().get(f)) return defenders;
		}
		return null;
	}

	public Side getSide(Participant p) {
		return getSide(p.getLeader());
	}

	public Side getOppositeSide(Faction f) {
		Side same = getSide(f);
		if (same == null) return null;
		if (same.equals(attackers)) return defenders;
		if (same.equals(defenders)) return attackers;
		return null;
	}

	public HashMap<Faction, WarGoal> getWarGoalsOn(Faction p) {
		HashMap<Faction, WarGoal> map = new HashMap<>();
		Side side = getSide(p);
		if (side == null) return map;
		List<Participant> list;
		if (side.equals(defenders)) {
			list = attackers.getMainParticipants();
		} else {
			list = defenders.getMainParticipants();
		}
		for (Participant par : list) {
			if (par.hasWarGoal(p)) map.put(par.getLeader(), par.getWarGoal(p));
		}
		return map;
	}

	public boolean isMainParticipant(Faction f) {
		return getParticipant(f) != null;
	}

	public String getType(Faction f) {
		for (Participant p : attackers.getMainParticipants()) {
			if (p.getLeader().getId().equalsIgnoreCase(f.getId())) return "main_attacker";
		}
		for (Participant p : defenders.getMainParticipants()) {
			if (p.getLeader().getId().equalsIgnoreCase(f.getId())) return "main_defender";
		}
		return "secondary_participant";
	}

	public boolean canBeCalled(Faction f) {
		return !attackers.isParticipating(f) || !defenders.isParticipating(f);
	}

	public Faction getEnemy(Faction f) {
		if (attackers.isParticipating(f)) return defenders.getLeader();
		if (defenders.isParticipating(f)) return attackers.getLeader();
		return null;
	}

	public boolean call(Faction caller, Faction joiner) {
		Participant p = getParticipant(caller);
		if (p == null) return false;
		if (!p.getAllies().containsKey(joiner)) return false;
		if (p.getAllies().get(joiner) == true) return false;
		p.getAllies().put(joiner, true);
		return true;
	}
}
