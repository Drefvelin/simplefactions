package me.Plugins.SimpleFactions.Guild.loans;

import java.util.UUID;

import me.Plugins.SimpleFactions.Guild.Guild;

public class Loan {
    private String id;
    private double amount;
    private double originalAmount;
    private Guild issuer;
    private Guild borrower;
    private long issueDate;
    private long dueDate;
    private double interestRate;
    private double paid;
    private boolean autoPay;

    public Loan(double amount, Guild issuer, Guild borrower, long issueDate, int durationInDays, double interestRate, boolean autoPay) {
        id = UUID.randomUUID().toString();
        this.amount = amount;
        this.originalAmount = amount;
        this.issuer = issuer;
        this.borrower = borrower;
        this.issueDate = issueDate;
        this.dueDate = issueDate + durationInDays * 24 * 60 * 60 * 1000L;
        this.interestRate = interestRate;
        this.paid = 0.0;
        this.autoPay = autoPay;
    }

    public Loan(String id, double amount, double originalAmount, Guild issuer, Guild borrower, long issueDate, int durationInDays, double interestRate, boolean autoPay) {
        this.id = id;
        this.amount = amount;
        this.originalAmount = originalAmount;
        this.issuer = issuer;
        this.borrower = borrower;
        this.issueDate = issueDate;
        this.dueDate = issueDate + durationInDays * 24 * 60 * 60 * 1000L;
        this.interestRate = interestRate;
        this.paid = 0.0;
        this.autoPay = autoPay;
    }

    public String getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public double getOriginalAmount() {
        return originalAmount;
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

    public void tickDay() {
    double dailyInterest = getDailyInterest();

    if (!autoPay) {
        // Interest compounds onto the loan
        amount += dailyInterest;
    }
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
        return (getTotalOwed() / getDaysUntilDue())+getDailyInterest();
    }

    public double makePayment(double amount) {
        if(amount <= 0) return 0.0;
        
        // Clamp to what's owed
        double actualPayment = Math.min(amount, getTotalOwed());
        
        // Transfer money from borrower to issuer
        borrower.getBank().withdraw(actualPayment);
        issuer.getBank().deposit(actualPayment);
        
        // Update paid amount
        paid += actualPayment;
        return actualPayment;
    }
}
