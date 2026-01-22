package me.Plugins.SimpleFactions.Database;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

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

    @SerializedName("citizen tax")
    public Double citizenTax;

    @SerializedName("guild tax")
    public Double guildTax;

    @SerializedName("vassal tax")
    public Double vassalTax;

    @SerializedName("dividend tax")
    public Double dividendTax;

    public Double tariffs;

    @SerializedName("specific taxes")
    public HashMap<String, HashMap<String, Double>> specificTaxes = new HashMap<>();

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

    public GovernmentData governmentData;

    @SerializedName("faction modifiers")
    public List<String> factionModifiers = new ArrayList<>();

    public List<String> laws = new ArrayList<>();
}
