package me.Plugins.SimpleFactions.Guild;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Guild.Branch.Branch;
import me.Plugins.SimpleFactions.Guild.income.Ledger;
import me.Plugins.SimpleFactions.Guild.income.TradeBreakdown;
import me.Plugins.SimpleFactions.Guild.loans.LoanHandler;
import me.Plugins.SimpleFactions.Guild.upgrade.Upgrade;
import me.Plugins.SimpleFactions.Guild.upgrade.UpgradeExpansion;
import me.Plugins.SimpleFactions.Loaders.BranchLoader;
import me.Plugins.SimpleFactions.Loaders.GuildLoader;
import me.Plugins.SimpleFactions.Loaders.RelationLoader;
import me.Plugins.SimpleFactions.Loaders.UpgradeLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.Objects.Bank;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.FactionModifier;
import me.Plugins.SimpleFactions.Objects.Modifier;
import me.Plugins.SimpleFactions.REST.RestServer;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Army.MilitaryExpansion;
import me.Plugins.SimpleFactions.Database.Database;
import me.Plugins.SimpleFactions.Database.GuildBranchData;
import me.Plugins.SimpleFactions.Database.GuildData;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.Utils.RandomRGB;
import me.Plugins.SimpleFactions.enums.GuildModifier;
import me.Plugins.SimpleFactions.enums.Rules;
import me.Plugins.SimpleFactions.enums.SFGUI;
import me.Plugins.SimpleFactions.enums.Stance;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class Guild {
    private Formatter format = new Formatter();

    private Faction host;

    private String id;
    private String name;
    private String leader;
    private String rgb;
    private GuildType type;
    private List<String> members = new ArrayList<>();
    private List<String> invites = new ArrayList<>();
    private Map<Integer, Branch> branches = new HashMap<>();
    private Map<String, Upgrade> upgrades = new LinkedHashMap<>();
    private Bank bank;

    private Ledger ledger;

    private Double wealth;

    private ItemStack banner;
	private List<String> bannerPatterns = new ArrayList<>();

    private int capital = -1;

	private List<Modifier> wealthModifiers = new ArrayList<>();

    private TradeBreakdown breakdown = new TradeBreakdown();

    private Stance stance;

    private List<UpgradeExpansion> upgradeQueue = new ArrayList<>();

    private final LoanHandler loanHandler;

    public Guild(Faction f) {
        host = f;
        id = f.getId();
        rgb = RandomRGB.similarButDistinct(f.getRGB());
        stance = Stance.NEUTRAL;
        while(!RandomRGB.isFree(rgb)) {
            rgb = RandomRGB.similarButDistinct(f.getRGB());
        }
        this.bank = new Bank(this);
        bannerPatterns = f.getBannerPatterns();
        name = f.getName();
        leader = f.getLeader();
        members.add(leader);
        type = GuildLoader.getBaseType();
        this.wealth = 0.0;
        int group = 0;
        while(BranchLoader.getByGroup(this, group) != null) {
            branches.put(group, new Branch(BranchLoader.getByGroup(this, group), 0));
            group++;
        }
        for(Upgrade u : UpgradeLoader.getList()) {
            upgrades.put(u.getId(), new Upgrade(u, 0));
        }
        this.ledger = new Ledger(this);
        this.loanHandler = new LoanHandler(this);
        createBanner();
    }

    public Guild(String id, Player p, Faction f, int province) {
        host = f;
        this.id = format.formatId(id);
		this.name = StringFormatter.formatHex(format.formatName(id));
        this.leader = p.getName();
        rgb = RandomRGB.random();
        stance = Stance.NEUTRAL;
        while(!RandomRGB.isFree(rgb)) {
            rgb = RandomRGB.random();
        }
        this.bank = new Bank(this);
        this.bannerPatterns = RestServer.fetchBannerList();
        this.members.add(leader);
        this.type = GuildLoader.getDefaultType();
        this.capital = province;
        this.wealth = 0.0;
        int group = 0;
        while(BranchLoader.getByGroup(this, group) != null) {
            branches.put(group, new Branch(BranchLoader.getByGroup(this, group), 0));
            group++;
        }
        for(Upgrade u : UpgradeLoader.getList()) {
            upgrades.put(u.getId(), new Upgrade(u, 0));
        }
        f.getOrCreateMainGuild().kick(p.getName()); //remove from main guild
        this.ledger = new Ledger(this);
        this.loanHandler = new LoanHandler(this);
        createBanner();
    }

    public Guild(GuildData data, Faction host) {
        this.type = GuildLoader.getByString(data.type);
        this.host = host;
        this.id = data.id;
        this.name = data.name;
        this.leader = data.leader;
        this.rgb = data.rgb;
        this.capital = data.capital;
        this.stance = data.stance != null ? Stance.valueOf(data.stance) : Stance.NEUTRAL;
        this.members = data.members != null ? data.members : new ArrayList<>();
        if(!this.members.contains(leader)) this.members.add(leader);
        for (GuildBranchData bd : data.branches) {
            Branch base = BranchLoader.getByString(bd.id);
            if (base != null) {
                branches.put(base.getGroup(),
                    new Branch(base, bd.level.intValue())
                );
            }
        }
        int group = 0;
        while(group < 10) {
            if(!this.branches.containsKey(group)) {
                Branch b = BranchLoader.getByGroup(this, group);
                if(b != null) this.branches.put(group, new Branch(b, 0));
            }
            group++;
        }
        if(data.upgrades != null) {
            for (GuildBranchData bd : data.upgrades) {
                Upgrade base = UpgradeLoader.getByString(bd.id);
                if (base != null) {
                    upgrades.put(base.getId(),
                        new Upgrade(base, bd.level.intValue())
                    );
                }
            }
        }
        for(Upgrade u : UpgradeLoader.getList()) {
            if(!upgrades.containsKey(u.getId())) upgrades.put(u.getId(), new Upgrade(u, 0));
        }
        this.bannerPatterns = data.banner;
        this.wealth = 0.0;
        this.wealthModifiers = Database.loadModifiers(data.wealthModifiers);
        this.ledger = new Ledger(this);
        this.loanHandler = new LoanHandler(this, data.creditScore == null ? 50 : data.creditScore);
        createBanner();
    }

    public void relocate(Faction f, int newCapital) {
        if(isBase()) return;
        if(f.getId().equalsIgnoreCase(host.getId())) return;
        Faction origin = FactionManager.getByString(this.host.getId());
        if(origin != null) {
            origin.getGuildHandler().removeGuild(id);
        }
        f.getGuildHandler().addGuild(this);
        this.host = f;
        if(newCapital != -1) {
            setCapital(newCapital);
        }
        
    }

    public void tick() {
		if(upgradeQueue.size() == 0) return;
		UpgradeExpansion e = upgradeQueue.get(0);
		e.tick();
		if(e.getTimeLeft() != 0) return;
		upgradeQueue.remove(0);
		e.getUpgrade().levelUp();
		FactionManager.getInv().getUpdater().inventorySound("minecraft:block.note_block.chime", SFGUI.UPGRADE_VIEW);
	}

    public Ledger getLedger() {
        return ledger;
    }
    
    public LoanHandler getLoanHandler() {
        return loanHandler;
    }

    public void dummify(Player p) {
        for(int i = 0; i<members.size(); i++) {
            String member = members.get(i);
            if(member.contains("dummy")) continue;
            String dummy = "dummy_";
            int x = 1;
            while(FactionManager.getGuildByMember((dummy+x)) != null) {
                x++;
            }
            dummy = dummy+x;
            members.set(i, dummy);
            p.sendMessage("§a"+dummy+" replaced "+member);
            if(isLeader(member)) {
                p.sendMessage("§a"+dummy+" became leader");
                if(isBase()) host.setLeader(dummy);
                else setLeader(dummy);
            }
        }
    }
    public void dummyLeader(Player p) {
        String dummy = "dummy_";
        int x = 1;
        while(FactionManager.getGuildByMember((dummy+x)) != null) {
            x++;
        }
        dummy = dummy+x;
        addMember(dummy);
        if(isBase()) host.setLeader(dummy);
        else setLeader(dummy);
        p.sendMessage("§a"+dummy+" became leader");
    }
    public boolean isBase() { return type.isBase(); }
    public Faction getFaction() { return host; }
    public List<String> getInvites() { return invites; }
    public boolean isInvited(String p) {
        return invites.contains(p);
    }
    public void invite(String p) {
        if(!invites.contains(p)) invites.add(p);
    }
    public String getId() { return id; }
    public String getName() { return isBase() ? host.getName() : name; }
    public List<String> getMembers() { return members; }
    public boolean isMember(String p) { return members.contains(p); }
    public boolean isMember(Player p) { return isMember(p.getName()); }
    public void addMember(String p) {
        if(isMember(p)) return;
        if(!isBase() && host.getOrCreateMainGuild().isMember(p)) {
            host.getOrCreateMainGuild().kick(p);
        } 
        if(isInvited(p)) invites.remove(p);
        members.add(p);
    }
    public void kick(String member) {
        members.remove(member);
    }
    public String getLeader() { return isBase() ? host.getLeader() : leader; }
    public void setLeader(String leader) {
        this.leader = leader;
    }
    public boolean isLeader(String p) {
        return leader.equalsIgnoreCase(p);
    }
    public boolean isLeader(Player p) { return isLeader(p.getName()); }
    public Map<Integer, Branch> getBranches() { return branches; }
    public Branch getBranch(int i) {
        return branches.getOrDefault(i, null);
    }
    public Branch getBranch(String id) {
        for(Branch b : branches.values()) {
            if(b.getId().equalsIgnoreCase(id)) return b;
        }
        return null;
    }
    public GuildType getType() { return type; }
    public int getCapital() {
        return isBase() ? host.getCapital() : capital;
    }
    public boolean hasCapital() {
        if(isBase()) return host.hasCapital();
		return capital != -1;
	}
    public void setCapital(int i) {
        capital = i;
        SimpleFactions.getInstance().getProvinceManager().recalculateForSingleGuild(this, true);
    }
    public String getRGB() {
        return isBase() ? host.getRGB() : rgb;
    }
    public void setRGB(String rgb) {
        this.rgb = rgb;
    }

    private void createBanner() {
		ItemStack item = new ItemStack(
			Material.valueOf(bannerPatterns.get(0).split("\\.")[0].toUpperCase() + "_BANNER"),
			1
		);

		BannerMeta meta = (BannerMeta) item.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
		for (int i = 1; i < bannerPatterns.size(); i++) {
			String p = bannerPatterns.get(i);
			String[] split = p.split("\\.");

			if (split.length != 2) continue;

			String colourName = split[0].toUpperCase();
			String patternName = split[1].toLowerCase();

			DyeColor dye;
			try {
				dye = DyeColor.valueOf(colourName);
			} catch (IllegalArgumentException e) {
				Bukkit.getLogger().warning("Invalid dye color: " + colourName);
				continue;
			}

			PatternType patternType = null;

			// 1️⃣ Try vanilla (minecraft namespace)
			NamespacedKey vanillaKey = NamespacedKey.minecraft(patternName);
			patternType = Registry.BANNER_PATTERN.get(vanillaKey);

			// 2️⃣ Try custom namespace (tfmc)
			if (patternType == null) {
				NamespacedKey customKey = new NamespacedKey("tfmc", patternName);
				patternType = Registry.BANNER_PATTERN.get(customKey);
			}

			if (patternType == null) {
				Bukkit.getLogger().warning("Invalid banner pattern: " + patternName);
				continue;
			}

			meta.addPattern(new Pattern(dye, patternType));
		}
		item.setItemMeta(meta);
		
		this.banner = item;
	}

    public void setBanner(ItemStack banner) {
        if(isBase()) return;
		BannerMeta b = (BannerMeta) banner.getItemMeta();
		this.bannerPatterns.clear();
		this.bannerPatterns.add(banner.getType().toString().replace("_BANNER", ".BASE"));
		for(Pattern p : b.getPatterns()) {
			String colour = p.getColor().toString();
			String pattern = p.getPattern().toString();
			pattern = pattern.replace("tfmc:", "").toUpperCase();
			this.bannerPatterns.add(colour+"."+pattern);
		}
		createBanner();
	}
	public List<String> getBannerPatterns() {
		return isBase() ? host.getBannerPatterns() : bannerPatterns;
	}
	public void setBannerPatterns(List<String> bannerPatterns) {
		this.bannerPatterns = bannerPatterns;
		createBanner();
	}
    
    public ItemStack getBanner() {
        if(isBase()) return host.getBanner();
        return banner;
    }

    public Bank getBank() {
		return bank;
	}
	public void setBank(Bank bank) {
		this.bank = bank;
	}

    public void setName(String name) {
		this.name = name;
	}

    public Double getWealth() {
		return wealth;
	}
	public void setWealth(Double wealth) {
		this.wealth = wealth;
	}

    public void updateWealth() {
        if(bank == null) return;
		wealth = 0.0;
		addWealthModifier(new Modifier("Bank", bank.getWealth(), false));
        double spent = getTotalExpansionSpent();
        if(spent > 0) addWealthModifier(new Modifier("Expansions", spent, false));
		for(Modifier p : wealthModifiers) {
			wealth = wealth + p.getAmount();
		}
		wealth = format.formatDouble(wealth);
        host.updateWealth();
    }

    public List<Modifier> getWealthModifiers() {
		return wealthModifiers;
	}
	public void setWealthModifiers(List<Modifier> wealthModifiers) {
		this.wealthModifiers = wealthModifiers;
	}

    public void addWealthModifier(Modifier m) {
		for(int i = 0; i<wealthModifiers.size(); i++) {
			if(wealthModifiers.get(i).getType().equalsIgnoreCase(m.getType())) {
				wealthModifiers.set(i, m);
				return;
			}
		}
		wealthModifiers.add(m);
	}

    public void addPersistentWealthModifier(Modifier m) {
		for(int i = 0; i<wealthModifiers.size(); i++) {
			if(wealthModifiers.get(i).getType().equalsIgnoreCase(m.getType())) {
				m.setAmount(wealthModifiers.get(i).getAmount()+m.getAmount());
				if(Double.compare(m.getAmount(), 0) == 0) {
					wealthModifiers.remove(i);
					i--;
				} else {
					wealthModifiers.set(i, m);
				}
				return;
			}
		}
		if(m.getAmount() != 0) {
			wealthModifiers.add(m);
		}
	}

    public int getSize() {
        int size = 0;
        for(Branch b : branches.values()) {
            size += b.getLevel();
        }
        return size;
    }

    public double getExpansionCost() {
        int size = getSize();
        double baseCost = Cache.branchUpgradeCost;
        double cost = baseCost*Math.pow(Cache.branchUpgradeExponent, size);
        return Math.round(cost * 100.0) / 100.0;
    }

    public double getTotalExpansionSpent() {
        int size = getSize();
        if (size <= 0) return 0.0;

        double baseCost = Cache.branchUpgradeCost;
        double r = Cache.branchUpgradeExponent;

        double total = baseCost * (Math.pow(r, size) - 1) / (r - 1);
        return Math.round(total * 100.0) / 100.0;
    }

    public double getRefund() {
        int size = getSize()-1;
        double baseCost = Cache.branchUpgradeCost;
        double cost = baseCost*Math.pow(Cache.branchUpgradeExponent, size);
        return Math.round(cost * 100.0) / 100.0;
    }

    public double getModifier(GuildModifier m) {
        double amount = 0.0;
        for(Branch b : branches.values()) {
            amount += b.getAmount(m);
        }
        for(Upgrade u : upgrades.values()) {
            amount += u.getAmount(m);
        }
        return amount;
    }

    public TradeBreakdown getTradeBreakdown() { return breakdown; }
    public void setTradeBreakdown(TradeBreakdown breakdown) { this.breakdown = breakdown; }

    public void newDay() {
        if(bank != null) {
            bank.deposit(breakdown.getIncome());
        }
    }

    public double getMemberPercentage() {
        int totalMembers = host.getMembers().size() + host.getVassalMembers().size();
        if (totalMembers <= 0) {
            return 0.0;
        }
        return (double) members.size() / (double) totalMembers;
    }

    public double getStabilityEffect() {
        double effect = 0.0;

        // Member percentage
        double memberPercentage = getMemberPercentage();
        effect += 30.0 * Math.min(memberPercentage, 1.0);

        // Wealth percentage
        double totalWealth = host.getWealth() + host.getVassalWealth();
        if (totalWealth > 0) {
            double wealthPercentage = wealth / totalWealth;
            effect += 20.0 * Math.min(wealthPercentage, 1.0);
        }

        // Trade power percentage
        double totalTradePower =
                host.getGuildHandler().getTotalTradePower() + host.getVassalTradePower();
        if (totalTradePower > 0) {
            double tradePercentage = breakdown.getTradePower() / totalTradePower;
            effect += 40.0 * Math.min(tradePercentage, 1.0);
        }

        return effect;
    }

    public double getStabilityModifier(Faction f) {
        double stability = getStabilityEffect();
        if(f.getId().equalsIgnoreCase(host.getId()) && isBase()) return stability;
        if(stance == Stance.NEUTRAL) {
            stability *= 0.35;
        } else if(stance == Stance.OPPOSE) {
            stability *= -1;
        }
        if(isBase()) stability *= (host.getGovernment().getStability()/100.0);
        return stability;
    }

    public Stance getStance(Faction f) {
        if(f.getId().equalsIgnoreCase(host.getId()) && isBase()) {
            return Stance.SUPPORT;
        }
        return stance;
    }

    public void switchStance() {
        switch (stance) {
            case OPPOSE:
                stance = Stance.NEUTRAL;
                break;
            case NEUTRAL:
                stance = Stance.SUPPORT;
                break;
            case SUPPORT:
                stance = Stance.OPPOSE;
                break;
        }
    }

    //Upgrades
    public Upgrade getUpgrade(String id) {
        return upgrades.get(id);
    }

    public List<Upgrade> getUpgrades() {
        List<Upgrade> list = new ArrayList<>();
        for(Upgrade u : upgrades.values()) {
            if(u.isAllowed(type)) list.add(u);
        }
        return list;
    }

    public boolean hasUpgrades() {
        return !getUpgrades().isEmpty();
    }

    public List<UpgradeExpansion> getUpgradeQueue() {
        return upgradeQueue;
    }

    public void setUpgradeQueue(List<UpgradeExpansion> queue) {
        this.upgradeQueue = queue;
    }

    public void enqueueUpgrade(Upgrade u) {
        if(upgradeQueue.size() == 3) return;
        upgradeQueue.add(new UpgradeExpansion(u));
    }

    public void addQueuedUpgrade(Upgrade u, int time) {
        if(upgradeQueue.size() == 3) return;
        upgradeQueue.add(new UpgradeExpansion(u, time));
    }

    public double getUpgradesUpkeep() {
        double total = 0;
        for(Upgrade u : upgrades.values()) {
            total+=u.getTotalUpkeep();
        }
        return total;
    }

    public double getRelocationCost(int province) {
        Province prov = SimpleFactions.getInstance().getProvinceManager().get(province);
        double cost = getTotalExpansionSpent();
        if(prov == null) return -1;
        Faction owner = prov.getOwner();
        if(owner == null) return Math.max(cost *= 0.15, 100);
        if(owner.getId().equalsIgnoreCase(host.getId())) return Math.max(cost *= 0.05, 100);
        return Math.max(cost *= 0.15, 100);
    }

    public double getElevationCost() {
        double cost = getSize()*12;
        return 25+Math.pow(cost, 1.1);
    }

    public boolean canBeElevated(Player p) {
        if(isBase()) {
            if(p != null) p.sendMessage("§cCannot elevate the main guild");
            return false;
        }
        if(!host.hasFactionRule(Rules.CAN_HAVE_VASSALS)) {
            if(p != null) p.sendMessage("§cHost faction cannot have vassals");
            return false;
        }
        if(hasCapital() && capital == host.getCapital()) {
            if(p != null) p.sendMessage("§cGuild capital is the same as the faction capital, move it first");
            return false;
        }
        for(Guild g : host.getGuildHandler().getGuilds()) {
            if(g.isBase()) continue;
            if(g.getId().equalsIgnoreCase(id)) continue;
            if(g.getCapital() == capital) {
                if(p != null) p.sendMessage("§cAnother guild already has that capital, move it first");
                return false;
            }
        }
        return true;
    }

    public void convert(GuildType type) {
        if (this.type == GuildLoader.getBaseType()) {
            this.capital = getCapital();
            this.id = getId();
            this.leader = getLeader();
            this.members = new ArrayList<>(getMembers());
            this.invites = new ArrayList<>(getInvites());
            this.name = getName();
            this.bannerPatterns = new ArrayList<>(getBannerPatterns());
            this.rgb = getRGB();
            this.bank = getBank();
            this.ledger = getLedger();
            this.wealth = getWealth();
            this.wealthModifiers = new ArrayList<>(getWealthModifiers());
            this.breakdown = getTradeBreakdown();
            this.stance = Stance.NEUTRAL;
            this.upgradeQueue = new ArrayList<>(getUpgradeQueue());
            createBanner();
        }

        this.type = type;

        for (Branch b : branches.values()) {
            if (!b.isAllowed(type)) {
                branches.put(
                    b.getGroup(),
                    new Branch(BranchLoader.getByGroup(this, b.getGroup()), b.getLevel())
                );
            }
        }

        for (Upgrade u : upgrades.values()) {
            if (!u.isAllowed(type)) {
                u.setLevel(0);
            }
        }
    }

    public Faction elevate(boolean subjugate) {
        if(!canBeElevated(null)) return null;
        host.getGuildHandler().removeGuild(id);
        host.getProvinceHandler().removeProvince(capital, false);
        Faction elevated = new Faction(this);
        Faction old = host;
        host = elevated;
        elevated.getProvinceHandler().addProvince(capital);
        FactionManager.addFaction(elevated);
        if(subjugate) RelationManager.setRelation(null, RelationLoader.getElevationTarget(), elevated, old, false);
        return elevated;
    }
}
