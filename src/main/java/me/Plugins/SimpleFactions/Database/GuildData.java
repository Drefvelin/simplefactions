package me.Plugins.SimpleFactions.Database;

import java.util.ArrayList;
import java.util.List;

public class GuildData {
    public String id;
    public String name;
    public String leader;
    public String rgb;
    public String type;
    public Integer capital;

    public List<String> members = new ArrayList<>();
    public List<GuildBranchData> branches = new ArrayList<>();
}
