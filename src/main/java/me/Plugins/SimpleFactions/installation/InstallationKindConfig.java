package me.Plugins.SimpleFactions.installation;

public final class InstallationKindConfig {
    private final double dailyUpkeep;
    private final int constructionTimeSeconds;

    public InstallationKindConfig(double dailyUpkeep, int constructionTimeSeconds) {
        this.dailyUpkeep = dailyUpkeep;
        this.constructionTimeSeconds = constructionTimeSeconds;
    }

    public double getDailyUpkeep() {
        return dailyUpkeep;
    }

    public int getConstructionTimeSeconds() {
        return constructionTimeSeconds;
    }
}
