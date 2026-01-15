package me.Plugins.SimpleFactions.government.proposal;

import me.Plugins.SimpleFactions.government.Government;
import me.Plugins.SimpleFactions.laws.Law;

public class Proposal {
    private Government gov;
    private String proposer;

    private Law law;
    private TaxLawChange tax;

    public Proposal(String proposer,Government gov) {
        this.gov = gov;
        this.proposer = proposer;
    }

    public String getProposer() {
        return proposer;
    }

    public boolean isLawProposal() {
        return law != null;
    }
    public Law getLaw() {
        return law;
    }
    public void setLawProposal(Law law) {
        this.law = law;
    }
    public boolean isTaxProposal() {
        return tax != null;
    }
    public TaxLawChange getTaxChange() {
        return tax;
    }
    public void setTaxProposal(TaxLawChange tax) {
        this.tax = tax;
    }
}
