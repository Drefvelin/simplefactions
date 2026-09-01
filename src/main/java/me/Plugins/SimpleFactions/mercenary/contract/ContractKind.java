package me.Plugins.SimpleFactions.mercenary.contract;

/**
 * What a contract buys. The discriminator exists from the first version so the
 * assassin program reuses this object instead of copying it.
 */
public enum ContractKind {
    MERCENARY("Mercenary Contract"),
    ASSASSIN("Assassination Contract");

    private final String display;

    ContractKind(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
