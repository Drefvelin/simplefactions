package me.Plugins.SimpleFactions.Database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarData {
    public int schemaVersion = 2;
    public int id;
    public String status;
    public String goal;
    public String warType;
    public String attackerLeaderId;
    public String defenderLeaderId;
    public String targetTitleId;
    public String subjectFactionId;
    public Integer objectiveProvinceId;
    public Integer campaignStartProvinceId;
    public List<Integer> campaignProvinces;
    public int cursorIndex;
    public Integer initiativeAttacker;
    public Integer initiativeDefender;
    public String initiativeHolder;
    public List<Integer> occupiedByAttacker;
    public List<Integer> occupiedByDefender;
    public List<Integer> lastBattleOccupied;
    public String campaignPhase;
    public String objectiveHeldBy;
    public boolean whitePeaceProposedByAttacker;
    public boolean whitePeaceProposedByDefender;
    public Integer campaignBattlesFought;
    public List<ScheduledCampaignBattleData> campaignBattleSchedule;
    public Integer campaignScheduleIndex;
    public List<ScheduledCampaignBattleData> campaignCounterSchedule;
    public Integer campaignCounterScheduleIndex;
    public Map<String, String> fortControllers;
    public Map<String, Integer> locationBattleCounts;
    public String battleSchedulePhase;
    public String battleDay;
    public String scheduledBattleAt;
    public Integer scheduledBattleHour;
    public Integer scheduledBattleProvinceId;
    public Map<String, List<Integer>> battleVotes;
    public boolean autoresolveProposedByAttacker;
    public boolean autoresolveProposedByDefender;
    public Integer postponementsThisCycle;
    public boolean defenderChoiceResolved;
    public String initiativeHolderCoalition;
    public String pushTarget;
    public String postBattleChoicePhase;
    public String postBattleWinnerCoalition;
    public Boolean postBattleChoiceResolved;
    public String lastBattleOffensiveCoalition;
    public boolean holdPeaceProposalActive;
    public boolean forceQuorumNextClose;
    public String startedAt;
    public String endedAt;
    public String endReason;
    public SideData attackers;
    public SideData defenders;
    public List<CommitmentData> commitments;

    public WarData() {
        campaignProvinces = new ArrayList<>();
        occupiedByAttacker = new ArrayList<>();
        occupiedByDefender = new ArrayList<>();
        lastBattleOccupied = new ArrayList<>();
        battleVotes = new HashMap<>();
        locationBattleCounts = new HashMap<>();
        fortControllers = new HashMap<>();
        campaignBattleSchedule = new ArrayList<>();
        campaignCounterSchedule = new ArrayList<>();
    }
}
