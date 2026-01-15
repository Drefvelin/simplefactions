package me.Plugins.SimpleFactions.Guild;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
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
import me.Plugins.SimpleFactions.Loaders.BranchLoader;
import me.Plugins.SimpleFactions.Loaders.GuildLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Bank;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.Modifier;
import me.Plugins.SimpleFactions.REST.RestServer;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.Utils.RandomRGB;
import me.Plugins.SimpleFactions.enums.GuildModifier;
import me.Plugins.SimpleFactions.enums.Stance;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class Guild {
    private Formatter format = new Formatter();

    private final Faction host;

    private String id;
    private String name;
    private String leader;
    private String rgb;
    private final GuildType type;
    private List<String> members = new ArrayList<>();
    private List<String> invites = new ArrayList<>();
    private Map<Integer, Branch> branches = new HashMap<>();
    private Bank bank;

    private Ledger ledger;

    private Double wealth;

    private ItemStack banner;
	private List<String> bannerPatterns = new ArrayList<>();

    private int capital = -1;

	private List<Modifier> wealthModifiers = new ArrayList<>();

    private TradeBreakdown breakdown = new TradeBreakdown();

    private Stance stance;

    public Guild(Faction f) {
        host = f;
        id = f.getId();
        rgb = RandomRGB.similarButDistinct(f.getRGB());
        stance = Stance.NEUTRAL;
        while(!RandomRGB.isFree(rgb)) {
            rgb = RandomRGB.similarButDistinct(f.getRGB());
        }
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
        this.ledger = new Ledger(this);
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
        f.getOrCreateMainGuild().kick(p.getName()); //remove from main guild
        this.ledger = new Ledger(this);
        createBanner();
    }

    public Guild(
        String id,
        String name,
        String leader,
        String rgb,
        int capital,
        String type,
        List<String> members,
        List<Branch> branchList,
        List<String> patterns,
        List<Modifier> wealthModifiers,
        Faction host,
        Stance stance
    ) {
        this.type = GuildLoader.getByString(type);
        this.host = host;
        this.id = id;
        this.name = name;
        this.leader = leader;
        this.rgb = rgb;
        this.capital = capital;
        this.stance = stance;
        this.members = members != null ? members : new ArrayList<>();
        for(Branch b : branchList) {
            this.branches.put(b.getGroup(), b);
        }
        int group = 0;
        while(group < 10) {
            if(!this.branches.containsKey(group)) {
                Branch b = BranchLoader.getByGroup(this, group);
                if(b != null) this.branches.put(group, new Branch(b, 0));
            }
            group++;
        }
        this.bannerPatterns = patterns;
        this.wealth = 0.0;
        this.wealthModifiers = wealthModifiers;
        this.ledger = new Ledger(this);
        createBanner();
    }
    public Ledger getLedger() {
        return ledger;
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
		ItemStack item = new ItemStack(Material.valueOf(bannerPatterns.get(0).split("\\.")[0].toUpperCase()+"_BANNER"), 1);
		BannerMeta b = (BannerMeta) item.getItemMeta();
		b.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		for(int i = 1; i < bannerPatterns.size(); i++) {
			String p = bannerPatterns.get(i);
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
        return amount;
    }

    public TradeBreakdown getTradeBreakdown() { return breakdown; }

    public void newDay() {
        if(bank != null) {
            bank.deposit(breakdown.getIncome());
        }
    }

    public double getMemberPercentage() {
        return (double)members.size()/(double)host.getMembers().size();
    }

    public double getStabilityEffect() {
        double effect = 0;
        double percentage = getMemberPercentage();
        effect += 30*Math.min(percentage, 1.0);
        double wealthPercentage = wealth / host.getWealth();
        effect += 20*Math.min(wealthPercentage, 1.0);
        double tradePercentage = breakdown.getTradePower() / host.getGuildHandler().getTotalTradePower();
        effect += 40*Math.min(tradePercentage, 1.0);
        return effect;
    }

    public double getStabilityModifier() {
        double stability = getStabilityEffect();
        if(isBase()) return stability;
        if(stance == Stance.NEUTRAL) {
            stability *= 0.35;
        } else if(stance == Stance.OPPOSE) {
            stability *= -1;
        }
        return stability;
    }

    public Stance getStance() {
        if(isBase()) {
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
}
