package me.Plugins.SimpleFactions.Managers.Inventory;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.WarType;
import me.Plugins.SimpleFactions.War.campaign.progression.BelligerentRole;
import me.Plugins.SimpleFactions.War.campaign.progression.CampaignRouteEntry;
import me.Plugins.SimpleFactions.War.campaign.progression.CampaignRouteRenderer;
import me.Plugins.SimpleFactions.War.campaign.progression.CampaignChoiceService;
import me.Plugins.SimpleFactions.War.campaign.progression.CampaignPostBattleChoiceService;
import me.Plugins.SimpleFactions.War.campaign.progression.WhitePeaceService;
import me.Plugins.SimpleFactions.War.resolution.WarResolutionService;
import me.Plugins.SimpleFactions.War.campaign.runtime.BattleAutoresolveService;
import me.Plugins.SimpleFactions.War.campaign.vote.VoteResults.BattleHourTally;
import me.Plugins.SimpleFactions.War.campaign.runtime.BattleScheduleLookups;
import me.Plugins.SimpleFactions.War.campaign.vote.BattleVoteService;
import me.Plugins.SimpleFactions.War.campaign.vote.VoteResults.BattleVoteToggleResult;
import me.Plugins.SimpleFactions.War.campaign.vote.BattleVoterEligibility;
import me.Plugins.SimpleFactions.War.campaign.runtime.BattleWindowService;
import me.Plugins.SimpleFactions.enums.SFGUI;

public class CampaignView {
	private static final int ROUTE_SLOT_COUNT = 9;
	private static final int ROUTE_START_SLOT = 10;
	private static final int VOTING_HELP_SLOT = 27;

	public InventoryManager inv;
	public CampaignCreator creator = new CampaignCreator();

	public CampaignView(InventoryManager inv) {
		this.inv = inv;
	}

	public void campaignView(Player player, War war, boolean open) {
		if (war == null || !war.isActive() || war.getWarType() == WarType.RAID) {
			player.sendMessage("§cThis war has no campaign view.");
			return;
		}
		List<Integer> axis = war.getCampaignProvinces();
		if (axis == null || axis.isEmpty()) {
			player.sendMessage("§cThis war has no campaign route.");
			return;
		}
		List<CampaignRouteEntry> routeEntries = CampaignRouteRenderer.buildRouteEntries(war);

		Faction viewerFaction = FactionManager.getByLeader(player.getName());
		if (viewerFaction == null) {
			viewerFaction = FactionManager.getByMember(player.getName());
		}
		if (viewerFaction == null) {
			player.sendMessage("§cYou are not in a faction.");
			return;
		}

		Inventory inventory = SimpleFactions.plugin.getServer().createInventory(
				new CampaignInventoryHolder(war.getId(), SFGUI.CAMPAIGN_VIEW),
				54,
				war.getName() + " §7Campaign");

		for (int slot : Arrays.asList(0, 1, 2, 6, 7, 8, 45, 46, 52)) {
			inventory.setItem(slot, inv.getFiller(Material.GRAY_STAINED_GLASS_PANE));
		}

		inventory.setItem(4, creator.createInfoItem(
				war,
				viewerFaction,
				player.getUniqueId(),
				BattleScheduleLookups.uuidToFactionForWar(war)));

		populateHourToggles(inventory, war, viewerFaction, player.getUniqueId());

		int entryLimit = Math.min(routeEntries.size(), ROUTE_SLOT_COUNT);
		int firstBattleMarkerSlot = -1;
		for (int i = 0; i < ROUTE_SLOT_COUNT; i++) {
			int slot = ROUTE_START_SLOT + i;
			if (i >= entryLimit) {
				inventory.setItem(slot, new ItemStack(Material.AIR));
				continue;
			}
			CampaignRouteEntry routeEntry = routeEntries.get(i);
			inventory.setItem(slot, creator.createRouteEntryItem(war, viewerFaction, routeEntry));
			if (firstBattleMarkerSlot < 0
					&& CampaignRouteRenderer.isBorderFirstBattleSlot(war, routeEntry)) {
				firstBattleMarkerSlot = slot + 9;
			}
		}
		if (firstBattleMarkerSlot >= 0) {
			inventory.setItem(firstBattleMarkerSlot, creator.createFirstBattleMarkerItem());
		}

		if (canSurrender(player, war)) {
			inventory.setItem(47, creator.createSurrenderButton(war));
		} else {
			inventory.setItem(47, inv.getFiller(Material.GRAY_STAINED_GLASS_PANE));
		}

		if (canAcceptWhitePeace(player, war)) {
			inventory.setItem(48, creator.createAcceptPeaceButton(war));
		} else {
			inventory.setItem(48, inv.getFiller(Material.GRAY_STAINED_GLASS_PANE));
		}

		populatePostBattleChoiceButtons(inventory, war, player);

		populateAutoresolveButtons(inventory, war, player);

		inventory.setItem(53, inv.createBackButton(SFGUI.WAR_VIEW));
		if (open) {
			player.openInventory(inventory);
		}
	}

	private void populateHourToggles(Inventory inventory, War war, Faction viewerFaction, UUID viewerUuid) {
		inventory.setItem(VOTING_HELP_SLOT, creator.createVotingHelpItem());
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

	private void populatePostBattleChoiceButtons(Inventory inventory, War war, Player player) {
		Faction leader = FactionManager.getByLeader(player.getName());
		for (int slot : List.of(40, 41, 42, 43)) {
			inventory.setItem(slot, inv.getFiller(Material.GRAY_STAINED_GLASS_PANE));
		}
		if (leader == null || !CampaignPostBattleChoiceService.needsAnyChoice(war)) {
			return;
		}
		if (!CampaignPostBattleChoiceService.isChoiceLeader(war, leader)) {
			return;
		}
		if (CampaignPostBattleChoiceService.needsWinnerChoice(war)) {
			inventory.setItem(40, creator.createPushButton(war));
			inventory.setItem(41, creator.createHoldButton(war));
			return;
		}
		if (CampaignPostBattleChoiceService.needsLoserResponse(war)) {
			inventory.setItem(42, creator.createLoserAttackButton(war));
			inventory.setItem(43, creator.createLoserAcceptPeaceButton(war));
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

		Integer voteWarId = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_vote_war"),
				PersistentDataType.INTEGER);
		Integer voteHour = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_vote_hour"),
				PersistentDataType.INTEGER);
		if (voteWarId != null && voteHour != null && voteWarId == war.getId()) {
			handleHourToggleClick(player, war, voteHour);
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
			handleAutoresolveClick(player, war, BelligerentRole.valueOf(autoresolveSide));
			return;
		}

		Integer surrenderWarId = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_surrender"),
				PersistentDataType.INTEGER);
		if (surrenderWarId != null && surrenderWarId == war.getId()) {
			if (!canSurrender(player, war)) {
				player.sendMessage("§cYou cannot surrender right now.");
				return;
			}
			Faction leader = FactionManager.getByLeader(player.getName());
			if (leader == null) {
				return;
			}
			inv.confirming.put(player, leader);
			inv.campaignConfirmWar.put(player, war.getId());
			inv.confirmView(player, leader, "campaign_surrender", String.valueOf(war.getId()));
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
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

		Integer pushWarId = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_push"),
				PersistentDataType.INTEGER);
		if (pushWarId != null && pushWarId == war.getId()) {
			if (!canMakePostBattleChoice(player, war)) {
				player.sendMessage("§cYou cannot push right now.");
				return;
			}
			Faction leader = FactionManager.getByLeader(player.getName());
			if (leader == null) {
				return;
			}
			inv.confirming.put(player, leader);
			inv.campaignConfirmWar.put(player, war.getId());
			inv.confirmView(player, leader, "campaign_push", String.valueOf(war.getId()));
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			return;
		}

		Integer holdWarId = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_hold"),
				PersistentDataType.INTEGER);
		if (holdWarId != null && holdWarId == war.getId()) {
			if (!canMakePostBattleChoice(player, war)) {
				player.sendMessage("§cYou cannot hold right now.");
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

		Integer attackWarId = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_attack"),
				PersistentDataType.INTEGER);
		if (attackWarId != null && attackWarId == war.getId()) {
			if (!canMakePostBattleChoice(player, war)) {
				player.sendMessage("§cYou cannot attack right now.");
				return;
			}
			Faction leader = FactionManager.getByLeader(player.getName());
			if (leader == null) {
				return;
			}
			inv.confirming.put(player, leader);
			inv.campaignConfirmWar.put(player, war.getId());
			inv.confirmView(player, leader, "campaign_attack", String.valueOf(war.getId()));
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			return;
		}

		Integer loserPeaceWarId = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_loser_peace"),
				PersistentDataType.INTEGER);
		if (loserPeaceWarId != null && loserPeaceWarId == war.getId()) {
			if (!canMakePostBattleChoice(player, war)) {
				player.sendMessage("§cYou cannot accept peace right now.");
				return;
			}
			Faction leader = FactionManager.getByLeader(player.getName());
			if (leader == null) {
				return;
			}
			inv.confirming.put(player, leader);
			inv.campaignConfirmWar.put(player, war.getId());
			inv.confirmView(player, leader, "campaign_loser_peace", String.valueOf(war.getId()));
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			return;
		}

		Integer provinceId = meta.getPersistentDataContainer().get(
				new NamespacedKey(SimpleFactions.plugin, "campaign_province"),
				PersistentDataType.INTEGER);
		if (provinceId != null) {
			// Route provinces are informational only; choices use dedicated buttons.
		}
	}

	private void handleHourToggleClick(Player player, War war, int hour) {
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
			campaignView(player, war, true);
		}
	}

	private void handleAutoresolveClick(
			Player player,
			War war,
			BelligerentRole side) {
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
				campaignView(player, war, true);
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
			campaignView(player, war, true);
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			return;
		}

		Faction leader = FactionManager.getByLeader(player.getName());
		if (leader == null) {
			return;
		}

		switch (key) {
			case "campaign_push" -> {
				if (!canMakePostBattleChoice(player, war) || !CampaignChoiceService.applyPush(war)) {
					player.sendMessage("§cCould not push.");
					return;
				}
				if (WarManager.getById(war.getId()) == null) {
					inv.warList(player);
					player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
					return;
				}
				player.sendMessage("§aOffensive continues.");
			}
			case "campaign_hold" -> {
				if (!canMakePostBattleChoice(player, war) || !CampaignChoiceService.applyHold(war)) {
					player.sendMessage("§cCould not hold the front.");
					return;
				}
				if (WarManager.getById(war.getId()) == null) {
					inv.warList(player);
					player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
					return;
				}
				player.sendMessage("§aFront held. White peace proposed.");
			}
			case "campaign_attack" -> {
				if (!canMakePostBattleChoice(player, war) || !CampaignChoiceService.applyLoserAttack(war)) {
					player.sendMessage("§cCould not schedule attack.");
					return;
				}
				if (WarManager.getById(war.getId()) == null) {
					inv.warList(player);
					player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
					return;
				}
				player.sendMessage("§aAttack scheduled at the held front.");
			}
			case "campaign_loser_peace" -> {
				if (!canMakePostBattleChoice(player, war) || !CampaignChoiceService.applyLoserAcceptPeace(war)) {
					player.sendMessage("§cCould not accept white peace.");
					return;
				}
				player.sendMessage("§aWhite peace accepted.");
				inv.warList(player);
				player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
				return;
			}
			case "campaign_surrender" -> {
				if (!canSurrender(player, war) || !WarResolutionService.surrender(war, leader)) {
					player.sendMessage("§cCould not surrender.");
					return;
				}
				player.sendMessage("§aYou have surrendered.");
				inv.warList(player);
				player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
				return;
			}
			case "campaign_accept_peace" -> {
				if (!CampaignChoiceService.acceptWhitePeaceAndEnd(war, leader)) {
					player.sendMessage("§cCould not accept white peace.");
					return;
				}
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
		campaignView(player, war, true);
	}

	public boolean canSurrender(Player player, War war) {
		if (war == null || !war.isActive() || CampaignPostBattleChoiceService.needsAnyChoice(war)) {
			return false;
		}
		return isAttackerLeader(player, war) || isDefenderLeader(player, war);
	}

	public boolean canAcceptWhitePeace(Player player, War war) {
		Faction leader = FactionManager.getByLeader(player.getName());
		if (leader == null) {
			return false;
		}
		return WhitePeaceService.acceptWhitePeace(war, leader);
	}

	public boolean canMakePostBattleChoice(Player player, War war) {
		Faction leader = FactionManager.getByLeader(player.getName());
		return leader != null && CampaignPostBattleChoiceService.isChoiceLeader(war, leader);
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
