package me.Plugins.SimpleFactions.government;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.Utils.Wealth;
import me.Plugins.SimpleFactions.enums.FactionModifiers;
import me.Plugins.SimpleFactions.enums.GuildModifier;
import me.Plugins.SimpleFactions.enums.Rules;
import me.Plugins.SimpleFactions.government.election.Candidate;
import me.Plugins.SimpleFactions.government.election.Election;
import me.Plugins.SimpleFactions.government.proposal.Proposal;
import me.Plugins.SimpleFactions.laws.LawGroup;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import me.Plugins.SimpleFactions.Managers.RelationManager;

public class Government {
    private Faction f;
    private Council council;
    private double power;

    private final Election election;
    private List<Location> votingBooths = new ArrayList<>();

    private Date lastElectionDate = new Date(0);

    public Government(Faction f) {
        this.f = f;
        this.council = new Council(this, f);
        this.power = -1;
        this.election = new Election(this, false);
    }

    public Government(Faction f, me.Plugins.SimpleFactions.Database.GovernmentData data) {
        this.f = f;
        this.council = new Council(this, f);
        this.power = data.power != null ? data.power : -1;
        this.lastElectionDate = data.lastElectionDate != null ? new java.util.Date(data.lastElectionDate) : new java.util.Date(0);
        boolean electionActive = false;

        // If an election was started less than 7 days ago, it is still active
        if (data.lastElectionDate != null) {
            LocalDate start = Instant.ofEpochMilli(data.lastElectionDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            electionActive = ChronoUnit.DAYS.between(start, LocalDate.now()) < 7;
        }

        this.election = new Election(this, electionActive);
        // Restore election candidates and votes
        if (data.electionCandidates != null || data.electionVotes != null || data.previousVotes != null) {
            election.restoreFromData(data.electionCandidates, data.electionVotes, data.previousVotes);
        }
        
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

        if (shouldStartElection()) {
            election.start();
        }

        if (election.isActive()) {
            LocalDate today = LocalDate.now();
            LocalDate end = getElectionEndDate();

            if (end != null && !today.isBefore(end)) {
                election.end();
            }
        }
    }

    // Government
    public void cancelElections(Candidate type) {
        if (!election.isActive()) return;
        election.cancel(type);
    }

    public void cancelAllElections() {
        if (!election.isActive()) return;
        election.cancelAll();
    }

    public void setLastElectionDate() {
        lastElectionDate = new Date();
    }

    public void applyElectionResults() {
        // Leader
        if (hasLeaderElections()) {
            election.getWinners(Candidate.LEADER).stream()
                    .filter(f::canBecomeLeader)
                    .findFirst()
                    .ifPresent(f::promoteToLeader);
        }

        // Council
        if (hasCouncilElections() && council.getType() == Rules.ELECTED_COUNCIL) {
            int maxSize = council.getMaxSize();

            List<String> oldCouncil = new ArrayList<>(council.getMembers());
            List<String> winners = election.getWinners(Candidate.COUNCIL);

            council.clearMembers();

            for (String name : winners) {
                if (council.getCurrentSize() >= maxSize) break;
                if (council.canBeMember(name, true)) {
                    council.addMemberForce(name);
                }
            }

            for (String name : oldCouncil) {
                if (council.getCurrentSize() >= maxSize) break;
                if (council.canBeMember(name, true)) {
                    council.addMemberForce(name);
                }
            }
        }
        replace();
    }


    public void tick() {
        election.tick();
        if(!hasElections()) lastElectionDate = new Date(0);
        replace();
    }

    public void powerTick() {
        power += getPowerGain();
        double maxPower = getMaxPower();
        if (power > maxPower) power = maxPower;
    }

    public void replace() {
        replaceLeader();
        council.replace();
    }


    private void replaceLeader() {
        if (!f.canRemainLeader(f.getLeader())) {
            boolean promoted =
                election.getWinners(Candidate.LEADER).stream()
                    .filter(f::canBecomeLeader)
                    .findFirst()
                    .map(name -> {
                        f.promoteToLeader(name);
                        return true;
                    })
                    .orElse(false);

            if (!promoted) {
                f.getMembers().stream()
                    .filter(f::canBecomeLeader)
                    .findFirst()
                    .ifPresent(f::promoteToLeader);
            }
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

    public double getTotalUpkeep() {
        double total = 0;
        for(LawGroup group : f.getLawHandler().getGroupList()) {
            total += group.getCurrent().getUpkeep();
        }
        total *= 3-getStability()/50.0;
        return total;
    }

    public double getTaxEfficiency() {
        return getStability()/100.0;
    }

    public double getBaseMaxPower() {
        double base = f.getMembers().size() * 10;
        base += f.getOrCreateMainGuild().getModifier(GuildModifier.ADMIN_POWER);
        base *= 1+f.getModifier(FactionModifiers.ADMIN_POWER_MULTIPLIER).getAmount()/100.0;
        base *= getStability()/100.0;
        return base;
    }

    public double getMaxPower() {
        double max = getBaseMaxPower();
        max -= getTotalUpkeep();
        return max;
    }

    public double getPower() {
        if(power > getMaxPower()) {
            power = getMaxPower();
        }
        return Formatter.formatDouble(power);
    }

    public void spendPower(double amount) {
        power -= amount;
        if(power < 0) power = 0;
    }

    public double getPowerGain() {
        double base = 1;
        base += f.getOrCreateMainGuild().getModifier(GuildModifier.ADMIN_POWER_GAIN);
        base *= 1+f.getModifier(FactionModifiers.ADMIN_POWER_GAIN_MULTIPLIER).getAmount()/100.0;
        base *= getStability()/100.0;
        return base;
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

    public double getBaseStability() {
        return 100.0/(f.getMembers().size()+f.getVassalMembers().size());
    }

    public double getStability() {
        double stability = getBaseStability();
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

    //Elections
    public LocalDate getLastElectionStartDate() {
        return Instant.ofEpochMilli(lastElectionDate.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public LocalDate getNextElectionStartDate() {
        LocalDate lastStart = getLastElectionStartDate();

        // Last election ends after 7 days, then wait 21 days
        LocalDate earliestAllowed = lastStart.plusDays(7 + 21);

        LocalDate today = LocalDate.now();

        // Choose the later of (earliestAllowed, today)
        LocalDate base = earliestAllowed.isAfter(today) ? earliestAllowed : today;

        // Jump directly to the closest Monday forward (including today if Monday)
        return base.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    public boolean hasElections() {
        return hasLeaderElections() || hasCouncilElections();
    }

    public String getTimeUntilNextElection() {
        // If elections are disabled
        if (!hasElections()) {
            return "N/A";
        }

        LocalDate nextDate = getNextElectionStartDate();

        Instant now = Instant.now();
        Instant next = nextDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        if (next.isBefore(now)) {
            return "0d 0h";
        }

        long seconds = ChronoUnit.SECONDS.between(now, next);

        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;

        return days + "d " + hours + "h";
    }

    public String getLastElectionString() {
        if (lastElectionDate == null || lastElectionDate.getTime() == 0) {
            return "Never";
        }

        LocalDate date = getLastElectionStartDate();

        int day = date.getDayOfMonth();
        int month = date.getMonthValue();

        long timestamp = date
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        String year = Cache.getFantasyYear(timestamp);

        return String.format("%02d/%02d %s", day, month, year);
    }


    public LocalDate getElectionEndDate() {
        // If election never started
        if (lastElectionDate == null || lastElectionDate.getTime() == 0) {
            return null;
        }

        // Election lasts exactly 7 days
        return getLastElectionStartDate().plusDays(7);
    }

    public String getTimeUntilElectionEnds() {
        if (!hasElection()) {
            return "N/A";
        }

        LocalDate endDate = getElectionEndDate();
        if(endDate == null) return "N/A";
        Instant end = endDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        Instant now = Instant.now();

        if (end.isBefore(now)) {
            return "0d 0h";
        }

        long seconds = ChronoUnit.SECONDS.between(now, end);
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;

        return days + "d " + hours + "h";
    }



    public boolean hasElections(Candidate type) {
        switch (type) {
            case LEADER:
                return hasLeaderElections();
            case COUNCIL:
                return hasCouncilElections();
            default:
                return false;
        }
    }

    public boolean hasElection() {
        return election.isActive();
    }

    public boolean shouldStartElection() {
        if(hasElection()) return false;
        if(!election.hasAnyCandidates()) return false;
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

    public List<Location> getVotingBooths() {
        return votingBooths;
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
        data.electionCandidates = election.serializeCandidates();
        data.electionVotes = election.serializeVotes();
        data.previousVotes = election.serializePreviousVotes();
        return data;
    }
}
