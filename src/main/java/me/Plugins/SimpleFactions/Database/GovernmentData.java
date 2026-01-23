package me.Plugins.SimpleFactions.Database;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class GovernmentData {
    public Double power;
    
    @SerializedName("last election date")
    public Long lastElectionDate;
    
    @SerializedName("council members")
    public List<String> councilMembers = new ArrayList<>();
    
    public List<String> proposals = new ArrayList<>();
}
