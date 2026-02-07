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
    private boolean autoPay;

    public Loan(double amount, Guild issuer, Guild borrower, long issueDate, long dueDate, double interestRate, boolean autoPay) {
        this.amount = amount;
        this.issuer = issuer;
        this.borrower = borrower;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.interestRate = interestRate;
        this.paid = 0.0;
        this.autoPay = autoPay;
    }

    public double getAmount() {
        return amount;
    }

    public Guild getIssuer() {
        return issuer;
    }

    public Guild getBorrower() {
        return borrower;
    }

    public long getIssueDate() {
        return issueDate;
    }

    public long getDueDate() {
        return dueDate;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public double getPaid() {
        return paid;
    }

    public boolean isAutoPay() {
        return autoPay;
    }

    public double getDailyInterestRate() {
        return interestRate / 7.0;
    }

    public double getDailyInterest() {
        return getTotalOwed() * getDailyInterestRate() / 100.0;
    }

    public double getTotalOwed() {
        return amount - paid;
    }

    public int getDaysUntilDue() {
        long now = System.currentTimeMillis();
        long diff = dueDate - now;
        return (int) (diff / (1000 * 60 * 60 * 24));
    }

    public int getDurationInDays() {
        long diff = dueDate - issueDate;
        return (int) (diff / (1000 * 60 * 60 * 24));
    }

    public double getDailyPayment() {
        int daysUntilDue = getDaysUntilDue();
        if (daysUntilDue <= 0) return getTotalOwed(); // Due or overdue
        return (getTotalOwed() / getDurationInDays())+getDailyInterest();
    }
}
