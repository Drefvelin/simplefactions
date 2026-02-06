package me.Plugins.SimpleFactions.Guild.loans;

import java.util.LinkedHashMap;
import java.util.Map;

import me.Plugins.SimpleFactions.Guild.Guild;

public class LoanHandler {
    private Guild guild;
    private Map<String, Loan> issuedLoans = new LinkedHashMap<>();

    public LoanHandler(Guild guild) {
        this.guild = guild;
    }
}
