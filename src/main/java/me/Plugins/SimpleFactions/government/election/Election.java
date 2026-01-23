package me.Plugins.SimpleFactions.government.election;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.government.Government;

public class Election {
    private Government gov;
    private boolean active;

    private Map<Candidate, List<String>> candidates = new HashMap<>() {{
        put(Candidate.LEADER, new ArrayList<>());
        put(Candidate.COUNCIL, new ArrayList<>());
    }};

    private Map<Candidate, Map<String, String>> votes = new HashMap<>() {{
        put(Candidate.LEADER, new HashMap<>());
        put(Candidate.COUNCIL, new HashMap<>());
    }}; //type -> (voter -> candidate)

    public Election(Government gov) {
        this.gov = gov;
        this.active = false;
    }

    public void start() {
        this.active = true;
    }

    public boolean isActive() {
        return active;
    }

    public void addCandidate(Candidate c, String player) {
        candidates.get(c).add(player);
    }

    public boolean isCandiate(Candidate c, String player) {
        return candidates.get(c).contains(player);
    }

    public void applyReasons(List<String> lore, Candidate type, Player p) {
        if(isCandiate(type, p.getName())) return;
        if(!canBeCandidate(type, p.getName())) {
            lore.add("§cYou cannot apply for this position:");
            if(active) {
                lore.add("§7- #ba7872Election is in the voting phase");
            }
            if(type == Candidate.LEADER) {
                if(!gov.getFaction().isMember(p.getName())) {
                    lore.add("§7- #ba7872You must be a member of the faction to be Leader");
                }
                if(otherCandidateExists(Candidate.LEADER, p.getName())) {
                    lore.add("§7- #ba7872Your guild already has a Leader candidate");
                }
            } else if(type == Candidate.COUNCIL) {
                if(otherCandidateExists(Candidate.COUNCIL, p.getName())) {
                    lore.add("§7- #ba7872Your guild already has a Council candidate");
                }
            }
        }

    }

    public boolean canBeCandidate(Candidate type, String player) {
        if(active) return false;
        switch (type) {
            case LEADER:
                if(candidates.get(Candidate.LEADER).contains(player)) return false;
                if(!gov.getFaction().isMember(player)) return false;
                if(otherCandidateExists(Candidate.LEADER, player)) return false;
                break;
            case COUNCIL:
                if(candidates.get(Candidate.COUNCIL).contains(player)) return false;
                if(otherCandidateExists(Candidate.COUNCIL, player)) return false;
                break;
            default:
                return false;
        }
        return true;
    }

    public boolean otherCandidateExists(Candidate type, String player) {
        Guild g = gov.getFaction().getGuildHandler().getGuildByMember(player);
        if(g == null) return false;
        if(g.isBase()) return false;
        for(String candidate : candidates.get(type)) {
            Guild cg = gov.getFaction().getGuildHandler().getGuildByMember(candidate);
            if(cg == null) continue;
            if(cg.isBase()) continue;
            if(cg.getId().equalsIgnoreCase(g.getId())) return  true;
        }
        return false;
    }

    public List<String> getCandidates(Candidate type) {
        return candidates.get(type);
    }
}
