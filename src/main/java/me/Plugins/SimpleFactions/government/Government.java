package me.Plugins.SimpleFactions.government;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.enums.Rules;
import me.Plugins.SimpleFactions.government.election.Election;
import me.Plugins.SimpleFactions.government.proposal.Proposal;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import me.Plugins.SimpleFactions.Managers.RelationManager;

public class Government {
    private Faction f;
    private Council council;
    private double power;
    private double powerGain;

    private Election election;
    private List<Location> votingBooths = new ArrayList<>();

    private Date lastElectionDate = new Date(0);

    public final double STABILITY_BASE = 25.0;

    public Government(Faction f) {
        this.f = f;
        this.council = new Council(this, f);
        this.power = -1;
        this.powerGain = -1;
    }

    public Government(Faction f, me.Plugins.SimpleFactions.Database.GovernmentData data) {
        this.f = f;
        this.council = new Council(this, f);
        this.power = data.power != null ? data.power : -1;
        this.powerGain = -1;
        this.lastElectionDate = data.lastElectionDate != null ? new java.util.Date(data.lastElectionDate) : new java.util.Date(0);
        
        // Restore council members
        if (data.councilMembers != null) {
            for (String member : data.councilMembers) {
                council.addMember(member);
            }
        }
        
        // Restore proposals
        if (data.proposals != null) {
            council.getProposalHandler().restoreProposals(f, data.proposals);
        }
    }

    public void ping() {
        council.reorganize();

        if (power == -1) power = f.getMembers().size() * 10;
        if (powerGain == -1) powerGain = 1;

        if (election == null && shouldStartElection()) {
            election = new Election(this);
            lastElectionDate = new Date(); // store START date
        }
    }


    public boolean isCouncilMember(Player p) {
        String name = p.getName();
        return council.isMember(name) || f.getLeader().equalsIgnoreCase(name);
    }

    public boolean canProposeOrStartMovement(Player p) {
        String name = p.getName();
        if(isCouncilMember(p)) {
            return council.getProposalHandler().canPropose(name);
        }
        Guild guild = FactionManager.getGuildByMember(name);
        if(guild != null) {
            if(guild.getFaction().getId().equalsIgnoreCase(f.getId())) {
                if(guild.isBase()) return true; //Member of the base guild in the faction
                if(guild.getLeader().equalsIgnoreCase(name))return true; //Guild leader of a guild in the faction
            }
        }
        return false;
    }

    public Faction getFaction() {
        return f;
    }

    public boolean canPropose(Player p) {
        String name = p.getName();
        if(name.equalsIgnoreCase(f.getLeader())) return true;
        if(council.canPropose(name)) return true;
        return false;
    }

    public void propose(Proposal proposal) {
        council.getProposalHandler().propose(proposal);
    }

    public boolean canBeProposed(Proposal proposal) {
        return council.canBeProposed(proposal);
    }

    public Council getCouncil() {
        return council;
    }

    public boolean hasCouncil() {
        return !f.getCouncilType().equals(Rules.NO_COUNCIL);
    }

    public List<String> getCouncilMembers() {
        return council.getMembers();
    }

    public boolean hasLeaderElections() {
        return f.hasFactionRule(Rules.LEADER_ELECTIONS);
    }

    public boolean hasCouncilElections() {
        return f.hasFactionRule(Rules.ELECTED_COUNCIL);
    }
    public double getPower() {
        return power;
    }

    public double getPowerGain() {
        return powerGain;
    }

    public boolean canAffectStability(Guild guild) {
        if(guild.getFaction().getId().equalsIgnoreCase(f.getId()) && !guild.isBase()) return true;
        for(Faction vassal : RelationManager.getSubjects(f)) {
            if(vassal.getId().equalsIgnoreCase(guild.getId())) {
                return true;
            }
        }
        return false;
    }

    public double getStabilityMalusFromCouncil() {
        if(council.couldBeBigger()) {
            double fillPercentage = council.fillPercentage();
            return (1.0 - fillPercentage) * 50.0;
        }
        return 0;
    }

    public double getStability() {
        double stability = STABILITY_BASE;
        for(Guild guild : f.getGuildHandler().getGuilds()) {
            stability += guild.getStabilityModifier(f);
        }
        for(Faction vassal : RelationManager.getSubjects(f)) {
            stability += vassal.getOrCreateMainGuild().getStabilityModifier(f);
        }
        if(council.couldBeBigger()) {
            stability -= getStabilityMalusFromCouncil();
        }
        if(stability < 0) stability = 0;
        if(stability > 100) stability = 100;
        return Formatter.formatDouble(stability);
    }

    public double getMaxPower() {
        double base = f.getMembers().size() * 10;
        base *= 1+(council.getCurrentSize())/2.0;
        base *= getStability()/100.0;
        return base;
    }

    public String getStabilityString() {
        // Clamp stability just in case
        double s = Math.max(0, Math.min(100, getStability()));
        double t = s / 100.0;

        // Dark red → bright green
        int startR = 139, startG = 0,   startB = 0;
        int endR   = 0,   endG   = 255, endB   = 0;

        int r = (int) Math.round(startR + (endR - startR) * t);
        int g = (int) Math.round(startG + (endG - startG) * t);
        int b = (int) Math.round(startB + (endB - startB) * t);

        return StringFormatter.formatHex(String.format("#%02X%02X%02X"+getStability(), r, g, b));
    }

    public LocalDate getLastElectionStartDate() {
        return Instant.ofEpochMilli(lastElectionDate.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    //election
    public boolean hasElection() {
        return election != null;
    }

    public boolean shouldStartElection() {
        LocalDate today = LocalDate.now();
        
        // 1. Must be Monday
        if (today.getDayOfWeek() != DayOfWeek.MONDAY) {
            return false;
        }

        // 2. Calculate last election end
        LocalDate lastElectionStart = getLastElectionStartDate();
        LocalDate lastElectionEnd = lastElectionStart.plusDays(7);

        // 3. Days since last election ended
        long daysSinceEnd = ChronoUnit.DAYS.between(lastElectionEnd, today);

        return daysSinceEnd >= 21;
    }

    public void addVotingBooth(Location loc) {
        votingBooths.add(loc);
    }

    public void removeVotingBooth(Location loc) {
        votingBooths.remove(loc);
    }

    public boolean isVotingBooth(Location loc) {
        for(Location l : votingBooths) {
            if(l.equals(loc)) return true;
        }
        return false;
    }

    public Election getElection() {
        return election;
    }

    public me.Plugins.SimpleFactions.Database.GovernmentData serialize() {
        me.Plugins.SimpleFactions.Database.GovernmentData data = new me.Plugins.SimpleFactions.Database.GovernmentData();
        data.power = this.power >= 0 ? this.power : null;
        data.lastElectionDate = this.lastElectionDate.getTime();
        data.councilMembers = new java.util.ArrayList<>(council.getMembers());
        data.proposals = council.getProposalHandler().serializeProposals();
        return data;
    }
}
