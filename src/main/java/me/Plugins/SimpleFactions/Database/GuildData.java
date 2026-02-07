package me.Plugins.SimpleFactions.Database;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class GuildData {
    public String id;
    public String name;
    public String leader;
    public String rgb;
    public String type;
    public Integer capital;

    public String bank;
    public String world;

    @SerializedName("xPos")
    public Double xPos;

    @SerializedName("zPos")
    public Double zPos;

    public Double balance;
    
    public List<String> banner = new ArrayList<>();

    public List<String> members = new ArrayList<>();
    public List<GuildBranchData> branches = new ArrayList<>();
    public List<GuildBranchData> upgrades = new ArrayList<>();
    
    @SerializedName("upgrade queue")
    public List<UpgradeExpansionData> upgradeQueue = new ArrayList<>();

    @SerializedName("wealth modifiers")
    public List<String> wealthModifiers = new ArrayList<>();

    public List<LoanData> loans = new ArrayList<>();
}
