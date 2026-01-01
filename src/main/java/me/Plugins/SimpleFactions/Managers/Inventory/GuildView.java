package me.Plugins.SimpleFactions.Managers.Inventory;

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
import me.Plugins.SimpleFactions.enums.MenuItemType;
import me.Plugins.SimpleFactions.enums.SFGUI;
import me.Plugins.SimpleFactions.keys.Keys;

public class GuildView {
	public InventoryManager inv;
	
	public GuildCreator creator = new GuildCreator();
	
	public GuildView(InventoryManager inv) {
		this.inv = inv;
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
		if(guild.isLeader(player) && !guild.isBase()) i.setItem(19, creator.createMenuItem(player, guild, MenuItemType.BANNER_RANDOM));
		i.setItem(11, creator.createMenuItem(player, guild, MenuItemType.LEADER));
		i.setItem(12, creator.createMenuItem(player, guild, MenuItemType.WEALTH));
		i.setItem(14, creator.createMenuItem(player, guild, MenuItemType.MEMBERS));
		int group = 0;
		while(guild.getBranch(group) != null || group > 10) {
			Branch b = guild.getBranch(group);
			group++;
			i.setItem(group+28, creator.createBranchItem(player, guild, b));
		}
	}

	public void click(InventoryClickEvent e, Inventory inventory, Player p) {
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
				guildView(p, guild, inventory);
			}
		}
	}
}
