package me.Plugins.SimpleFactions.Database;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class MovementData {
    public String leader;
    public Double organization;
    public List<CauseData> causes = new ArrayList<>();
    public PoolData supporters = new PoolData();
    
    @SerializedName("foreign backers")
    public List<String> foreignBackers = new ArrayList<>();
}
