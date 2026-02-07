package me.Plugins.SimpleFactions.Guild.loans;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class LoanHandler {
    private Guild guild;
    private int creditScore = 50;
    private List<Loan> issuedLoans = new ArrayList<>();

    public LoanHandler(Guild guild) {
        this.guild = guild;
    }

    public LoanHandler(Guild guild, int creditScore, List<Loan> issuedLoans) {
        this.guild = guild;
        this.creditScore = creditScore;
        this.issuedLoans = issuedLoans;
    }

    public List<Loan> getLoansByGuild(Guild g) {
        List<Loan> loans = new ArrayList<>();
        for(Loan l : issuedLoans) {
            if(l.getBorrower().getId().equalsIgnoreCase(g.getId())) {
                loans.add(l);
            }
        }
        return loans;
    }

    public List<Loan> getLoansGiven() {
        return issuedLoans;
    }

    public List<Loan> getLoansTaken() {
        List<Loan> loansTaken = new ArrayList<>();
        for(Guild g : FactionManager.getAllGuilds()) {
            if(g.getId().equalsIgnoreCase(guild.getId())) continue;
            loansTaken.addAll(g.getLoanHandler().getLoansByGuild(guild));
        }
        return loansTaken;
    }

    public double getTotalOwed() {
        double total = 0.0;
        for(Loan l : getLoansTaken()) {
            total += l.getTotalOwed();
        }
        return total;
    }

    public double getDailyInterest() {
        double total = 0.0;
        for(Loan l : getLoansTaken()) {
            total += l.getDailyInterest();
        }
        return total;
    }   

    public double getTotalLent() {
        double total = 0.0;
        for(Loan l : issuedLoans) {
            total += l.getTotalOwed();
        }
        return total;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public String getCreditScoreString() {
        // Clamp stability just in case
        double s = Math.max(0, Math.min(100, getCreditScore()));
        double t = s / 100.0;

        // Dark red → bright green
        int startR = 139, startG = 0,   startB = 0;
        int endR   = 0,   endG   = 255, endB   = 0;

        int r = (int) Math.round(startR + (endR - startR) * t);
        int g = (int) Math.round(startG + (endG - startG) * t);
        int b = (int) Math.round(startB + (endB - startB) * t);

        return StringFormatter.formatHex(String.format("#%02X%02X%02X"+getCreditScore()+"/100", r, g, b));
    }
}
