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

import me.Plugins.SimpleFactions.Guild.Branch.Branch;
import me.Plugins.SimpleFactions.Loaders.BranchLoader;
import me.Plugins.SimpleFactions.Loaders.GuildLoader;
import me.Plugins.SimpleFactions.Objects.Bank;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.Modifier;
import me.Plugins.SimpleFactions.REST.RestServer;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.Utils.RandomRGB;
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

    private Double wealth;
	private Double prestige;

    private ItemStack banner;
	private List<String> bannerPatterns = new ArrayList<>();

    private int capital = -1;

	private List<Modifier> wealthModifiers = new ArrayList<>();

    public Guild(Faction f) {
        host = f;
        id = f.getId();
        rgb = RandomRGB.similarButDistinct(f.getRGB());
        while(!RandomRGB.isFree(rgb)) {
            rgb = RandomRGB.similarButDistinct(f.getRGB());
        }
        bannerPatterns = f.getBannerPatterns();
        name = f.getName();
        leader = f.getLeader();
        members.add(leader);
        type = GuildLoader.getBaseType();
        this.wealth = 0.0;
		this.prestige = 0.0;
        int group = 0;
        while(BranchLoader.getByGroup(this, group) != null) {
            branches.put(group, new Branch(BranchLoader.getByGroup(this, group), 0));
            group++;
        }
        createBanner();
    }

    public Guild(String id, Player p, Faction f, int province) {
        host = f;
        this.id = format.formatId(id);
		this.name = StringFormatter.formatHex(format.formatName(id));
        this.leader = p.getName();
        rgb = RandomRGB.random();
        while(!RandomRGB.isFree(rgb)) {
            rgb = RandomRGB.random();
        }
        this.bannerPatterns = RestServer.fetchBannerList();
        this.members.add(leader);
        this.type = GuildLoader.getDefaultType();
        this.capital = province;
        this.wealth = 0.0;
		this.prestige = 0.0;
        int group = 0;
        while(BranchLoader.getByGroup(this, group) != null) {
            branches.put(group, new Branch(BranchLoader.getByGroup(this, group), 0));
            group++;
        }
        f.getOrCreateMainGuild().kick(p.getName());
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
        Faction host
    ) {
        this.type = GuildLoader.getByString(type);
        this.host = host;
        this.id = id;
        this.name = name;
        this.leader = leader;
        this.rgb = rgb;
        this.capital = capital;
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
		this.prestige = 0.0;
        this.wealthModifiers = wealthModifiers;
        createBanner();
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
    public GuildType getType() { return type; }
    public int getCapital() {
        return isBase() ? host.getCapital() : capital;
    }
    public void setCapital(int i) {
        capital = i;
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
			this.bannerPatterns.add(colour+"."+pattern);
		}
		createBanner();
	}
	public List<String> getBannerPatterns() {
		return bannerPatterns;
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
		addWealthModifier(new Modifier("Bank", bank.getWealth()));
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
}
