package me.Plugins.SimpleFactions.Managers.Inventory;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.Holder.CampaignInventoryHolder;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.WarEndReason;
import me.Plugins.SimpleFactions.War.enums.WarType;
import me.Plugins.SimpleFactions.War.progression.BelligerentRole;
import me.Plugins.SimpleFactions.War.progression.CampaignProgressionService;
import me.Plugins.SimpleFactions.War.progression.CampaignRouteRenderer;
import me.Plugins.SimpleFactions.War.progression.WhitePeaceService;
import me.Plugins.SimpleFactions.War.schedule.BattleAutoresolveService;
import me.Plugins.SimpleFactions.War.schedule.BattleHourTally;
import me.Plugins.SimpleFactions.War.schedule.BattleScheduleLookups;
import me.Plugins.SimpleFactions.War.schedule.BattleVoteService;
import me.Plugins.SimpleFactions.War.schedule.BattleVoteToggleResult;
import me.Plugins.SimpleFactions.War.schedule.BattleVoterEligibility;
import me.Plugins.SimpleFactions.War.schedule.BattleWindowService;
import me.Plugins.SimpleFactions.enums.SFGUI;

public class CampaignView {
	private static final int ROUTE_SLOTS_PER_PAGE = 9;
	private static final int ROUTE_START_SLOT = 10;
	private static final int PREV_PAGE_SLOT = 9;
	private static final int NEXT_PAGE_SLOT = 19;

	public InventoryManager inv;
	public CampaignCreator creator = new CampaignCreator();

	public CampaignView(InventoryManager inv) {
		this.inv = inv;
	}

	public void campaignView(Player player, War war, int routePage, boolean open) {
		if (war == null || !war.isActive() || war.getWarType() == WarType.RAID) {
			player.sendMessage("§cThis war has no campaign view.");
			return;
		}
		List<Integer> axis = war.getCampaignProvinces();
		if (axis == null || axis.isEmpty()) {
			player.sendMessage("§cThis war has no campaign route.");
			return;
		}

		Faction viewerFaction = FactionManager.getByLeader(player.getName());
		if (viewerFaction == null) {
			viewerFaction = FactionManager.getByMember(player.getName());
		}
		if (viewerFaction == null) {
			player.sendMessage("§cYou are not in a faction.");
			return;
		}

		int maxPage = Math.max(0, (axis.size() - 1) / ROUTE_SLOTS_PER_PAGE);
		int page = Math.max(0, Math.min(routePage, maxPage));

		Inventory inventory = SimpleFactions.plugin.getServer().createInventory(
				new CampaignInventoryHolder(war.getId(), page, SFGUI.CAMPAIGN_VIEW),
				54,
				war.getName() + " §7Campaign");

		for (int slot : Arrays.asList(0, 1, 2, 6, 7, 8, 45, 46, 47, 52)) {
			inventory.setItem(slot, inv.getFiller(Material.GRAY_STAINED_GLASS_PANE));
		}

		inventory.setItem(4, creator.createInfoItem(
				war,
				viewerFaction,
				player.getUniqueId(),
				BattleScheduleLookups.uuidToFactionForWar(war)));

		populateHourToggles(inventory, war, viewerFaction, player.getUniqueId());

		int axisOffset = page * ROUTE_SLOTS_PER_PAGE;
		for (int i = 0; i < ROUTE_SLOTS_PER_PAGE; i++) {
			int axisIndex = axisOffset + i;
			int slot = ROUTE_START_SLOT + i;
			if (axisIndex >= axis.size()) {
				inventory.setItem(slot, new ItemStack(Material.AIR));
				continue;
			}
			int provinceId = axis.get(axisIndex);
			inventory.setItem(slot, creator.createRouteProvinceItem(war, viewerFaction, provinceId, axisIndex));
		}

		if (page > 0) {
			inventory.setItem(PREV_PAGE_SLOT, creator.createPageButton("Previous page", war.getId(), page - 1));
		}
		if (page < maxPage) {
			inventory.setItem(NEXT_PAGE_SLOT, creator.createPageButton("Next page", war.getId(), page + 1));
		}

		if (canAcceptWhitePeace(player, war)) {
			inventory.setItem(48, creator.createAcceptPeaceButton(war));
		} else {
			inventory.setItem(48, inv.getFiller(Material.GRAY_STAINED_GLASS_PANE));
		}

		populateAutoresolveButtons(inventory, war, player);

		inventory.setItem(53, inv.createBackButton(SFGUI.WAR_VIEW));
		if (open) {
			player.openInventory(inventory);
		}
	}

	private void populateHourToggles(Inventory inventory, War war, Faction viewerFaction, UUID viewerUuid) {
		boolean eligible = BattleVoterEligibility.isEligibleVoter(war, viewerFaction);
		Set<Integer> selections = BattleVoteService.getPlayerSelections(war, viewerUuid);
		var uuidToFaction = BattleScheduleLookups.uuidToFactionForWar(war);
		var hourTally = BattleVoteService.buildHourTally(war, uuidToFaction);
		for (BattleVoterEligibility.HourSlotEntry entry : BattleVoterEligibility.hourSlotLayout(
				BattleWindowService.listValidHours())) {
			if (entry.hour() == null) {
				inventory.setItem(entry.slot(), creator.createUnusedHourSlotItem());
				continue;
			}
			boolean selected = selections.contains(entry.hour());
			BattleHourTally tally = hourTally.getOrDefault(entry.hour(), new BattleHourTally(0, 0));
			inventory.setItem(
					entry.slot(),
					creator.createHourToggleItem(war, entry.hour(), selected, eligible, tally));
		}
	}

	private void populateAutoresolveButtons(Inventory inventory, War war, Player player) {
		inventory.setItem(51, inv.getFiller(Material.GRAY_STAINED_GLASS_PANE));
		if (war.getBattleSchedulePhase() != BattleSchedulePhase.VOTING
				|| !BattleAutoresolveService.canProposeAutoresolveNow(war, Instant.now())) {
			inventory.setItem(49, inv.getFiller(Material.GRAY_STAINED_GLASS_PANE));
			inventory.setItem(50, inv.getFiller(Material.GRAY_STAINED_GLASS_PANE));
			return;
		}
		if (isAttackerLeader(player, war)) {
			inventory.setItem(49, creator.createAutoresolveProposeButton(war, BelligerentRole.ATTACKER));
		} else {
			inventory.setItem(49, inv.getFiller(Material.GRAY_STAINED_GLASS_PANE));
		}
		if (isDefenderLeader(player, war)) {
			inventory.setItem(50, creator.createAutoresolveProposeButton(war, BelligerentRole.DEFENDER));
		} else {
			inventory.setItem(50, inv.getFiller(Material.GRAY_STAINED_GLASS_PANE));
		}
	}

	public void click(InventoryClickEvent e, Inventory inventory, Player player) {
		if (!(inventory.getHolder() instanceof CampaignInventoryHolder holder)) {
			return;
		}
		if (holder.getType() != SFGUI.CAMPAIGN_VIEW) {
			return;
		}
		e.setCancelled(true);

		War war = WarManager.getById(holder.getWarId());
		if (war == null || !war.isActive()) {
			player.sendMessage("§cWar not found.");
			return;
		}

		int slot = e.getSlot();
		if (slot == 53) {
			inv.warView(null, player, war, true);
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			return;
		}

		ItemStack clicked = e.getCurrentItem();
		if (clicked == null || clicked.getItemMeta() == null) {
			return;
		}
		ItemMeta meta = clicked.getItemMeta();

		Integer pageWarId = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_page_war"),
				PersistentDataType.INTEGER);
		Integer page = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_page"),
				PersistentDataType.INTEGER);
		if (pageWarId != null && page != null && pageWarId == war.getId()) {
			campaignView(player, war, page, true);
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			return;
		}

		Integer voteWarId = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_vote_war"),
				PersistentDataType.INTEGER);
		Integer voteHour = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_vote_hour"),
				PersistentDataType.INTEGER);
		if (voteWarId != null && voteHour != null && voteWarId == war.getId()) {
			handleHourToggleClick(player, war, voteHour, holder.getRoutePage());
			return;
		}

		Integer autoresolveWarId = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_autoresolve_war"),
				PersistentDataType.INTEGER);
		String autoresolveSide = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_autoresolve_side"),
				PersistentDataType.STRING);
		if (autoresolveWarId != null
				&& autoresolveSide != null
				&& autoresolveWarId == war.getId()) {
			handleAutoresolveClick(player, war, BelligerentRole.valueOf(autoresolveSide), holder.getRoutePage());
			return;
		}

		Integer acceptWarId = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_accept_peace"),
				PersistentDataType.INTEGER);
		if (acceptWarId != null && acceptWarId == war.getId()) {
			if (!canAcceptWhitePeace(player, war)) {
				player.sendMessage("§cYou cannot accept white peace right now.");
				return;
			}
			Faction leader = FactionManager.getByLeader(player.getName());
			if (leader == null) {
				return;
			}
			inv.confirming.put(player, leader);
			inv.campaignConfirmWar.put(player, war.getId());
			inv.confirmView(player, leader, "campaign_accept_peace", String.valueOf(war.getId()));
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			return;
		}

		Integer holdWarId = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_hold"),
				PersistentDataType.INTEGER);
		if (holdWarId != null && holdWarId == war.getId()) {
			if (!isDefenderLeader(player, war) || !creator.isDefenderChoiceActive(war)) {
				player.sendMessage("§cYou cannot hold the front right now.");
				return;
			}
			Faction leader = FactionManager.getByLeader(player.getName());
			if (leader == null) {
				return;
			}
			inv.confirming.put(player, leader);
			inv.campaignConfirmWar.put(player, war.getId());
			inv.confirmView(player, leader, "campaign_hold", String.valueOf(war.getId()));
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			return;
		}

		Integer counterWarId = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_counter"),
				PersistentDataType.INTEGER);
		if (counterWarId != null && counterWarId == war.getId()) {
			if (!isDefenderLeader(player, war) || !creator.isDefenderChoiceActive(war)) {
				player.sendMessage("§cYou cannot counter-push right now.");
				return;
			}
			Faction leader = FactionManager.getByLeader(player.getName());
			if (leader == null) {
				return;
			}
			inv.confirming.put(player, leader);
			inv.campaignConfirmWar.put(player, war.getId());
			inv.confirmView(player, leader, "campaign_counter", String.valueOf(war.getId()));
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			return;
		}

		Integer provinceId = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_province"),
				PersistentDataType.INTEGER);
		if (provinceId != null && isDefenderLeader(player, war) && creator.isDefenderChoiceActive(war)) {
			List<Integer> choices = CampaignRouteRenderer.actionProvinceIds(war);
			if (choices.size() != 2 || !choices.contains(provinceId)) {
				return;
			}
			int frontProvince = war.getCampaignProvinces().get(war.getCursorIndex());
			Faction leader = FactionManager.getByLeader(player.getName());
			if (leader == null) {
				return;
			}
			inv.confirming.put(player, leader);
			inv.campaignConfirmWar.put(player, war.getId());
			if (provinceId == frontProvince) {
				inv.confirmView(player, leader, "campaign_hold", String.valueOf(war.getId()));
			} else {
				inv.confirmView(player, leader, "campaign_counter", String.valueOf(war.getId()));
			}
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
		}
	}

	private void handleHourToggleClick(Player player, War war, int hour, int routePage) {
		Faction faction = FactionManager.getByMember(player.getName());
		if (faction == null || !BattleVoterEligibility.isEligibleVoter(war, faction)) {
			player.sendMessage("§cYou cannot vote for battle hours right now.");
			return;
		}

		BattleVoteToggleResult result = BattleVoteService.toggleVote(
				war, player.getUniqueId(), hour, faction, player.isOnline());
		switch (result) {
			case ADDED -> player.sendMessage("§aAdded " + hour + ":00 UTC to your availability.");
			case REMOVED -> player.sendMessage("§aRemoved " + hour + ":00 UTC from your availability.");
			case REJECTED_INVALID_HOUR -> player.sendMessage("§cThat hour is not in the battle window.");
			case REJECTED_NOT_PARTICIPANT -> player.sendMessage("§cYou are not eligible to vote in this war.");
			case REJECTED_OFFLINE -> player.sendMessage("§cYou must be online to vote.");
		}

		if (result == BattleVoteToggleResult.ADDED || result == BattleVoteToggleResult.REMOVED) {
			WarManager.persist(war);
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			campaignView(player, war, routePage, true);
		}
	}

	private void handleAutoresolveClick(
			Player player,
			War war,
			BelligerentRole side,
			int routePage) {
		boolean allowed = switch (side) {
			case ATTACKER -> isAttackerLeader(player, war);
			case DEFENDER -> isDefenderLeader(player, war);
		};
		if (!allowed || !BattleAutoresolveService.canProposeAutoresolveNow(war, Instant.now())) {
			player.sendMessage("§cYou cannot propose autoresolve right now.");
			return;
		}
		switch (BattleAutoresolveService.sendProposeRequest(player, war, side)) {
			case SENT -> {
				player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				campaignView(player, war, routePage, true);
			}
			case OPPOSING_LEADER_OFFLINE -> player.sendMessage("§cThe opposing war leader is not online.");
			case NOT_ALLOWED -> player.sendMessage("§cYou cannot propose autoresolve right now.");
		}
	}

	public void handleConfirm(Player player, String key, String data, boolean confirmed) {
		Integer warId = inv.campaignConfirmWar.remove(player);
		inv.confirming.remove(player);
		if (warId == null) {
			return;
		}
		War war = WarManager.getById(warId);
		if (war == null || !war.isActive()) {
			player.sendMessage("§cWar not found.");
			return;
		}
		if (!confirmed) {
			campaignView(player, war, 0, true);
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			return;
		}

		Faction leader = FactionManager.getByLeader(player.getName());
		if (leader == null) {
			return;
		}

		switch (key) {
			case "campaign_hold" -> {
				if (!isDefenderLeader(player, war) || !CampaignProgressionService.applyDefenderHold(war)) {
					player.sendMessage("§cCould not hold the front.");
					return;
				}
				applyRecalcAndPersist(player, war);
				player.sendMessage("§aFront held. No counter-offensive.");
			}
			case "campaign_counter" -> {
				if (!isDefenderLeader(player, war) || !CampaignProgressionService.applyDefenderCounterPush(war)) {
					player.sendMessage("§cCould not start counter-push.");
					return;
				}
				applyRecalcAndPersist(player, war);
				player.sendMessage("§aCounter-push started.");
			}
			case "campaign_accept_peace" -> {
				if (!WhitePeaceService.acceptWhitePeace(war, leader)) {
					player.sendMessage("§cCould not accept white peace.");
					return;
				}
				WarManager.endWar(war, WarEndReason.WHITE_PEACE);
				player.sendMessage("§aWhite peace accepted.");
				inv.warList(player);
				player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
				return;
			}
			default -> {
				return;
			}
		}
		player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
		campaignView(player, war, 0, true);
	}

	private void applyRecalcAndPersist(Player player, War war) {
		WhitePeaceService.recalculateProposals(war).ifPresent(reason -> WarManager.endWar(war, reason));
		if (WarManager.getById(war.getId()) != null) {
			WarManager.persist(war);
		} else {
			inv.warList(player);
		}
	}

	public boolean canAcceptWhitePeace(Player player, War war) {
		Faction leader = FactionManager.getByLeader(player.getName());
		if (leader == null) {
			return false;
		}
		return WhitePeaceService.acceptWhitePeace(war, leader);
	}

	public boolean isDefenderLeader(Player player, War war) {
		Faction leader = FactionManager.getByLeader(player.getName());
		return leader != null && leader.getId().equalsIgnoreCase(war.getDefenderLeaderId());
	}

	public boolean isAttackerLeader(Player player, War war) {
		Faction leader = FactionManager.getByLeader(player.getName());
		return leader != null && leader.getId().equalsIgnoreCase(war.getAttackerLeaderId());
	}
}
