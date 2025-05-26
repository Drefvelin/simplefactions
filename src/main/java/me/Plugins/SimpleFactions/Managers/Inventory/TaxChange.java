package me.Plugins.SimpleFactions.Managers.Inventory;

public class TaxChange {
    private boolean domestic;
    private int time;

    public TaxChange(boolean b) {
        domestic = b;
        time = 0;
    }

    public boolean tick() {
        time++;
        return time == 30;
    }

    public boolean isDomestic() {
        return domestic;
    }
}
