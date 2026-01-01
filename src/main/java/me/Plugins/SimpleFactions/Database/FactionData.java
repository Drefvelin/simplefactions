package me.Plugins.SimpleFactions.Database;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class FactionData {
    public String id;
    public String name;
    public String rgb;
    public String leader;

    @SerializedName("ruler title")
    public String rulerTitle;

    public String government;
    public String culture;
    public String religion;

    @SerializedName("tax rate")
    public Double taxRate;

    @SerializedName("vassal tax rate")
    public Double vassalTaxRate;

    public Integer capital;

    @SerializedName("extra node capacity")
    public Double extraNodeCapacity;

    public List<String> banner = new ArrayList<>();
    public List<Number> provinces = new ArrayList<>();
    public List<String> titles = new ArrayList<>();
    public List<String> relations = new ArrayList<>();

    @SerializedName("tier index")
    public Double tierIndex;

    public List<String> military = new ArrayList<>();

    @SerializedName("military queue")
    public List<String> militaryQueue = new ArrayList<>();

    @SerializedName("prestige modifiers")
    public List<String> prestigeModifiers = new ArrayList<>();

    public String overlord;

    public List<GuildData> guilds = new ArrayList<>();

    @SerializedName("faction modifiers")
    public List<String> factionModifiers = new ArrayList<>();
}
