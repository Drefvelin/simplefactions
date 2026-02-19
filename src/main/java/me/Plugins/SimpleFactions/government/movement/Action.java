package me.Plugins.SimpleFactions.government.movement;

public enum Action {
    CHANGE_LEADER("#c5e0e3Change Leader"),
    NATIONHOOD("#c5e0e3Demand Nationhood"),
    INDEPENDENCE("#c5e0e3Declare Independence"),
    SNAP_ELECTIONS("#c5e0e3Call Snap Elections"),
    DISSOLVE("#c5e0e3Dissolve Faction"),
    NONE("#c5e0e3Do Nothing");

    private String displayName;

    Action(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplay() {
        return displayName;
    }
}
