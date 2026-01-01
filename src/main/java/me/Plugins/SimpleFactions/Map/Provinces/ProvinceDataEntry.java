package me.Plugins.SimpleFactions.Map.Provinces;

public class ProvinceDataEntry {
    private final String id;
    private double trade;
    private double production;

    public ProvinceDataEntry(String id) {
        this.id = id;
        this.trade = 0;
        this.production = 0;
    }

    public ProvinceDataEntry(String id, double trade, double production) {
        this.id = id;
        this.trade = trade;
        this.production = production;
    }

    public boolean isConsidered() {
        return trade > 0 || production > 0;
    }

    public String getId() { return id; }
    public double getTrade() { return trade; }
    public double getProduction() { return production; }

    public void setTrade(double t) {
        trade = t;
    }

    public void setProduction(double p) {
        production = p;
    }
}
