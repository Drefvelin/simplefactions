package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Guild.Branch.Branch;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.REST.RestServer;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Utils.FactionRanker;
import me.Plugins.SimpleFactions.enums.MenuItemType;
import me.Plugins.SimpleFactions.enums.RankType;
import me.Plugins.SimpleFactions.enums.SFGUI;
import me.Plugins.SimpleFactions.keys.Keys;

public class GuildView {
	public InventoryManager inv;
	
	public GuildCreator creator = new GuildCreator();

	public static HashMap<Player, RankType> currentRanking = new HashMap<>();
	public static HashMap<Player, Integer> currentPage = new HashMap<>();

	private static final int INVENTORY_SIZE = 54;

	private static final List<Integer> RESERVED_SLOTS = List.of(
		8, 17, 26, 35, 44,
		45, // prev page
		53  // next page
	);

	private static final int PREV_PAGE_SLOT = 45;
	private static final int NEXT_PAGE_SLOT = 53;

	
	public GuildView(InventoryManager inv) {
		this.inv = inv;
	}

	public void guildList(Player p) {

		currentRanking.putIfAbsent(p, RankType.WEALTH);
		currentPage.putIfAbsent(p, 0);

		RankType rank = currentRanking.get(p);
		int page = currentPage.get(p);

		List<Guild> guilds = new FactionRanker().getRankedGuildList(rank);
		Collections.reverse(guilds);

		Inventory inv = SimpleFactions.plugin.getServer()
				.createInventory(null, INVENTORY_SIZE, "§7Guild List");

		// Build usable slots
		List<Integer> usableSlots = new ArrayList<>();
		for (int i = 0; i < INVENTORY_SIZE; i++) {
			if (!RESERVED_SLOTS.contains(i)) {
				usableSlots.add(i);
			}
		}

		int perPage = usableSlots.size();
		int start = page * perPage;
		int end = Math.min(start + perPage, guilds.size());

		for (int i = start; i < end; i++) {
			Guild g = guilds.get(i);
			inv.setItem(
				usableSlots.get(i - start),
				creator.createListItem(p, g)
			);
		}

		// Rank toggle
		inv.setItem(8, DefaultCreator.createRankButton(rank));

		// Page buttons
		if (page > 0)
			inv.setItem(PREV_PAGE_SLOT, DefaultCreator.createPreviousPageButton());

		if (end < guilds.size())
			inv.setItem(NEXT_PAGE_SLOT, DefaultCreator.createNextPageButton());

		p.openInventory(inv);
	}


	public void guildView(Player player, Guild guild) {
		Inventory i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(guild.getId(), SFGUI.GUILD_VIEW), 54, "§7Guild View");
		guildView(player, guild, i);
		player.openInventory(i);
	}

    public void guildView(Player player, Guild guild, Inventory i) {
		i.clear();
		if(guild.isMember(player)) i.setItem(1, creator.createMenuItem(player, guild, MenuItemType.BANNER_GET));
		i.setItem(10, creator.createMenuItem(player, guild, MenuItemType.BANNER));
		if(/*guild.isLeader(player) && */!guild.isBase()) i.setItem(19, creator.createMenuItem(player, guild, MenuItemType.BANNER_RANDOM));
		i.setItem(11, creator.createMenuItem(player, guild, MenuItemType.LEADER));
		i.setItem(12, creator.createMenuItem(player, guild, MenuItemType.WEALTH));
		i.setItem(14, creator.createMenuItem(player, guild, MenuItemType.MEMBERS));
		int group = 0;
		while(guild.getBranch(group) != null || group > 10) {
			Branch b = guild.getBranch(group);
			group++;
			i.setItem(group+28, creator.createBranchItem(player, guild, b));
		}
		i.setItem(13, creator.createMenuItem(player, guild, MenuItemType.TRADE_BREAKDOWN));
		i.setItem(53, inv.createBackButton(SFGUI.GUILD_VIEW));
	}

	public void click(InventoryClickEvent e, Inventory inventory, Player p) {
		if (e.getView().getTitle().equalsIgnoreCase("§7Guild List")) {
			e.setCancelled(true);

			// Next page
			if (e.getSlot() == NEXT_PAGE_SLOT) {
				currentPage.put(p, currentPage.getOrDefault(p, 0) + 1);
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				guildList(p);
				return;
			}

			// Previous page
			if (e.getSlot() == PREV_PAGE_SLOT) {
				currentPage.put(p, Math.max(0, currentPage.getOrDefault(p, 0) - 1));
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				guildList(p);
				return;
			}

			// Rank toggle
			if (e.getSlot() == 8) {
				RankType r = currentRanking.get(p);
				switch (r) {
					case WEALTH -> currentRanking.put(p, RankType.MEMBERS);
					case MEMBERS -> currentRanking.put(p, RankType.TRADE_POWER);
					case TRADE_POWER -> currentRanking.put(p, RankType.INCOME);
					default -> currentRanking.put(p, RankType.WEALTH);
				}
				currentPage.put(p, 0);
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				guildList(p);
				return;
			}

			// Open guild
			ItemStack item = e.getCurrentItem();
			if (item == null || !item.hasItemMeta()) return;

			String id = item.getItemMeta()
				.getPersistentDataContainer()
				.get(new NamespacedKey(SimpleFactions.plugin, "id"),
					PersistentDataType.STRING);

			if (id == null) return;

			Guild guild = FactionManager.getGuildByString(id);
			if (guild != null) {
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				guildView(p, guild);
			}
		}
		if(e.getView().getTitle().equalsIgnoreCase("§7Guild View")) {
			e.setCancelled(true);
			SFInventoryHolder h = (SFInventoryHolder) inventory.getHolder();
			Guild guild = FactionManager.getGuildByString(h.getId());
			if(e.getSlot() == 19) {
				if(!(inventory.getHolder() instanceof SFInventoryHolder)) return;
				guild.setBannerPatterns(RestServer.fetchBannerList());
				inventory.setItem(10, creator.createMenuItem(p, guild, MenuItemType.BANNER));
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			} else if(e.getSlot() == 1) {
				if(!(inventory.getHolder() instanceof SFInventoryHolder)) return;
				ItemStack i = new ItemStack(guild.getBanner());
				p.getInventory().addItem(i);
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			}
			ItemStack item = e.getCurrentItem();
			ItemMeta meta = item.getItemMeta();
			String data = meta.getPersistentDataContainer().get(Keys.BRANCH_ID, PersistentDataType.STRING);
			if(data != null ) {
				if(!guild.isLeader(p)) return;
				Branch b = guild.getBranch(data);
				double cost = guild.getExpansionCost();
				if(guild.getBank().getWealth() < cost) {
					p.sendMessage("§cCannot afford to upgrade");
					p.playSound(p, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
					return;
				}
				b.levelUp();
				p.sendMessage("§aUpgraded "+b.getName()+ "§a to level §e"+b.getLevel());
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				guild.getBank().withdraw(cost);
				SimpleFactions.getInstance().getProvinceManager().recalculate();
				guildView(p, guild, inventory);
			}
		}
	}
}
