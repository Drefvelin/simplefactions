package me.Plugins.SimpleFactions.Guild.loans;

import java.util.UUID;

import me.Plugins.SimpleFactions.Guild.Guild;

public class Loan {
    private String id;
    private double amount;
    private double paidInterest;
    private double unpaidInterest;
    private Guild issuer;
    private Guild borrower;
    private long issueDate;
    private long dueDate;
    private double interestRate;
    private double paid;
    private boolean autoPay;

    private double tempPayment = 0; //Used by the ledger to earmark a payment without processing during that calculation cycle
    private double tempInterestPayment = 0; //Used by the ledger to earmark an interest payment without processing during that calculation cycle

    public Loan(double amount, Guild issuer, Guild borrower, long issueDate, int durationInDays, double interestRate, boolean autoPay) {
        id = UUID.randomUUID().toString();
        this.amount = amount;
        this.paidInterest = 0;
        this.unpaidInterest = 0;
        this.issuer = issuer;
        this.borrower = borrower;
        this.issueDate = issueDate;
        this.dueDate = issueDate + durationInDays * 24 * 60 * 60 * 1000L;
        this.interestRate = interestRate;
        this.paid = 0.0;
        this.autoPay = autoPay;
    }

    public Loan(String id, double amount, double paidInterest, double unpaidInterest, Guild issuer, Guild borrower, long issueDate, int durationInDays, double interestRate, boolean autoPay) {
        this.id = id;
        this.amount = amount;
        this.paidInterest = paidInterest;
        this.unpaidInterest = unpaidInterest;
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

    public double getPaidInterest() {
        return paidInterest;
    }

    public double getUnpaidInterest() {
        return unpaidInterest;
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

    public void setAutoPay(boolean autoPay) {
        this.autoPay = autoPay;
    }

    public void tickDay() {
        double dailyInterest = getDailyInterest();

        if (!autoPay) {
            // Interest compounds onto the loan
            unpaidInterest += dailyInterest;
        }
        if(tempPayment > 0) {
            makePayment(tempPayment, false);
            tempPayment = 0;
        }
        if(tempInterestPayment > 0) {
            paidInterest += tempInterestPayment;
            paid += tempInterestPayment;
            tempInterestPayment = 0;
        }
    }
        
    public void setTempPayment(double amount) {
        this.tempPayment = amount;
    }

    public void setTempInterestPayment(double amount) {
        this.tempInterestPayment = amount;
    }

    public double getDailyInterestRate() {
        return interestRate / 7.0;
    }

    public double getDailyInterest() {
        return getTotalOwed() * getDailyInterestRate() / 100.0;
    }

    public double getTotalOwed() {
        return amount + unpaidInterest + paidInterest - paid;
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

    public double makePayment(double paymentAmount, boolean ledger) {
        if (paymentAmount <= 0) return 0.0;

        double totalOwed = getTotalOwed();
        if (totalOwed <= 0) return 0.0;

        // Clamp payment to what is owed
        double actualPayment = Math.min(paymentAmount, totalOwed);

        double remaining = actualPayment;

        if (unpaidInterest > 0) {
            double interestPaid = Math.min(remaining, unpaidInterest);
            unpaidInterest -= interestPaid;
            if (ledger) issuer.getLedger().addInterestPaymentEntry(borrower.getId(), interestPaid);
            paidInterest += interestPaid; // Move paid interest to the main interest pool
            remaining -= interestPaid;
        }

        if (ledger) issuer.getLedger().addLoanPaymentEntry(borrower.getId(), remaining);

        paid += actualPayment;

        return actualPayment;
    }
}
