package me.Plugins.SimpleFactions.government.election;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.government.Government;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class Election {
    private Government gov;
    private boolean active;

    private int notify = 0;

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
            lore.add(StringFormatter.formatHex("§cYou cannot apply for this position:"));
            if(active) {
                lore.add(StringFormatter.formatHex("§7- #ba7872Election is in the voting phase"));
            }
            if(type == Candidate.LEADER) {
                if(!gov.getFaction().isMember(p.getName())) {
                    lore.add(StringFormatter.formatHex("§7- #ba7872You must be a member of the faction to be Leader"));
                }
                if(otherCandidateExists(Candidate.LEADER, p.getName())) {
                    lore.add(StringFormatter.formatHex("§7- #ba7872Your guild already has a Leader candidate"));
                }
            } else if(type == Candidate.COUNCIL) {
                if(otherCandidateExists(Candidate.COUNCIL, p.getName())) {
                    lore.add(StringFormatter.formatHex("§7- #ba7872Your guild already has a Council candidate"));
                }
            }
        }

    }

    public boolean canBeCandidate(Candidate type, String player) {
        return canBeCandidate(type, player, true);
    }

    public boolean canBeCandidate(Candidate type, String player, boolean includeAlreadySignedUp) {
        if(active && includeAlreadySignedUp) return false;
        switch (type) {
            case LEADER:
                if(candidates.get(Candidate.LEADER).contains(player) && includeAlreadySignedUp) return false;
                if(!gov.getFaction().isMember(player)) return false;
                if(otherCandidateExists(Candidate.LEADER, player)) return false;
                break;
            case COUNCIL:
                if(candidates.get(Candidate.COUNCIL).contains(player) && includeAlreadySignedUp) return false;
                if(otherCandidateExists(Candidate.COUNCIL, player)) return false;
                break;
            default:
                return true;
        }
        return true;
    }

    public void tick() {
        if(active) {
            if(notify == 300) {
                for(Player p : Bukkit.getOnlinePlayers()) {
                    if(gov.getFaction().canVote(p) && !hasVoted(p.getName()) && gov.getVotingBooths().size() > 0) {
                        p.sendMessage("§e=================================");
                        p.sendMessage("§a§lActive Election! §7Head to a §e§lVoting Booth §7to vote!");
                        p.sendMessage("§eBooths:");
                        int i = 1;
                        for(Location loc : gov.getVotingBooths()) {
                            p.sendMessage("§7"+i+". §e"+(int)loc.getX()+"§fx §e"+(int)loc.getY()+"§fy §e"+(int)loc.getZ()+"§fz");
                            i++;
                        }
                        p.sendMessage("§e=================================");
                    }
                }
                notify = 0;
            } else {
                notify++;
            }
        }
        for(Candidate c : Candidate.values()) {
            for(String candidate : candidates.get(c)) {
                if(!canBeCandidate(c, candidate, false)) {
                    removeCandidate(c, candidate);
                }
            }
        }
    }

    public void removeCandidate(Candidate c, String player) {
        candidates.get(c).remove(player);
        votes.get(c).remove(player);
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

    public void addVote(Candidate type, String voter, String candidate) {
        votes.get(type).put(voter, candidate);
    }

    public boolean hasVoted(String voter) {
        for(Candidate c : Candidate.values()) {
            if(hasVoted(c, voter)) return true;
        }
        return false;
    }

    public boolean hasVoted(Candidate type, String voter) {
        return votes.get(type).containsKey(voter);
    }

    public String getVote(Candidate type, String voter) {
        return votes.get(type).get(voter);
    }
}
