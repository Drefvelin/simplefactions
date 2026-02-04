package me.Plugins.SimpleFactions.Objects.Handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Objects.Faction;

public class GuildHandler {
    private Faction f;
    private Map<String, Guild> guilds = new HashMap<>();

    public GuildHandler(Faction f) {
        this.f = f;
    }

    public List<Guild> getGuilds() {
        return new ArrayList<>(guilds.values());
    }

    public List<Guild> getReleasableGuilds() {
        List<Guild> releasable = new ArrayList<>();
        for(Guild g : guilds.values()) {
            if(g.canBeElevated(null)) releasable.add(g);
        }
        return releasable;
    }

    public Guild getGuild(String id) {
        return guilds.getOrDefault(id, null);
    }

    public void removeGuild(String id) {
        guilds.remove(id);
        f.getProvinceHandler().revalidateClaims();
    }

    public Guild getGuildByMember(String member) {
        for(Guild g : guilds.values()) {
            if(g.isMember(member)) return g;
        }
        return null;
    }

    public boolean isGuildLeader(String p) {
        for(Guild g : guilds.values()) {
            if(g.isLeader(p)) return true;
        }
        return false;
    }

    public void forceKick(String member) {
        for(Guild g : guilds.values()) {
            if(g.isMember(member)) g.kick(member);
        }
    }

    public void addGuild(Guild g) {
        guilds.put(g.getId(), g);
    }

    public List<String> getAllMembers() {
        List<String> members = new ArrayList<>();
        for(Guild g : guilds.values()) {
            members.addAll(g.getMembers());
        }
        Collections.sort(members);
        return members;
    }

    public double getTotalTradePower() {
        double total = 0;
        for(Guild g : guilds.values()) {
            total += g.getTradeBreakdown().getTradePower();
        }
        return total;
    }
}
