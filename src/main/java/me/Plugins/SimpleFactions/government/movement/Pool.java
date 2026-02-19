package me.Plugins.SimpleFactions.government.movement;

import java.util.ArrayList;
import java.util.List;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class Pool {
    private List<String> members = new ArrayList<>();
    private List<Guild> guilds = new ArrayList<>();
    private List<Faction> factions = new ArrayList<>();

    public Pool() {}

    public Pool(List<String> members, List<Guild> guilds, List<Faction> factions) {
        this.members = members;
        this.guilds = guilds;
        this.factions = factions;
    }

    public List<String> getMembers() {
        return members;
    }

    public List<Guild> getGuilds() {
        return guilds;
    }

    public List<Faction> getFactions() {
        return factions;
    }

    public List<String> getAllMembers() {
        List<String> allMembers = new ArrayList<>(members);
        for (Guild guild : guilds) {
            allMembers.addAll(guild.getMembers());
        }
        for (Faction faction : factions) {
            allMembers.addAll(faction.getMembers());
        }
        return allMembers;
    }

    public List<String> getFormattedList() {
        List<String> formattedList = new ArrayList<>();
        for (String member : members) {
            formattedList.add(StringFormatter.formatHex(member + " #77d1a3(Member)"));
        }
        for (Guild guild : guilds) {
            formattedList.add(StringFormatter.formatHex(guild.getName() + " #d1b83b(Guild)"));
        }
        for (Faction faction : factions) {
            formattedList.add(StringFormatter.formatHex(faction.getName() + " #77a6d1(Faction)"));
        }
        return formattedList;
    }

    public void remove(String pool, String member) {
        switch(pool) {
            case "members":
                members.remove(member);
                break;
            case "guilds":
                guilds.removeIf(g -> g.getId().equals(member));
                break;
            case "factions":
                factions.removeIf(f -> f.getId().equals(member));
                break;
        }
    }
}
