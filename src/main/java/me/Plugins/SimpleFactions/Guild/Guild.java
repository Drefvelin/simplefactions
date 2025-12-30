package me.Plugins.SimpleFactions.Guild;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.Guild.Branch.Branch;
import me.Plugins.SimpleFactions.Loaders.GuildLoader;
import me.Plugins.SimpleFactions.Objects.Faction;

public class Guild {
    private String id;
    private String name;
    private String leader;
    private GuildType type;
    private List<String> members = new ArrayList<>();
    private Map<Integer, Branch> branches = new HashMap<>();

    private int capital = -1;

    public Guild(Faction f) {
        id = f.getId();
        name = f.getName();
        leader = f.getLeader();
        type = GuildLoader.getByString("realm");
    }

    public Guild(
        String id,
        String name,
        String leader,
        int capital,
        String type,
        List<String> members,
        Map<Integer, Branch> branches
    ) {
        this.id = id;
        this.name = name;
        this.leader = leader;
        this.capital = capital;
        this.members = members != null ? members : new ArrayList<>();
        this.branches = branches != null ? branches : new HashMap<>();
        this.type = GuildLoader.getByString(type);
    }


    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getMembers() { return members; }
    public boolean isMember(String p) { return members.contains(p); }
    public void addMember(String p) {
        if(isMember(p)) return;
        members.add(p);
    }
    public void kick(String member) {
        members.remove(member);
    }
    public String getLeader() { return leader; }
    public void setLeader(String leader) {
        this.leader = leader;
    }
    public boolean isLeader(String p) {
        return leader.equalsIgnoreCase(p);
    }
    public Map<Integer, Branch> getBranches() { return branches; }
    public GuildType getType() { return type; }
    public int getCapital() {
        return capital;
    }
    public void setCapital(int i) {
        capital = i;
    }
}
