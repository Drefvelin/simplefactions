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
import org.checkerframework.checker.units.qual.t;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Database.CauseData;
import me.Plugins.SimpleFactions.Database.GovernmentData;
import me.Plugins.SimpleFactions.Database.MovementData;
import me.Plugins.SimpleFactions.Database.PoolData;
import me.Plugins.SimpleFactions.Database.ProposalData;
import me.Plugins.SimpleFactions.Database.StabilityModifierData;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Loaders.PoliticalActionLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.Utils.Wealth;
import me.Plugins.SimpleFactions.enums.FactionModifiers;
import me.Plugins.SimpleFactions.enums.GuildModifier;
import me.Plugins.SimpleFactions.enums.Member;
import me.Plugins.SimpleFactions.enums.Rules;
import me.Plugins.SimpleFactions.government.election.Candidate;
import me.Plugins.SimpleFactions.government.election.Election;
import me.Plugins.SimpleFactions.government.movement.Action;
import me.Plugins.SimpleFactions.government.movement.Movement;
import me.Plugins.SimpleFactions.government.movement.PoliticalAction;
import me.Plugins.SimpleFactions.government.movement.Pool;
import me.Plugins.SimpleFactions.government.movement.cause.Cause;
import me.Plugins.SimpleFactions.government.proposal.Proposal;
import me.Plugins.SimpleFactions.laws.LawGroup;
import me.Plugins.SimpleFactions.laws.Law;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import me.Plugins.SimpleFactions.Managers.RelationManager;

public class Government {
    private Faction f;
    private Council council;
    private double power;
    private List<StabilityModifier> stabilityModifiers = new ArrayList<>();

    private final Election election;
    private List<Location> votingBooths = new ArrayList<>();

    private Date lastElectionDate = new Date(0);

    private List<Movement> movements = new ArrayList<>();
    private List<MovementData> movementData = new ArrayList<>();

    public Government(Faction f) {
        this.f = f;
        this.council = new Council(this, f);
        this.power = -1;
        this.election = new Election(this, false);
    }

    public Government(Faction f, GovernmentData data) {
        this.f = f;
        this.council = new Council(this, f);
        this.power = data.power != null ? data.power : -1;
        this.lastElectionDate = data.lastElectionDate != null ? new java.util.Date(data.lastElectionDate) : new java.util.Date(0);
        if (data.stabilityModifiers != null) {
            for (StabilityModifierData smd : data.stabilityModifiers) {
                stabilityModifiers.add(new StabilityModifier(smd.name, smd.modifier, smd.decay));
            }
        }
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
        if (data.electionCandidates != null || data.electionVotes != null || data.previousVotes != null || data.eligibleVoters != null) {
            election.restoreFromData(data.electionCandidates, data.electionVotes, data.previousVotes, data.eligibleVoters);
        }
        
        // Restore council members
        if (data.councilMembers != null) {
            for (String member : data.councilMembers) {
                council.addMemberForce(member);
            }
        }
        
        // Restore proposals
        if (data.proposals != null) {
            council.getProposalHandler().restoreProposals(f, data.proposals);
        }
        
        // Restore movements
        if (data.movements != null && !data.movements.isEmpty()) {
            movementData = data.movements;
        }
    }

    public void loadMovements() {
        for (MovementData md : movementData) {
            Movement movement = new Movement(f, md);
            movements.add(movement);
        }
    }

    public void ping() {
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

    //Movements

    public boolean hasMovements() {
        return !movements.isEmpty();
    }

    public List<Movement> getMovements() {
        return movements;
    }

    public void startMovement(String leader, Proposal cause) {
        Movement movement = new Movement(f, leader, cause);
        movements.add(movement);
    }

    public void endMovement(Movement movement) {
        movements.remove(movement);
    }

    public Movement getMovementByLeader(String leader) {
        for (Movement movement : movements) {
            if (movement.getLeader().equalsIgnoreCase(leader)) {
                return movement;
            }
        }
        return null;
    }

    public Movement getMovementById(String id) {
        for (Movement movement : movements) {
            if (movement.getId().equalsIgnoreCase(id)) {
                return movement;
            }
        }
        return null;
    }

    public Movement getMovementByMember(String member) {
        for (Movement movement : movements) {
            if (movement.getAllMembers().contains(member)) {
                return movement;
            }
        }
        return null;
    }

    public Movement getMovementByForeignBacker(Faction backer) {
        for (Movement movement : movements) {
            if (movement.getForeignBackers().contains(backer)) {
                return movement;
            }
        }
        return null;
    }

    public StabilityModifier getByName(String name) {
        for (StabilityModifier modifier : stabilityModifiers) {
            if (modifier.getName().equalsIgnoreCase(name)) {
                return modifier;
            }
        }
        return null;
    }

    public void addStabilityModifier(StabilityModifier modifier) {
        if(getByName(modifier.getName()) != null) {
            getByName(modifier.getName()).increaseModifier(modifier.getModifier());
            return;
        }
        stabilityModifiers.add(modifier);
    }

    public List<StabilityModifier> getStabilityModifiers() {
        return stabilityModifiers;
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
                if (council.canBeMember(name, true, false)) {
                    council.addMemberForce(name);
                }
            }

            for (String name : oldCouncil) {
                if (council.getCurrentSize() >= maxSize) break;
                if (council.canBeMember(name, true, false)) {
                    council.addMemberForce(name);
                }
            }
        }
        replace();
    }


    public void tick() {
        for(Movement movement : new ArrayList<>(movements)) {
            movement.tick();
        }
        election.tick();
        if(!hasElections()) lastElectionDate = new Date(0);
        replace();
    }

    public void powerTick() {
        for(StabilityModifier modifier : new ArrayList<>(stabilityModifiers)) {
            if(modifier.tick()) {
                stabilityModifiers.remove(modifier);
            }
        }
        double maxPower = getMaxPower();
        if (power > maxPower && getPower() > 0) return; //cant overstack but it can decline
        power += getPowerGain();
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
        Faction faction = FactionManager.getByLeader(name);
        if(faction != null) {
            if(faction.getOverlord() != null && faction.getOverlord().getId().equalsIgnoreCase(f.getId())) return true;
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

    public boolean canProposePolitical(Player p, Action action) {
        if(action == Action.NONE) return false;
        String name = p.getName();
        if(name.equalsIgnoreCase(f.getLeader())) return false;
        if(council.isMember(name)) return false;
        if(action == Action.SNAP_ELECTIONS && !hasElections()) return false;
        Member relation = f.getRelationToFaction(name);
        switch (relation) {
            case LEADER:
            case VASSAL_MEMBER:
            case FOREIGNER:
                return false;
            case MEMBER:
                if(!PoliticalActionLoader.getByAction(action).allowCitizens()) return false;
                return true;
            case GUILD_MEMBER:
                if(action == Action.NATIONHOOD) return false; //only leaders can demand that
            case GUILD_LEADER:
                if(action == Action.NATIONHOOD && !FactionManager.getGuildByMember(p.getName()).canBeElevated(null)) return false;
                if(!PoliticalActionLoader.getByAction(action).allowGuilds()) return false;
                break;
            case VASSAL_LEADER:
                if(!PoliticalActionLoader.getByAction(action).allowFactions()) return false;
            default:
                break;
            
        }
        return true;
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
        if(f.getOrCreateMainGuild().isBankrupt()) {
            stability -= 100;
        }
        if(hasElections() && votingBooths.size() == 0) {
            stability -= 75;
        }
        for(StabilityModifier modifier : stabilityModifiers) {
            stability += modifier.getModifier();
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

    private List<MovementData> serializeMovements() {
        List<MovementData> result = new ArrayList<>();
        
        for (Movement m : movements) {
            MovementData data = new MovementData();
            if(m.hasLeader()) data.leader = m.getLeader();
            data.organization = m.getOrganization();
            data.phase = m.getPhase().name();
            data.id = m.getId();
            // Serialize causes
            for (Cause cause : m.getCauses()) {
                CauseData causeData = new CauseData();
                causeData.leader = cause.getLeader();
                causeData.action = cause.getAction().toString();
                
                // Serialize proposal
                Proposal proposal = cause.getProposal();
                causeData.proposal = serializeProposal(proposal);
                
                // Serialize members pool
                Pool memberPool = cause.getPool();
                causeData.members = serializePool(memberPool);
                
                data.causes.add(causeData);
            }
            
            // Serialize supporters pool
            Pool supportersPool = m.getSupporters();
            data.supporters = serializePool(supportersPool);
            
            // Serialize foreign backers
            for (Faction backer : m.getForeignBackers()) {
                data.foreignBackers.add(backer.getId());
            }
            
            result.add(data);
        }
        
        return result;
    }

    private PoolData serializePool(Pool pool) {
        PoolData data = new PoolData();
        data.citizens = new java.util.ArrayList<>(pool.getCitizens());
        
        for (Guild guild : pool.getGuilds()) {
            data.guilds.add(guild.getId());
        }
        
        for (Faction faction : pool.getFactions()) {
            data.factions.add(faction.getId());
        }
        
        return data;
    }

    private ProposalData serializeProposal(Proposal p) {
        ProposalData data = new ProposalData();
        data.proposer = p.getProposer();
        
        if (p.isLawProposal() && p.getLaw() != null) {
            data.type = "law";
            data.groupId = p.getLaw().getGroup();
            data.lawId = p.getLaw().getId();
        } else if (p.isTaxProposal() && p.getTaxChange() != null) {
            data.type = "tax";
            me.Plugins.SimpleFactions.government.proposal.TaxLawChange tax = p.getTaxChange();
            data.taxTarget = tax.getTarget().name();
            data.taxId = tax.getId();
            data.newTax = tax.getNewTax();
        } else if (p.isPoliticalActionProposal() && p.getPoliticalAction() != null) {
            data.type = "political";
            data.actionKey = p.getPoliticalAction().getAction().toString();
            data.target = p.getTarget();
        }
        
        return data;
    }

    public void deserializeMovements(Faction faction, List<MovementData> movementDataList) {
        movements.clear();
        
        for (MovementData movementData : movementDataList) {
            try {
                Movement movement = new Movement(faction, movementData);
                movements.add(movement);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public GovernmentData serialize() {
        GovernmentData data = new GovernmentData();
        data.power = this.power >= 0 ? this.power : null;
        data.lastElectionDate = this.lastElectionDate.getTime();
        data.councilMembers = new ArrayList<>(council.getMembers());
        data.proposals = council.getProposalHandler().serializeProposals();
        data.electionCandidates = election.serializeCandidates();
        data.electionVotes = election.serializeVotes();
        data.previousVotes = election.serializePreviousVotes();
        data.eligibleVoters = new ArrayList<>(election.getStoredEligibleVoters());
        data.stabilityModifiers = new ArrayList<>();
        for (StabilityModifier modifier : stabilityModifiers) {
            StabilityModifierData modifierData = new StabilityModifierData();
            modifierData.name = modifier.getName();
            modifierData.modifier = modifier.getModifier();
            modifierData.decay = modifier.getDecay();
            data.stabilityModifiers.add(modifierData);
        }
        data.movements = serializeMovements();
        return data;
    }
}
