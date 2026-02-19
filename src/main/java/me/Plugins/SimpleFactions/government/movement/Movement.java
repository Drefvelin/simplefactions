package me.Plugins.SimpleFactions.government.movement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.enums.Member;
import me.Plugins.SimpleFactions.enums.Stance;
import me.Plugins.SimpleFactions.government.movement.cause.Cause;
import me.Plugins.SimpleFactions.government.proposal.Proposal;

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
}
