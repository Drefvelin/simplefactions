package me.Plugins.SimpleFactions.government;

import java.util.List;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.enums.Rules;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class Government {
    private Faction f;
    private Council council;
    private double power;
    private double powerGain;

    public Government(Faction f) {
        this.f = f;
        this.council = new Council(f);
        this.power = -1;
        this.powerGain = -1;
    }

    public void ping() {
        this.council.reorganize();
        if(power == -1) this.power = f.getMembers().size() * 10;
        if(powerGain == -1) this.powerGain = 1;
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
        double stability = 25.0;
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
