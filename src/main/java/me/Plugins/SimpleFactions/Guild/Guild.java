package me.Plugins.SimpleFactions.Guild;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Guild.Branch.Branch;
import me.Plugins.SimpleFactions.Loaders.GuildLoader;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.Utils.RandomRGB;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class Guild {
    private Formatter format = new Formatter();

    private Faction host;

    private String id;
    private String name;
    private String leader;
    private String rgb;
    private GuildType type;
    private List<String> members = new ArrayList<>();
    private List<String> invites = new ArrayList<>();
    private Map<Integer, Branch> branches = new HashMap<>();

    private int capital = -1;

    public Guild(Faction f) {
        host = f;
        id = f.getId();
        rgb = RandomRGB.similarButDistinct(f.getRGB());
        while(!RandomRGB.isFree(rgb)) {
            rgb = RandomRGB.similarButDistinct(f.getRGB());
        }
        name = f.getName();
        leader = f.getLeader();
        members.add(leader);
        type = GuildLoader.getByString("realm");
    }

    public Guild(String id, Player p, Faction f, int province) {
        host = f;
        this.id = format.formatId(id);
		this.name = StringFormatter.formatHex(format.formatName(id));
        this.leader = p.getName();
        rgb = RandomRGB.random();
        while(!RandomRGB.isFree(rgb)) {
            rgb = RandomRGB.random();
        }
        this.members.add(leader);
        this.type = GuildLoader.getByString("guild");
        this.capital = province;
        f.getOrCreateMainGuild().kick(p.getName());
    }

    public Guild(
        String id,
        String name,
        String leader,
        String rgb,
        int capital,
        String type,
        List<String> members,
        Map<Integer, Branch> branches,
        Faction host
    ) {
        this.host = host;
        this.id = id;
        this.name = name;
        this.leader = leader;
        this.rgb = rgb;
        this.capital = capital;
        this.members = members != null ? members : new ArrayList<>();
        this.branches = branches != null ? branches : new HashMap<>();
        this.type = GuildLoader.getByString(type);
    }

    public Faction getFaction() { return host; }
    public List<String> getInvites() { return invites; }
    public boolean isInvited(String p) {
        return invites.contains(p);
    }
    public void invite(String p) {
        if(!invites.contains(p)) invites.add(p);
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getMembers() { return members; }
    public boolean isMember(String p) { return members.contains(p); }
    public void addMember(String p) {
        if(isMember(p)) return;
        if(isInvited(p)) invites.remove(p);
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
    public String getRGB() {
        return rgb;
    }
    public void setRGB(String rgb) {
        this.rgb = rgb;
    }
}
