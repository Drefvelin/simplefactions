package me.Plugins.SimpleFactions.enums;

public enum Rules {
    CAN_HAVE_VASSALS("Can Have Vassals", true),
    CAN_MAKE_FEDERATION("Can Form Federation", false),
    LEADER_CAN_BE_ON_COUNCIL("Leader can be Councilmember", true),
    LEADER_ELECTIONS("Leader Elections", false),
    HAS_COUNCIL("Has Council", false),
    APPOINTED_COUNCIL("Leader Appoints Council", false),
    WEALTH_BASED_COUNCIL("Wealth-Based Council", false),
    ELECTED_COUNCIL("Elected Council", false),
    CITIZEN_TAX("Can Collect Citizen Taxes", true),
    VASSAL_TAX("Can Collect Vassal Taxes", true),
    GUILD_TAX("Can Collect Guild Taxes", true),
    DIVIDEND_TAX("Can Collect Dividend Taxes", true),
    CAN_RECRUIT_PROFESSIONAL_ARMY("Can Recruit Professional Army", true),
    NO_COUNCIL("No Council", false);

    private final String display;
    private final boolean absentTrue;

    Rules(String display, boolean absentTrue) {
        this.display = display;
        this.absentTrue = absentTrue;
    }

    public String getDisplay() {
        return display;
    }

    public boolean trueIfAbsent() {
        return absentTrue;
    }
}

