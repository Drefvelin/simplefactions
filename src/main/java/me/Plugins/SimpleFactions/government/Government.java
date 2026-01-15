package me.Plugins.SimpleFactions.government;

import java.util.List;

import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.enums.Rules;
import me.Plugins.SimpleFactions.government.proposal.Proposal;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class Government {
    private Faction f;
    private Council council;
    private double power;
    private double powerGain;

    public final double STABILITY_BASE = 25.0;

    public Government(Faction f) {
        this.f = f;
        this.council = new Council(this, f);
        this.power = -1;
        this.powerGain = -1;
    }

    public void ping() {
        this.council.reorganize();
        if(power == -1) this.power = f.getMembers().size() * 10;
        if(powerGain == -1) this.powerGain = 1;
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

    public double getStability() {
        double stability = STABILITY_BASE;
        if(f.getGuildHandler().getGuilds().size() == 1) {
            stability = 100.0;
        }
        for(Guild guild : f.getGuildHandler().getGuilds()) {
            stability += guild.getStabilityModifier();
        }
        if(council.couldBeBigger()) {
            double fillPercentage = council.fillPercentage();
            stability -= (1.0 - fillPercentage) * 50.0;
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

    public void calculateStability() {
        
    }
}
