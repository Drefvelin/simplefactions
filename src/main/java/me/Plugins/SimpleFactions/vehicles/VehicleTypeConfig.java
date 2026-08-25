package me.Plugins.SimpleFactions.vehicles;

public final class VehicleTypeConfig {
    private final double upkeep;
    private final int size;
    private final int perPersonLimit;
    private final boolean ignoreLimit;

    public VehicleTypeConfig(double upkeep, int size, int perPersonLimit, boolean ignoreLimit) {
        this.upkeep = upkeep;
        this.size = size;
        this.perPersonLimit = perPersonLimit;
        this.ignoreLimit = ignoreLimit;
    }

    public double getUpkeep() {
        return upkeep;
    }

    public int getSize() {
        return size;
    }

    public int getPerPersonLimit() {
        return perPersonLimit;
    }

    public boolean isIgnoreLimit() {
        return ignoreLimit;
    }
}
