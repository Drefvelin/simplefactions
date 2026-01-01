package me.Plugins.SimpleFactions.Objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import me.Plugins.SimpleFactions.Army.Military;
import me.Plugins.SimpleFactions.Army.Regiment;
import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Diplomacy.Relation;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Loaders.RankLoader;
import me.Plugins.SimpleFactions.Loaders.TierLoader;
import me.Plugins.SimpleFactions.Loaders.TitleLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Objects.Handler.GuildHandler;
import me.Plugins.SimpleFactions.REST.RestServer;
import me.Plugins.SimpleFactions.Tiers.Tier;
import me.Plugins.SimpleFactions.Tiers.Title;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.Utils.RandomRGB;
import me.Plugins.SimpleFactions.enums.FactionModifiers;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class Faction {
	private Formatter format = new Formatter();
	private String id;
	private String name;
	private PrestigeRank rank;
	private String government;
	private String culture;
	private String religion;
	private String rgb;
	private ItemStack banner;
	private List<String> bannerPatterns = new ArrayList<>();
	private List<String> invited = new ArrayList<>();
	private Double wealth;
	private Double prestige;
	private String rulerTitle;
	private String leader;
	private Integer extraNodeCapacity;
	private List<Modifier> prestigeModifiers = new ArrayList<>();
	private List<Integer> provinces = new ArrayList<>();

	private int capital = -1;
	
	private double taxRate = 5;
	private double vassalTax = 100;

	private Tier tier;
	
	//Diplomacy
	private HashMap<String, Relation> relations = new HashMap<>();
	
	//Military
	private Military military;
	
	//Titles
	private List<Title> titles = new ArrayList<>();
	
	//Modifiers
	private HashMap<FactionModifiers, List<FactionModifier>> modifiers = new HashMap<>();

	//Guilds
	private GuildHandler guildHandler = new GuildHandler();
	
	public Faction(String id, String leader) {
		this.id = format.formatId(id);
		this.name = StringFormatter.formatHex(format.formatName(id));
		this.leader = leader;
		this.rulerTitle = "Leader";
		this.bannerPatterns = RestServer.fetchBannerList();
		this.rank = RankLoader.getLowest();
		this.government = "Homestead";
		this.culture = "Multicultural";
		this.religion = "Religious Diversity";
		this.wealth = 0.0;
		this.prestige = 0.0;
		this.extraNodeCapacity = 0;
		this.rgb = RandomRGB.random();
		while(!RandomRGB.isFree(rgb)) {
			this.rgb = RandomRGB.random();
		}
		this.military = new Military(this);
		guildHandler.addGuild(new Guild(this));
		init();
		createBanner(bannerPatterns);
		updatePrestige();
		updateTier();
	}
	public Faction(String id, String rgb, List<Integer> provinces, List<Title> titles, String leader, String name, String rulerTitle, List<String> patterns, String government, String culture, String religion, int exCap, List<Modifier> prestigeModifiers, double taxRate, double vassalTax, int capital) {
		this.id = id;
		this.name = name;
		this.leader = leader;
		this.rulerTitle = rulerTitle;
		this.bannerPatterns = patterns;
		this.rank = RankLoader.getLowest();
		this.government = government;
		this.culture = culture;
		this.religion = religion;
		this.wealth = 0.0;
		this.prestige = 0.0;
		this.extraNodeCapacity = exCap;
		this.prestigeModifiers = prestigeModifiers;
		this.rgb = rgb;
		this.capital = capital;
		for(int i : provinces) {
			if(TitleManager.getByProvince(i) != null) continue;
			this.provinces.add(i);
		}
		this.titles = titles;
		this.military = new Military(this);
		this.taxRate = taxRate;
		this.vassalTax = vassalTax;
		init();
		createBanner(bannerPatterns);
		updateTier();
	}
	
	private void createBanner(List<String> patterns) {
		ItemStack item = new ItemStack(Material.valueOf(patterns.get(0).split("\\.")[0].toUpperCase()+"_BANNER"), 1);
		BannerMeta b = (BannerMeta) item.getItemMeta();
		b.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		for(int i = 1; i < patterns.size(); i++) {
			String p = patterns.get(i);
			String colour = p.split("\\.")[0];
			String pattern = p.split("\\.")[1];
			try {
			    PatternType patternType = PatternType.valueOf(pattern.toUpperCase());
			    DyeColor dyeColor = DyeColor.valueOf(colour.toUpperCase());
			    b.addPattern(new Pattern(dyeColor, patternType));
			} catch (IllegalArgumentException e) {
				try {
				    PatternType patternType = PatternType.valueOf("tfmc:"+pattern.toLowerCase());
				    DyeColor dyeColor = DyeColor.valueOf(colour.toUpperCase());
				    b.addPattern(new Pattern(dyeColor, patternType));
				} catch (IllegalArgumentException ex) {
				    // Invalid pattern or color name, skip it
					Bukkit.getLogger().info(pattern+" is not a valid pattern");
				}
			}
		}
		item.setItemMeta(b);
		this.banner = item;
	}

	public Guild getOrCreateMainGuild() {
		Guild g = guildHandler.getGuild(id);
		if (g == null) {
			g = new Guild(this);
			guildHandler.addGuild(g);
		}
		return g;
	}

	public boolean hasCapital() {
		return capital != -1;
	}

	public int getCapital() {
		return capital;
	}

	public void setCapital(int i) {
		if(!provinces.contains(i)) return;
		getOrCreateMainGuild().setCapital(i);
		capital = i;
	}

	public double getForeignTaxRate(Faction f) {
		double taxRate = 0;
		for(FactionModifier mod : getModifiers()) {
			if(mod.getFrom() == null) continue;
			if(!mod.getFrom().getId().equalsIgnoreCase(f.getId())) continue;
			if(!mod.getType().equals(FactionModifiers.TAX)) continue;
			double tax = mod.getAmount();
			String overlord = RelationManager.getOverlord(this);
			if(overlord != null && overlord.equalsIgnoreCase(mod.getFrom().getId())) {
				tax = mod.getFrom().getVassalTaxRate()/100.0*tax;
			}
			Faction from = mod.getFrom();
			if(from.getBank() == null) continue;
			taxRate += tax;
		}
		return taxRate;
	}

	public double getTotalForeignTaxRate() {
		double taxRate = 0;
		for(FactionModifier mod : getModifiers()) {
			if(mod.getFrom() == null) continue;
			if(!mod.getType().equals(FactionModifiers.TAX)) continue;
			taxRate+=getForeignTaxRate(mod.getFrom());
		}
		return taxRate;
	}

	public void giveTax(double amount) {
    	giveTax(amount, new HashSet<>());
	}

	private void giveTax(double amount, Set<String> visitedFactions) {
		if (!visitedFactions.add(this.getId())) return; // prevent recursion loops

		double paidTax = 0;

		for (FactionModifier mod : getModifiers()) {
			if (paidTax >= amount) break;
			if (mod.getFrom() == null || !mod.getType().equals(FactionModifiers.TAX)) continue;

			double tax = mod.getAmount() / 100.0 * amount;
			Faction from = mod.getFrom();

			String overlordId = RelationManager.getOverlord(this);
			if (overlordId != null && overlordId.equalsIgnoreCase(from.getId())) {
				tax = from.getVassalTaxRate() / 100.0 * tax;
			}

			if (from.getBank() == null) continue;
			paidTax += tax;
			from.giveTax(tax, visitedFactions);
		}

		amount -= paidTax;
		getBank().deposit(amount);
	}

	public double setTaxRate(double d) {
		double totalForeignTax = getTotalForeignTaxRate();
		if(d+totalForeignTax > 100) d = 100-totalForeignTax;
		taxRate = Math.min(60, d);
		return taxRate;
	}
	
	public double getTaxRate() {
		return taxRate;
	}

	public void setVassalTaxRate(double d) {
		vassalTax = Math.max(20, Math.min(100, d));
	}

	public double getVassalTaxRate() {
		return vassalTax;
	}
	
	public void init() {
		addModifier(null, new FactionModifier(FactionModifiers.DE_JURE, Cache.deJureRequirement));
	}
	
	public HashMap<String, Relation> getRelations(){
		return relations;
	}
	
	public Relation getRelation(String s) {
		if(relations.containsKey(s)) return relations.get(s);
		return new Relation();
	}
	
	public Military getMilitary() {
		return military;
	}
	
	public void tick() {
		//taxation fix, doubt this will be neccesary
		double tax = getTotalForeignTaxRate();
		if(taxRate + tax > 100) taxRate = 100-tax;
		
		military.tick();
		for(FactionModifier m : getModifiers()) {
			if(!m.isTimed()) continue;
			if(m.tick()) removeModifier(m);
		}
	}

	public boolean hasProvince(int i) {
		return provinces.contains(i);
	}
	
	public void addProvince(int i) {
		if(provinces.contains(i)) return;
		provinces.add(i);
		updateTier();
	}
	
	public void removeProvince(int i) {
		for(int x = 0; x<provinces.size(); x++) {
			int p = provinces.get(x);
			if(p == i) {
				provinces.remove(x);
				return;
			}
		}
		updateTier();
	}
	public List<Integer> getProvinces(){
		return provinces;
	}
	public List<String> getInvited() {
		return invited;
	}
	public void setInvited(List<String> invited) {
		this.invited = invited;
	}
	public List<Modifier> getWealthModifiers() {
		List<Modifier> list = new ArrayList<>(getOrCreateMainGuild().getWealthModifiers());
		for(Guild guild : guildHandler.getGuilds()) {
			if(guild.isBase()) continue;
			if(guild.getWealth() == 0) continue;
			list.add(new Modifier(guild.getName()+" #a39ba8("+guild.getType().getName()+"#a39ba8)", guild.getWealth()));
		}
		return list;
	}
	public void setRGB(String rgb) {
		this.rgb = rgb;
	}
	public String getRGB() {
		return this.rgb;
	}
	public String getRulerTitle() {
		return rulerTitle;
	}
	public void setRulerTitle(String rulerTitle) {
		this.rulerTitle = rulerTitle;
	}
	public PrestigeRank getRank() {
		return rank;
	}
	public void setRank(PrestigeRank rank) {
		if(this.rank.hasModifiers()) {
			removeModifiers(this.rank.getModifiers());
		}
		this.rank = rank;
		if(rank.hasModifiers()) {
			addModifiers(null, rank.getModifiers());
		}
	}
	public String getGovernment() {
		return government;
	}
	public void setGovernment(String government) {
		this.government = government;
	}
	public String getCulture() {
		return culture;
	}
	public void setCulture(String culture) {
		this.culture = culture;
	}
	public String getReligion() {
		return religion;
	}
	public void setReligion(String religion) {
		this.religion = religion;
	}
	public ItemStack getBanner() {
		return banner;
	}
	public void addPersistentPrestigeModifier(Modifier p) {
		for(int i = 0; i<prestigeModifiers.size(); i++) {
			if(prestigeModifiers.get(i).getType().equalsIgnoreCase(p.getType())) {
				p.setAmount(prestigeModifiers.get(i).getAmount()+p.getAmount());
				if(Double.compare(p.getAmount(), 0) == 0) {
					prestigeModifiers.remove(i);
					i--;
				} else {
					prestigeModifiers.set(i, p);
				}
				return;
			}
		}
		if(p.getAmount() != 0) {
			prestigeModifiers.add(p);
		}
	}
	public void addPrestigeModifier(Modifier p) {
		for(int i = 0; i<prestigeModifiers.size(); i++) {
			if(prestigeModifiers.get(i).getType().equalsIgnoreCase(p.getType())) {
				prestigeModifiers.set(i, p);
				return;
			}
		}
		prestigeModifiers.add(p);
	}
	public void setBanner(ItemStack banner) {
		BannerMeta b = (BannerMeta) banner.getItemMeta();
		this.bannerPatterns.clear();
		this.bannerPatterns.add(banner.getType().toString().replace("_BANNER", ".BASE"));
		for(Pattern p : b.getPatterns()) {
			String colour = p.getColor().toString();
			String pattern = p.getPattern().toString();
			this.bannerPatterns.add(colour+"."+pattern);
		}
		createBanner(bannerPatterns);
	}
	public List<String> getBannerPatterns() {
		return bannerPatterns;
	}
	public void setBannerPatterns(List<String> bannerPatterns) {
		this.bannerPatterns = bannerPatterns;
		createBanner(bannerPatterns);
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<String> getMembers() {
		return guildHandler.getAllMembers();
	}
	public void addMember(String m) {
		getOrCreateMainGuild().addMember(m);
	}
	public void forceRemoveMember(String m) {
		guildHandler.forceKick(m);;
	}
	public boolean isInGuild(String member) {
		if(!getMembers().contains(member)) return false;
		return !getOrCreateMainGuild().isMember(member);
	}
	public boolean canBeCleanKicked(String p) {
		if(leader.equalsIgnoreCase(p)) return false;
		return !guildHandler.isGuildLeader(p);
	}
	public Double getWealth() {
		return wealth;
	}
	public void setWealth(Double wealth) {
		this.wealth = wealth;
	}
	public Double getPrestige() {
		return prestige;
	}
	public void setPrestige(Double prestige) {
		this.prestige = prestige;
	}
	public Bank getBank() {
		return getOrCreateMainGuild().getBank();
	}
	public void setBank(Bank bank) {
		getOrCreateMainGuild().setBank(bank);
	}
	public String getLeader() {
		return leader;
	}
	public void setLeader(String leader) {
		getOrCreateMainGuild().setLeader(leader);
		this.leader = leader;
	}
	public List<Modifier> getPrestigeModifiers() {
		return prestigeModifiers;
	}
	public void setPrestigeModifiers(List<Modifier> prestigeModifiers) {
		this.prestigeModifiers = prestigeModifiers;
	}
	public Integer getExtraNodeCapacity() {
		return extraNodeCapacity;
	}
	public void setExtraNodeCapacity(Integer extraNodeCapacity) {
		this.extraNodeCapacity = extraNodeCapacity;
	}
	public boolean canPurchaseCapacity() {
		return this.extraNodeCapacity < Cache.maxExtraNodeCapacity;
	}
	public GuildHandler getGuildHandler() {
		return guildHandler;
	}
	public void updatePrestige() {
		prestige = 0.0;
		addPrestigeModifier(new Modifier("Members", format.formatDouble(Math.pow(guildHandler.getAllMembers().size()+4, 1.8)+5)));
		if(wealth == 0) {
			addPrestigeModifier(new Modifier("Wealth", 0.0));
		}
		if(wealth > 0 && FactionManager.getGlobalWealth() > 0) {
			Double amount = wealth/FactionManager.getGlobalWealth()*Cache.maxWealthPrestige;
			if(amount > wealth) {
				amount = wealth;
			}
			addPrestigeModifier(new Modifier("Wealth", format.formatDouble(amount)));
		}
		int provincePrestige = TierLoader.getByString("province").getPrestige();
		if(provinces.size() > 0 && provincePrestige > 0) {
			addPrestigeModifier(new Modifier("Provinces", (double) (provincePrestige*provinces.size())));
		}
		if(titles.size() > 0) {
			double titleAmount = getHighestTitle().getTier().getPrestige();
			addPrestigeModifier(new Modifier("Titles", titleAmount));
		}
		if(getModifier(FactionModifiers.PRESTIGE_BONUS).getAmount() > 0.0) {
			double multiplier = getModifier(FactionModifiers.PRESTIGE_BONUS).getAmount()/100.0;
			double extra = 0.0;
			for(Modifier p : prestigeModifiers) {
				extra += p.getAmount();
			}
			extra = format.formatDouble(extra*multiplier);
			addPrestigeModifier(new Modifier(getModifier(FactionModifiers.PRESTIGE_BONUS).getAmount()+"% Bonus", extra));
		}
		double fromSubjects = 0.0;
		for(Faction s : RelationManager.getSubjects(this)) {
			if(s == null) continue;
			double added = getRelation(s.getId()).getGiveModifier(FactionModifiers.PRESTIGE);
			if(added > 0) {
				fromSubjects += s.getPrestige()*(added/100.0);
			}
		}
		if(fromSubjects > 0) {
			fromSubjects = format.formatDouble(fromSubjects);
			addPrestigeModifier(new Modifier("Subjects", fromSubjects));
		}
		for(Modifier p : prestigeModifiers) {
			prestige = prestige + p.getAmount();
		}
		prestige = format.formatDouble(prestige);
		
		if(this.rank.getLevel() < RankLoader.getRanks().size()) {
			Double rankUpAmount = FactionManager.getRankUpAmount(RankLoader.getByLevel(this.rank.getLevel()+1));
			if(prestige >= rankUpAmount) {
				this.rank = RankLoader.getByLevel(this.rank.getLevel()+1);
			}
		}
		if(this.rank.getLevel() != 1) {
			Double rankDownAmount = FactionManager.getRankUpAmount(RankLoader.getByLevel(this.rank.getLevel()));
			rankDownAmount = rankDownAmount*0.95;
			if(prestige < rankDownAmount) {
				this.rank = RankLoader.getByLevel(this.rank.getLevel()-1);
			}
		}
		String overlord = RelationManager.getOverlord(this);
		if(overlord != null) {
			Faction o = FactionManager.getByString(overlord);
			if(o != null) o.updatePrestige();
		}
	}
	public void updateWealth() {
		wealth = 0.0;
		for(Guild guild : guildHandler.getGuilds()) {
			wealth += guild.getWealth();
		}
		wealth = format.formatDouble(wealth);
		FactionManager.updateAllPrestige();
	}
	
	public void setRelation(Faction f, Relation r) {
		//Clean old relation modifiers
		if(relations.containsKey(f.getId())) {
			Relation old = relations.get(f.getId());
			if(old.getType().hasRecieveModifiers()) {
				removeModifiers(old.getType().getRecieveModifiers());
			}
			if(old.getType().hasGiveModifiers()) {
				f.removeModifiers(old.getType().getGiveModifiers());
			}
		}
		
		
		//update
		relations.put(f.getId(), r);
		
		//Apply new modifiers
		if(r.getType().hasRecieveModifiers()) addModifiers(f, r.getType().getRecieveModifiers());
		if(r.getType().hasGiveModifiers()) f.addModifiers(this, r.getType().getGiveModifiers());
	}
	
	public void updateRelations() {
		for(Map.Entry<String, Relation> entry : relations.entrySet()) {
			entry.getValue().tick();
		}
	}
	
	//Titles
	public void countyCheck() {
		List<Title> counties = getTitles(TierLoader.getByString("county"));
		Random rand = new Random();

		while (counties.size() > guildHandler.getAllMembers().size()) {
			int index = rand.nextInt(counties.size()); // pick random index
			removeTitle(counties.get(index));          // remove that county
			counties.remove(index);                    // keep local list in sync
		}
		for(Title t : new ArrayList<>(getTitles())) {
			if(!hasTitle(t)) continue;
			t.destroy(this, TitleManager.getProvinces(this), TitleManager.getTitles(this));
		}
	}
	public void resetTitles(List<Title> list) {
		titles = list;
		updatePrestige();
	}
	
	public List<Integer> getUntitledProvinces() {
		List<Integer> p = new ArrayList<>();
		for(int i : provinces) {
			if(TitleLoader.getByProvince(i) == null) p.add(i);
		}
		for(Faction subject : RelationManager.getSubjects(this)) {
			for(int i : subject.getProvinces()) {
				if(TitleLoader.getByProvince(i) == null) p.add(i);
			}
		}
		return p;
	}
	
	public List<Title> getFreeTitles(Tier tier) {
		List<Title> freeTitles = new ArrayList<>();
		for(Title t : titles) {
			if(!t.getTier().getId().equalsIgnoreCase(tier.getId())) continue;
			if(TitleLoader.getByTitle(t) == null) freeTitles.add(t);
		}
		return freeTitles;
	}
	
	public boolean hasTitle(Title t) {
		return titles.contains(t);
	}
	
	public void addTitle(Title t) {
		if(hasTitle(t)) return;
		titles.add(t);
		updateTier();
	}
	
	public void removeTitle(Title t) {
		if(!hasTitle(t)) return;
		titles.remove(t);
		t.destroy(this, TitleManager.getProvinces(this), TitleManager.getTitles(this));
		updateTier();
	}
	
	public List<Title> getTitles() {
		return titles;
	}
	
	public List<Title> getTitles(Tier t) {
		List<Title> list = new ArrayList<>();
		for(Title title : titles) {
			if(title.getTier().getTier() == t.getTier()) list.add(title);
		}
		return list;
	}
	
	public Tier getTier() {
		return tier;
	}
	
	public List<Title> getRankedTitles() {
	    return titles.stream()
	                 .sorted((a, b) -> Integer.compare(b.getTier().getTier(), a.getTier().getTier()))
	                 .collect(Collectors.toList());
	}

	
	public Title getHighestTitle() {
		return titles.stream()
	            .sorted((a, b) -> Integer.compare(b.getTier().getTier(), a.getTier().getTier()))
	            .findFirst()
	            .orElse(null);
	}
	
	public void updateTier() {
		Tier temp = null;
	    if (provinces.size() == 0 && titles.size() == 0) {
	    	temp = TierLoader.getLowest();
	    } else if (provinces.size() > 0 && titles.size() == 0) {
	    	temp = TierLoader.getByString("province");
	    } else {
	        Title highest = titles.stream()
	            .sorted((a, b) -> Integer.compare(b.getTier().getTier(), a.getTier().getTier()))
	            .findFirst()
	            .orElse(null);

	        if (highest != null) {
	        	temp = highest.getTier();
	        } else {
	        	temp = TierLoader.getLowest();
	        }
	    }
	    if(tier == null || !tier.getId().equalsIgnoreCase(temp.getId())) tier = new Tier(temp, -1);
		Player p = Bukkit.getPlayerExact(leader);
		for(Faction subject : RelationManager.getSubjects(this)) {
			if(subject.getTier().getTier() > tier.getTier()) {
				RelationManager.endVassalage(subject, this, false);
				if(p != null && p.isOnline()) {
					p.sendMessage("§cLost the subject "+subject.getName()+" §cdue to rank difference!");
				}
			}
		}
		updatePrestige();
	}

	
	
	//Modifiers
	
	public void addModifiers(Faction from, List<FactionModifier> mods) {
	    for (FactionModifier m : mods) {
	        addModifier(from, m);
	    }
	}
	
	public void addModifier(Faction from, FactionModifier m) {
		if(from != null && from.getId().equalsIgnoreCase("cape_vander")) Bukkit.getPlayerExact("drefvelin").sendMessage(m.getType().toString()+" "+m.getAmount());
		FactionModifier mod = new FactionModifier(from, m.getType(), m.getAmount(), m.getTime());
		modifiers.computeIfAbsent(mod.getType(), k -> new ArrayList<>());
        List<FactionModifier> list = modifiers.get(m.getType());
        if (!list.contains(mod)) {
            list.add(mod);
			if(mod.getFrom() != null && id.equalsIgnoreCase("Wythe")) Bukkit.getPlayerExact("drefvelin").sendMessage(mod.getFrom().getName()+" "+mod.getType().toString()+" "+mod.getAmount());
        }
	}

	
	public void removeModifiers(List<FactionModifier> mods) {
	    for (FactionModifier m : mods) {
	        removeModifier(m);
	    }
	}
	
	public void removeModifier(FactionModifier m) {
		List<FactionModifier> list = modifiers.get(m.getType());
        if (list != null) {
            list.remove(m);
            if (list.isEmpty()) {
                modifiers.remove(m.getType());
            }
        }
	}
	
	public Collection<FactionModifier> getModifiers() {
	    List<FactionModifier> all = new ArrayList<>();
	    for (List<FactionModifier> list : modifiers.values()) {
	        all.addAll(list);
	    }
	    return all;
	}
	
	public Collection<FactionModifier> getCombinedModifiers() {
	    Map<FactionModifiers, Double> combined = new HashMap<>();

	    for (List<FactionModifier> list : modifiers.values()) {
	        for (FactionModifier mod : list) {
	            combined.merge(mod.getType(), mod.getAmount(), Double::sum);
	        }
	    }

	    List<FactionModifier> result = new ArrayList<>();
	    for (Map.Entry<FactionModifiers, Double> entry : combined.entrySet()) {
	        double total = entry.getValue();
	        if (total != 0) {
	            result.add(new FactionModifier(entry.getKey(), total));
	        }
	    }

	    return result;
	}
	
	public FactionModifier getModifier(FactionModifiers m) {
	    List<FactionModifier> list = modifiers.get(m);
	    if (list == null || list.isEmpty()) return new FactionModifier(m, 0.0);

	    double totalAmount = 0;
	    for (FactionModifier mod : list) {
	        totalAmount += mod.getAmount();
	    }
	    return new FactionModifier(m, totalAmount);
	}

    public void newDay() {
        double armyCost = military.getTotalUpkeep();
		if(armyCost > 0 && getBank() == null){
			for(Regiment r : military.getRegiments()){
				if(r.isLevy()) continue;
				while(r.getCurrentSlots() > r.getFreeSlots()){
					r.sizeDecrease();
				}
			}
		}
		while(armyCost > 0 && getBank().getWealth() < armyCost) {
			for(Regiment r : military.getRegiments()) {
				if(r.isLevy()) continue;
				if(r.getCurrentSlots() > r.getFreeSlots()) {
					r.sizeDecrease();
					break;
				}
			}
			armyCost = military.getTotalUpkeep();
		}
		if(armyCost > 0) {
			getBank().withdraw(armyCost);
		}
		provinceCap();
    }

	public void provinceCap() {
		if(TitleManager.overProvinceCap(this) && provinces.size() > 0) {
			int toRemove = provinces.get(provinces.size() - 1);
			if(toRemove == capital && provinces.size() > 1) toRemove = provinces.get(provinces.size() - 2);
			if(toRemove == capital) return;
			removeProvince(toRemove);
		}
	}

	public int numOnline() {
		int count = 0;
		for(String m : guildHandler.getAllMembers()){
			Player p = Bukkit.getPlayerExact(m);
			if(p != null && p.isOnline()) count++;
		}
		return count;
	}
}
