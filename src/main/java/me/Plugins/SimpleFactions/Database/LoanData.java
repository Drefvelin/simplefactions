package me.Plugins.SimpleFactions.Database;

import me.Plugins.SimpleFactions.Guild.loans.Loan;

public class LoanData {
    public String id;
    public double amount;
    public double paidInterest;
    public double unpaidInterest;
    public String issuer;
    public String borrower;
    public long issueDate;
    public long dueDate;
    public double interestRate;
    public double paid;
    public boolean autoPay;

    public double tempPayment = 0; //Used by the ledger to earmark a payment without processing during that calculation cycle
    public double tempInterestPayment = 0; //Used by the ledger to earmark an interest payment without processing during that calculation cycle

    public boolean defaulted = false;
    public boolean pausedInterest = false;

    public LoanData(Loan loan) {
        this.id = loan.getId();
        this.amount = loan.getAmount();
        this.paidInterest = loan.getPaidInterest();
        this.unpaidInterest = loan.getUnpaidInterest();
        this.issuer = loan.getIssuer().getId();
        this.borrower = loan.getBorrower().getId();
        this.issueDate = loan.getIssueDate();
        this.dueDate = loan.getDueDate();
        this.interestRate = loan.getInterestRate();
        this.paid = loan.getPaid();
        this.autoPay = loan.isAutoPay();
        this.defaulted = loan.hasDefaulted();
        this.pausedInterest = loan.isInterestPaused();
    }
}
