package me.Plugins.SimpleFactions.Utils;

import java.util.HashMap;
import java.util.Map;

import me.Plugins.SimpleFactions.Guild.Guild;

public class DailyGuildTransfers {

    // from -> (to -> amount)
    private final Map<Guild, Map<Guild, Double>> transfers = new HashMap<>();

    // per-guild net change coming from/to "the system" (minting/sinks)
    private final Map<Guild, Double> externalDeltas = new HashMap<>();

    public void add(Guild from, Guild to, double amount) {
        if (amount <= 0) return;
        transfers.computeIfAbsent(from, k -> new HashMap<>())
                 .merge(to, amount, Double::sum);
    }

    // Positive = deposit to guild, Negative = withdraw from guild
    public void addExternalDelta(Guild guild, double delta) {
        if (delta == 0) return;
        externalDeltas.merge(guild, delta, Double::sum);
    }

    public Map<Guild, Map<Guild, Double>> getTransfers() {
        return transfers;
    }

    public Map<Guild, Double> getExternalDeltas() {
        return externalDeltas;
    }

    public void clear() {
        transfers.clear();
        externalDeltas.clear();
    }
}


