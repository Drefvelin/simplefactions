package me.Plugins.SimpleFactions.government.election;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.government.Government;

public class Election {
    private Government gov;

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
    }

    public boolean canBeCandidate(Candidate type, String player) {
        switch (type) {
            case LEADER:
                if(candidates.get(Candidate.LEADER).contains(player)) return false;
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
}
