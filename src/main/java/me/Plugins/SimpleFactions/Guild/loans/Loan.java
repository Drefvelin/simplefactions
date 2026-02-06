package me.Plugins.SimpleFactions.Guild.loans;

import me.Plugins.SimpleFactions.Guild.Guild;

public class Loan {
    private double amount;
    private Guild issuer;
    private Guild borrower;
    private long issueDate;
    private long dueDate;
    private double interestRate;
    private double paid;
}
