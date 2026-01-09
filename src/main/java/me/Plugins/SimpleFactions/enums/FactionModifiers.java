package me.Plugins.SimpleFactions.enums;

public enum FactionModifiers {

    TAX(false),
    LEVY(false),
    NODE_SPEED(true),
    MILITARY_UPKEEP(false),
    PRESTIGE(true),
    PRESTIGE_BONUS(true),
    DE_JURE(false),
    TRADE_POWER(true);

    private final boolean positiveIsGood;

    FactionModifiers(boolean positiveIsGood) {
        this.positiveIsGood = positiveIsGood;
    }

    public boolean isPositiveGood() {
        return positiveIsGood;
    }
}
