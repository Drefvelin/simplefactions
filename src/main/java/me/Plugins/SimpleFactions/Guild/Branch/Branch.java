package me.Plugins.SimpleFactions.Guild.Branch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import me.Plugins.SimpleFactions.Guild.GuildType;
import me.Plugins.SimpleFactions.Loaders.GuildLoader;
import me.Plugins.SimpleFactions.enums.GuildModifier;

public class Branch {
    private String id;
    private String name;
    private String icon;
    private int group;
    private List<GuildType> allowedTypes = new ArrayList<>();
    private Map<GuildModifier, BranchModifier> modifiers = new HashMap<>();
    private int level;

    public Branch(String key, ConfigurationSection config) {
        id = key;
        name = config.getString("name", id);
        icon = config.getString("icon", "black_dye.10");
        group = config.getInt("group", 0);
        level = 0;
        for(String s : config.getStringList("allowed-types")) {
            GuildType type = GuildLoader.getByString(s);
            if(type != null) allowedTypes.add(type);
        }
        if(config.contains("modifiers")) {
            for(String s : config.getStringList("modifiers")) {
                String[] args = s.split("\\s+");
                if(args.length < 2) continue;
                try {
                    double base = Double.parseDouble(args[1]);
                    double perLevel = base;
                    if(args.length == 3) {
                        perLevel = Double.parseDouble(args[2]);
                    }
                    modifiers.put(GuildModifier.valueOf(args[0].toUpperCase()), new BranchModifier(base, perLevel));
                } catch (Exception e) {
                    Bukkit.getLogger().info("[SimpleFactions] could not parse modifier "+s);
                    // TODO: handle exception
                }
            }
        }
    }

    public Branch(Branch b, int level) {
        id = b.id;
        name = b.name;
        icon = b.icon;
        group = b.group;
        allowedTypes = b.allowedTypes;
        modifiers = b.modifiers;
        this.level = level;
    }


    public String getId() { return id; }
    public int getLevel() { return level; }
}
