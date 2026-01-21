package me.Plugins.SimpleFactions.Objects.Handler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.Loaders.LawLoader;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.FactionModifier;
import me.Plugins.SimpleFactions.Utils.ModifierMerger;
import me.Plugins.SimpleFactions.enums.Region;
import me.Plugins.SimpleFactions.enums.Scope;
import me.Plugins.SimpleFactions.laws.Law;
import me.Plugins.SimpleFactions.laws.LawEffect;
import me.Plugins.SimpleFactions.laws.LawGroup;

public class LawHandler {
    private Faction f;
    private Map<String, LawGroup> laws = new LinkedHashMap<>();

    public List<Law> getCurrentLaws() {
        List<Law> lawList = new ArrayList<>();
        for(LawGroup group : getGroupList()) {
            if(group.getCurrent() == null) continue;
            lawList.add(group.getCurrent());
        }
        return lawList;
    }

    public LawHandler(Faction f) {
        this.f = f;
        for(Map.Entry<String, LawGroup> entry : LawLoader.get().entrySet()) {
            laws.put(entry.getKey(), new LawGroup(f, entry.getValue()));
        }
    }

    public void apply() {
        for(LawGroup group : laws.values()) {
            Law current = group.getCurrent();
            if(current != null) {
                f.applyLaw(current, group);
            }
        }
    }

    public List<LawGroup> getGroupList() {
        return new ArrayList<>(laws.values());
    }

    public LawGroup getGroup(String id) {
        return laws.getOrDefault(id, null);
    }

    public Law getLaw(String group, String law) {
        LawGroup lawGroup = getGroup(group);
        if (lawGroup == null) return null;
        return lawGroup.getLaws().getOrDefault(law, null);
    }

    public List<FactionModifier> getLawModifiers(Scope scope, Region region) {
        List<FactionModifier> result = new ArrayList<>();

        for (LawGroup group : laws.values()) {

            Law current = group.getCurrent();
            if (current == null || !current.hasEffects()) continue;

            LawEffect effect = current.getScopedEffects().get(scope);
            if (effect == null) continue;

            // ---- Global (scope-only) modifiers ----
            if (effect.hasGlobalModifiers()) {
                result.addAll(effect.getGlobalModifiers());
            }

            // ---- Region-specific modifiers ----
            if (region != null && effect.hasRegionModifiers()) {
                List<FactionModifier> regionMods =
                        effect.getRegionModifiers().get(region);

                if (regionMods != null) {
                    result.addAll(regionMods);
                }
            }
        }

        return ModifierMerger.merge(result);
    }
}
