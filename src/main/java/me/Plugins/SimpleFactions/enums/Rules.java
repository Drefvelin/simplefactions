package me.Plugins.SimpleFactions.enums;

public enum Rules {
    CAN_HAVE_VASSALS("Can Have Vassals"),
    CAN_MAKE_FEDERATION("Can Form Federation"),
    LEADER_CAN_BE_ON_COUNCIL("Leader can be Councilmember"),
    LEADER_ELECTIONS("Leader Elections"),
    COUNCIL_ELECTIONS("Council Elections"),
    HAS_COUNCIL("Has Council"),
    APPOINTED_COUNCIL("Leader Appoints Council"),
    WEALTH_BASED_COUNCIL("Wealth-Based Council"),
    ELECTED_COUNCIL("Elected Council"),
    CITIZEN_TAX("Can Collect Citizen Taxes"),
    VASSAL_TAX("Can Collect Vassal Taxes"),
    GUILD_TAX("Can Collect Guild Taxes"),
    DIVIDEND_TAX("Can Collect Dividend Taxes"),
    CAN_RECRUIT_PROFESSIONAL_ARMY("Can Recruit Professional Army");

    private final String display;

    Rules(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}

