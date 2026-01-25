package me.Plugins.SimpleFactions.Database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class GovernmentData {
    public Double power;
    
    @SerializedName("last election date")
    public Long lastElectionDate;
    
    @SerializedName("council members")
    public List<String> councilMembers = new ArrayList<>();
    
    public List<String> proposals = new ArrayList<>();
    
    @SerializedName("election candidates")
    public Map<String, List<String>> electionCandidates = new HashMap<>();
    
    @SerializedName("election votes")
    public Map<String, Map<String, String>> electionVotes = new HashMap<>();

    @SerializedName("previous votes")
    public Map<String, Map<String, Integer>> previousVotes = new HashMap<>();
}
