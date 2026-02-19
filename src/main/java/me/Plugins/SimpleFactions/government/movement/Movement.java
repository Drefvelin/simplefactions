package me.Plugins.SimpleFactions.government.movement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.Database.CauseData;
import me.Plugins.SimpleFactions.Database.MovementData;
import me.Plugins.SimpleFactions.Database.PoolData;
import me.Plugins.SimpleFactions.Database.ProposalData;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.enums.Member;
import me.Plugins.SimpleFactions.enums.Stance;
import me.Plugins.SimpleFactions.government.movement.cause.Cause;
import me.Plugins.SimpleFactions.government.proposal.Proposal;
import me.Plugins.SimpleFactions.laws.Law;
import me.Plugins.SimpleFactions.laws.LawGroup;

public class Movement {
    private Faction f;

    private String leader;

    private double organization;

    private List<Cause> causes = new ArrayList<>();

    private Pool supporters = new Pool();
    
    private List<Faction> foreignBackers = new ArrayList<>();

    public Movement(Faction f, String leader, Proposal cause) {
        this.f = f;
        this.leader = leader;
        addCause(new Cause(this, cause, leader));
    }

    public Movement(Faction f, MovementData data) {
        this.f = f;
        this.leader = data.leader;
        this.organization = data.organization != null ? data.organization : 0;
        this.causes = new ArrayList<>();
        this.supporters = new Pool();
        this.foreignBackers = new ArrayList<>();
        
        // Deserialize all causes
        if (data.causes != null) {
            for (CauseData causeData : data.causes) {
                try {
                    Proposal proposal = deserializeProposal(f, causeData.proposal);
                    if (proposal == null) continue;
                    
                    Cause cause = new Cause(this, proposal, causeData.leader);
                    
                    // Restore members pool if available
                    if (causeData.members != null) {
                        cause.setMembers(deserializePool(f, causeData.members));
                    }
                    
                    causes.add(cause);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        // Deserialize supporters pool
        if (data.supporters != null) {
            supporters = deserializePool(f, data.supporters);
        }
        
        // Deserialize foreign backers
        if (data.foreignBackers != null) {
            for (String backerId : data.foreignBackers) {
                Faction backer = FactionManager.getByString(backerId);
                if (backer != null) {
                    foreignBackers.add(backer);
                }
            }
        }
    }

    public Faction getFaction() {
        return f;
    }

    public void tick() {
        for (Cause cause : causes) {
            cause.tick();
            checkMembers(supporters, cause.getProposal(), false);
            checkForeignBackers(cause.getProposal());
        }
    }

    public boolean canMemberJoin(String playerName, boolean asMember, Proposal proposal) {
        Member relation = f.getRelationToFaction(playerName);

        if (relation != Member.MEMBER) return false;
        if (asMember && !proposal.getPoliticalAction().allowMembers()) return false;

        return true;
    }

    public boolean canGuildJoin(Guild guild, boolean asMember, Proposal proposal) {
        if (asMember && !proposal.getPoliticalAction().allowGuilds()) return false;
        if (guild.isBase()) return false;
        if (!guild.getFaction().getId().equalsIgnoreCase(f.getId())) return false;
        if (guild.getStance(f) == Stance.SUPPORT) return false;
        if (!guild.canBeElevated(null) && proposal.getPoliticalAction().getAction() == Action.NATIONHOOD) return false;

        return true;
    }

    public boolean canFactionJoin(Faction faction, boolean asMember, Proposal proposal) {
        if (asMember && !proposal.getPoliticalAction().allowFactions()) return false;
        if (faction.getId().equalsIgnoreCase(f.getId())) return false;

        if (faction.getOverlord() == null ||
            !faction.getOverlord().getId().equalsIgnoreCase(f.getId()))
            return false;

        if (faction.getOrCreateMainGuild().getStance(f) == Stance.SUPPORT)
            return false;

        return true;
    }

    public boolean canForeignBackerJoin(Faction faction) {
        if (RelationManager.sameRealm(faction, f)) return false;

        if (faction.getRelation(f.getId()).getType().getId()
            .equalsIgnoreCase("ally"))
            return false;

        if (faction.getId().equalsIgnoreCase(f.getId()))
            return false;

        return true;
    }

    public void checkMembers(Pool pool, Proposal proposal, boolean asMember) {

        for (String member : new ArrayList<>(pool.getMembers())) {
            if (!canMemberJoin(member, asMember, proposal)) {
                pool.remove("member", member);
            }
        }

        for (Guild guild : new ArrayList<>(pool.getGuilds())) {
            if (!canGuildJoin(guild, asMember, proposal)) {
                pool.remove("guild", guild.getName());
            }
        }

        for (Faction faction : new ArrayList<>(pool.getFactions())) {
            if (!canFactionJoin(faction, asMember, proposal)) {
                pool.remove("faction", faction.getName());
            }
        }
    }

    public void checkForeignBackers(Proposal proposal) {
        foreignBackers.removeIf(faction -> !canForeignBackerJoin(faction));
    }

    public boolean canJoin(Object obj, boolean asMember, Proposal proposal) {

        if (obj instanceof String) {
            return canMemberJoin((String) obj, asMember, proposal);
        }

        if (obj instanceof Guild) {
            return canGuildJoin((Guild) obj, asMember, proposal);
        }

        if (obj instanceof Faction) {
            return canFactionJoin((Faction) obj, asMember, proposal);
        }

        return false;
    }

    public String getLeader() {
        return leader;
    }

    public double getOrganization() {
        return organization;
    }

    public List<Cause> getCauses() {
        return causes;
    }

    public Cause getCauseByLeader(String leaderName) {
        for (Cause cause : causes) {
            if (cause.getLeader().equalsIgnoreCase(leaderName)) {
                return cause;
            }
        }
        return null;
    }

    public Pool getSupporters() {
        return supporters;
    }

    public List<String> getAllMembers() {
        List<String> members = new ArrayList<>();
        for (Cause cause : causes) {
            members.addAll(cause.getMembersList());
        }
        members.addAll(supporters.getAllMembers());
        return members;
    }

    public List<Faction> getForeignBackers() {
        return foreignBackers;
    }

    public void addCause(Cause cause) {
        causes.add(cause);
    }

    private Proposal deserializeProposal(Faction faction, ProposalData proposalData) {
        if (proposalData == null) return null;
        
        Proposal p = new Proposal(proposalData.proposer, faction.getGovernment());
        
        try {
            if ("law".equals(proposalData.type)) {
                LawGroup group = faction.getLawHandler().getGroup(proposalData.groupId);
                if (group != null) {
                    Law law = group.getLaw(proposalData.lawId);
                    if (law != null) {
                        p.setLawProposal(law);
                        return p;
                    }
                }
            } else if ("tax".equals(proposalData.type)) {
                me.Plugins.SimpleFactions.government.proposal.TaxTarget target = 
                    me.Plugins.SimpleFactions.government.proposal.TaxTarget.valueOf(proposalData.taxTarget);
                me.Plugins.SimpleFactions.government.proposal.TaxLawChange tax = 
                    new me.Plugins.SimpleFactions.government.proposal.TaxLawChange(target, proposalData.taxId, proposalData.newTax);
                p.setTaxProposal(tax);
                return p;
            } else if ("political".equals(proposalData.type)) {
                Action action = Action.valueOf(proposalData.actionKey);
                PoliticalAction politicalAction = new PoliticalAction(action);
                p.setPoliticalActionProposal(politicalAction);
                p.setTarget(proposalData.target);
                return p;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }

    private Pool deserializePool(Faction faction, PoolData poolData) {
        Pool pool = new Pool();
        
        if (poolData.members != null) {
            for (String member : poolData.members) {
                pool.addMember(member);
            }
        }
        
        if (poolData.guilds != null) {
            for (String guildId : poolData.guilds) {
                Guild guild = faction.getGuildHandler().getGuild(guildId);
                if (guild != null) {
                    pool.addGuild(guild);
                }
            }
        }
        
        if (poolData.factions != null) {
            for (String factionId : poolData.factions) {
                Faction f = FactionManager.getByString(factionId);
                if (f != null) {
                    pool.addFaction(f);
                }
            }
        }
        
        return pool;
    }
}
