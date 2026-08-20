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
    public List<Integer> occupiedByAttacker;
    public List<Integer> occupiedByDefender;
    public List<Integer> lastBattleOccupied;
    public String campaignPhase;
    public String objectiveHeldBy;
    public boolean whitePeaceProposedByAttacker;
    public boolean whitePeaceProposedByDefender;
    public Integer campaignBattlesFought;
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
    public String startedAt;
    public String endedAt;
    public String endReason;
    public SideData attackers;
    public SideData defenders;

    public WarData() {
        campaignProvinces = new ArrayList<>();
        occupiedByAttacker = new ArrayList<>();
        occupiedByDefender = new ArrayList<>();
        lastBattleOccupied = new ArrayList<>();
        battleVotes = new HashMap<>();
    }
}
