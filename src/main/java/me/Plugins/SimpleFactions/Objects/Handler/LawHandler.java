package me.Plugins.SimpleFactions.Objects.Handler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.Loaders.LawLoader;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.laws.LawGroup;

public class LawHandler {
    private Faction f;
    private Map<String, LawGroup> laws = new LinkedHashMap<>();

    public List<LawGroup> getGroups() {
        return new ArrayList<>(laws.values());
    }

    public LawHandler(Faction f) {
        this.f = f;
        for(Map.Entry<String, LawGroup> entry : LawLoader.get().entrySet()) {
            laws.put(entry.getKey(), new LawGroup(f, entry.getValue()));
        }
    }

    public List<LawGroup> getGroupList() {
        return new ArrayList<>(laws.values());
    }
}
