package me.Plugins.SimpleFactions.government.movement.cause;

import java.util.ArrayList;
import java.util.List;
import me.Plugins.SimpleFactions.government.movement.Movement;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.enums.Member;
import me.Plugins.SimpleFactions.enums.Stance;
import me.Plugins.SimpleFactions.government.movement.Action;
import me.Plugins.SimpleFactions.government.movement.Pool;
import me.Plugins.SimpleFactions.government.proposal.Proposal;

public class Cause {
    private Movement movement;
    private Action action;
    private Proposal proposal;

    private String leader;
    
    private Pool pool = new Pool();

    public Cause(Movement movement, Proposal proposal, String leader) {
        this.movement = movement;
        this.proposal = proposal;
        action = proposal.getPoliticalAction().getAction();
        this.leader = leader;
        Member relation = movement.getFaction().getRelationToFaction(leader);
        if (relation == Member.GUILD_LEADER || relation == Member.GUILD_MEMBER) {
            pool.addGuild(FactionManager.getGuildByMember(leader));
        } else if (relation == Member.VASSAL_LEADER || relation == Member.VASSAL_MEMBER) {
            pool.addFaction(FactionManager.getByMember(leader));
        } else if (relation == Member.MEMBER) {
            pool.addCitizen(leader);
        } else {
            this.leader = null;
        }
    }

    public void tick() {
        checkMembers(pool);
        if(hasLeader() && !canBeLeader(leader)) {
            leader = null;
        }
        if(isEmpty()) {
            remove();
        }
    }

    public boolean isEmpty() {
        return pool.getAllMembers().isEmpty();
    }

    public void remove() {
        movement.removeCause(this);
    }

    public void join(Object o) {
        if(!canJoin(o)) return;
        if (o instanceof Guild guild) {
            pool.addGuild(guild);
        } else if (o instanceof Faction faction) {
            pool.addFaction(faction);
        } else if (o instanceof String citizen) {
            pool.addCitizen(citizen);
        }
    }

    public void leave(Object o) {
        if (o instanceof Guild guild) {
            pool.removeGuild(guild);
        } else if (o instanceof Faction faction) {
            pool.removeFaction(faction);
        } else if (o instanceof String citizen) {
            pool.removeCitizen(citizen);
        }
    }

    public boolean canBeLeader(String p) {
        if(!pool.getAllMembers().contains(p)) return false;
        Member relation = movement.getFaction().getRelationToFaction(p);
        switch(relation) {
            case LEADER:
            case FOREIGNER:
            case VASSAL_MEMBER:
                return false;
            case GUILD_LEADER:
            case GUILD_MEMBER:
                if(!proposal.getPoliticalAction().allowGuilds()) return false;
            case VASSAL_LEADER:
                if(!proposal.getPoliticalAction().allowFactions()) return false;
            case MEMBER:
                if(!proposal.getPoliticalAction().allowCitizens()) return false;
                return true;
            default:
                break;
        }
        return false;
    }

    public boolean canJoin(Object obj) {

        if (obj instanceof String) {
            return canMemberJoin((String) obj, true);
        }

        if (obj instanceof Guild) {
            return canGuildJoin((Guild) obj, true);
        }

        if (obj instanceof Faction) {
            return canFactionJoin((Faction) obj, true);
        }

        return false;
    }

    public void checkMembers(Pool pool) {

        for (String citizen : new ArrayList<>(pool.getCitizens())) {
            if (!canMemberJoin(citizen, false)) {
                pool.remove("citizens", citizen);
            }
        }

        for (Guild guild : new ArrayList<>(pool.getGuilds())) {
            if (!canGuildJoin(guild, false)) {
                pool.remove("guild", guild.getName());
            }
        }

        for (Faction faction : new ArrayList<>(pool.getFactions())) {
            if (!canFactionJoin(faction, false)) {
                pool.remove("faction", faction.getName());
            }
        }
    }

    public boolean canMemberJoin(String playerName, boolean checkExisting) {
        Member relation = movement.getFaction().getRelationToFaction(playerName);

        if (relation != Member.MEMBER) return false;
        if (!proposal.getPoliticalAction().allowCitizens()) return false;
        if (checkExisting && movement.isMember(playerName)) return false;

        return true;
    }

    public boolean canGuildJoin(Guild guild, boolean checkExisting) {
        if (!proposal.getPoliticalAction().allowGuilds()) return false;
        if (guild.isBase()) return false;
        if (!guild.getFaction().getId().equalsIgnoreCase(movement.getFaction().getId())) return false;
        if (guild.getStance(movement.getFaction()) == Stance.SUPPORT) return false;
        if (!guild.canBeElevated(null) && proposal.getPoliticalAction().getAction() == Action.NATIONHOOD) return false;
        if (checkExisting && movement.isMember(guild.getLeader())) return false;

        return true;
    }

    public boolean canFactionJoin(Faction faction, boolean checkExisting) {
        if (!proposal.getPoliticalAction().allowFactions()) return false;
        if (faction.getId().equalsIgnoreCase(movement.getFaction().getId())) return false;

        if (faction.getOverlord() == null ||
            !faction.getOverlord().getId().equalsIgnoreCase(movement.getFaction().getId()))
            return false;

        if (faction.getOrCreateMainGuild().getStance(movement.getFaction()) == Stance.SUPPORT)
            return false;

        if (checkExisting && movement.isMember(faction.getLeader())) return false;

        return true;
    }

    public int getIndex() {
        return movement.getCauses().indexOf(this);
    }

    public Movement getMovement() {
        return movement;
    }

    public Action getAction() {
        return action;
    }

    public Proposal getProposal() {
        return proposal;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public String getLeader() {
        return leader;
    }

    public boolean hasLeader() {
        return leader != null;
    }

    public Pool getPool() {
        return pool;
    }

    public List<String> getFullMemberList() {
        return pool.getAllMembers();
    }

    public void setPool(Pool pool) {
        this.pool = pool;
    }

    public List<String> getCitizenList() {
        return pool.getCitizens();
    }
}
