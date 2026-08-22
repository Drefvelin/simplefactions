package me.Plugins.SimpleFactions.Managers.Inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.naming.BattleNamingService;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.CampaignBattleKind;
import me.Plugins.SimpleFactions.War.pathfinder.TitleManagerProvinceOwnerLookup;
import me.Plugins.SimpleFactions.War.progression.BelligerentRole;
import me.Plugins.SimpleFactions.War.progression.CampaignRouteRenderer;
import me.Plugins.SimpleFactions.War.schedule.BattleHourTally;
import me.Plugins.SimpleFactions.War.schedule.BattleQuorumService;
import me.Plugins.SimpleFactions.War.schedule.BattleScheduleLookups;
import me.Plugins.SimpleFactions.War.schedule.BattleVoteService;
import me.Plugins.SimpleFactions.War.schedule.BattleWindowService;
import me.Plugins.SimpleFactions.War.schedule.CampaignScheduleService;
import me.Plugins.SimpleFactions.War.schedule.CampaignUiCopy;
import me.Plugins.SimpleFactions.War.schedule.CampaignUiTimeFormatter;
import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class CampaignCreator {
	private final TitleManagerProvinceOwnerLookup owners = new TitleManagerProvinceOwnerLookup();

	public ItemStack createRouteProvinceItem(War war, Faction viewer, int provinceId, int axisIndex) {
		CampaignBattleKind slotKind = CampaignScheduleService.slotForProvince(war, provinceId)
				.map(slot -> slot.kind())
				.orElse(null);
		Material material = resolveRouteMaterial(war, viewer, provinceId, slotKind);
		ItemStack item = new ItemStack(material, 1);
		ItemMeta meta = item.getItemMeta();
		String locationDisplay = BattleNamingService.resolveLocationDisplayName(provinceId);
		String locationKey = BattleNamingService.resolveLocationKey(provinceId);
		int ordinal = war.getLocationBattleCount(locationKey) + 1;
		BattleType battleType = CampaignScheduleService.slotForProvince(war, provinceId)
				.map(slot -> slot.battleType())
				.orElse(BattleType.FIELD);
		String displayName = BattleNamingService.buildDisplayName(battleType, locationDisplay, ordinal);
		meta.setDisplayName(StringFormatter.formatHex(CampaignUiCopy.VALUE + displayName));
		meta.setLore(CampaignRouteRenderer.buildRouteLore(war, provinceId, owners));
		if (slotKind == CampaignBattleKind.SIEGE) {
			meta.addEnchant(Enchantment.UNBREAKING, 1, true);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}
		NamespacedKey provinceKey = new NamespacedKey(SimpleFactions.plugin, "campaign_province");
		meta.getPersistentDataContainer().set(provinceKey, PersistentDataType.INTEGER, provinceId);
		item.setItemMeta(meta);
		return item;
	}

	private Material resolveRouteMaterial(War war, Faction viewer, int provinceId, CampaignBattleKind slotKind) {
		if (slotKind == CampaignBattleKind.NAVAL) {
			return Material.TRIDENT;
		}
		if (slotKind == CampaignBattleKind.NAVAL_INVASION) {
			return Material.IRON_SWORD;
		}
		return CampaignRouteRenderer.resolveMaterial(war, viewer, provinceId, owners);
	}

	public ItemStack createFirstBattleMarkerItem() {
		ItemStack item = TLibs.getItemAPI().getCreator().getItemsAdderItem("mcicons:icon_up_gray");
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex(CampaignUiCopy.STATUS_HIGHLIGHT + "First Battle"));
		meta.setLore(null);
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createInfoItem(War war, Faction viewerFaction, UUID viewerUuid) {
		return createInfoItem(war, viewerFaction, viewerUuid, null);
	}

	public ItemStack createInfoItem(
			War war,
			Faction viewerFaction,
			UUID viewerUuid,
			Function<UUID, Faction> uuidToFaction) {
		ItemStack item = new ItemStack(Material.BOOK, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex(CampaignUiCopy.VALUE + "Campaign status"));
		List<String> lore = new ArrayList<>();
		lore.add(StringFormatter.formatHex(
				CampaignUiCopy.LABEL + "Attacker Initiative: " + CampaignUiCopy.VALUE + war.getInitiativeAttacker()));
		lore.add(StringFormatter.formatHex(
				CampaignUiCopy.LABEL + "Defender Initiative: " + CampaignUiCopy.VALUE + war.getInitiativeDefender()));
		lore.add(StringFormatter.formatHex(
				CampaignUiCopy.LABEL + "Initiative Holder: "
						+ CampaignUiCopy.VALUE + CampaignUiCopy.formatInitiativeHolder(war.getInitiativeHolder())));
		lore.add(StringFormatter.formatHex(
				CampaignUiCopy.LABEL + "Phase: "
						+ CampaignUiCopy.VALUE + CampaignUiCopy.titleCasePhase(war.getCampaignPhase())));
		if (war.isWhitePeaceProposedByAttacker()) {
			lore.add(StringFormatter.formatHex(CampaignUiCopy.WARNING + "Attacker proposed white peace"));
		}
		if (war.isWhitePeaceProposedByDefender()) {
			lore.add(StringFormatter.formatHex(CampaignUiCopy.WARNING + "Defender proposed white peace"));
		}
		lore.addAll(buildScheduleInfoLines(war, viewerUuid, uuidToFaction));
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	public static List<String> buildScheduleInfoLines(
			War war,
			UUID viewerUuid,
			Function<UUID, Faction> uuidToFaction) {
		List<String> lines = new ArrayList<>();
		if (war == null) {
			return lines;
		}

		LocalDate battleDay = war.getBattleDay();
		lines.add(StringFormatter.formatHex(
				CampaignUiCopy.LABEL + "Battle Day: " + CampaignUiCopy.VALUE + CampaignUiCopy.formatBattleDay(battleDay)));
		lines.add(StringFormatter.formatHex(
				CampaignUiCopy.LABEL + "Vote Closes: "
						+ CampaignUiCopy.VALUE + CampaignUiTimeFormatter.formatUtcHour(battleDay, Cache.warVoteCloseHour)));
		lines.add(StringFormatter.formatHex(
				CampaignUiCopy.LABEL + "Status: "
						+ CampaignUiCopy.STATUS_HIGHLIGHT + CampaignUiCopy.resolveActivityStatus(war)));

		Function<UUID, Faction> factionLookup = uuidToFaction != null
				? uuidToFaction
				: BattleScheduleLookups.uuidToFactionForWar(war);

		BattleSchedulePhase schedulePhase = war.getBattleSchedulePhase();
		if (schedulePhase == BattleSchedulePhase.VOTING) {
			String attackerName = war.getAttackers().getLeader().getName();
			String defenderName = war.getDefenders().getLeader().getName();
			lines.add(StringFormatter.formatHex(CampaignUiCopy.STATUS_HIGHLIGHT + "Hour Votes:"));
			lines.add(StringFormatter.formatHex(
					CampaignUiCopy.MUTED + "(" + attackerName + "/" + defenderName + ")"));
			Map<Integer, BattleHourTally> tally = BattleVoteService.buildHourTally(war, factionLookup);
			for (int hour : BattleWindowService.listValidHours()) {
				BattleHourTally counts = tally.getOrDefault(hour, new BattleHourTally(0, 0));
				String timeLabel = CampaignUiTimeFormatter.formatUtcHour(battleDay, hour);
				lines.add(StringFormatter.formatHex(
						CampaignUiCopy.LABEL + timeLabel
								+ CampaignUiCopy.VALUE + " · " + counts.attackerCount() + "/" + counts.defenderCount()));
			}
			lines.add(StringFormatter.formatHex(
					CampaignUiCopy.LABEL + "Total voters: " + CampaignUiCopy.VALUE
							+ BattleQuorumService.countDistinctVoters(war)));
		}

		Set<Integer> selections = viewerUuid != null
				? new TreeSet<>(BattleVoteService.getPlayerSelections(war, viewerUuid))
				: Set.of();
		String yourHours = selections.isEmpty()
				? "-"
				: selections.stream()
						.map(h -> CampaignUiTimeFormatter.formatUtcHour(battleDay, h))
						.collect(Collectors.joining(", "));
		lines.add(StringFormatter.formatHex(CampaignUiCopy.LABEL + "Your Hours: " + CampaignUiCopy.VALUE + yourHours));

		if (schedulePhase == BattleSchedulePhase.SCHEDULED) {
			Instant fightAt = war.getScheduledBattleAt();
			if (fightAt != null) {
				lines.add(StringFormatter.formatHex(
						CampaignUiCopy.LABEL + "Fight At: "
								+ CampaignUiCopy.VALUE + CampaignUiTimeFormatter.formatInstant(fightAt)));
			}
			if (war.getScheduledBattleProvinceId() != null) {
				lines.add(StringFormatter.formatHex(
						CampaignUiCopy.LABEL + "Battle province: "
								+ CampaignUiCopy.VALUE + war.getScheduledBattleProvinceId()));
			}
		}

		return lines;
	}

	public ItemStack createHourToggleItem(
			War war,
			int hour,
			boolean selected,
			boolean clickable,
			BattleHourTally tally) {
		String iconId = selected ? "mcicons:icon_confirm" : "mcicons:icon_cancel";
		ItemStack item = TLibs.getItemAPI().getCreator().getItemsAdderItem(iconId);
		ItemMeta meta = item.getItemMeta();
		LocalDate battleDay = war != null ? war.getBattleDay() : null;
		String timeLabel = CampaignUiTimeFormatter.formatUtcHour(battleDay, hour);
		meta.setDisplayName(StringFormatter.formatHex(
				(selected ? CampaignUiCopy.SELECT : CampaignUiCopy.REMOVE) + timeLabel));
		int attackerVotes = tally != null ? tally.attackerCount() : 0;
		int defenderVotes = tally != null ? tally.defenderCount() : 0;
		List<String> lore = new ArrayList<>();
		lore.add(StringFormatter.formatHex(
				CampaignUiCopy.VALUE + attackerVotes + "/" + defenderVotes));
		if (war != null) {
			lore.add(StringFormatter.formatHex(
					CampaignUiCopy.MUTED + "("
							+ war.getAttackers().getLeader().getName()
							+ "/"
							+ war.getDefenders().getLeader().getName()
							+ ")"));
		}
		if (clickable) {
			lore.add(StringFormatter.formatHex(
					selected
							? CampaignUiCopy.REMOVE + "Click to remove this hour"
							: CampaignUiCopy.SELECT + "Click to select this hour"));
		} else {
			lore.add(StringFormatter.formatHex(CampaignUiCopy.LABEL + "Voting closed or you are not eligible"));
		}
		meta.setLore(lore);
		if (clickable && war != null) {
			NamespacedKey hourKey = new NamespacedKey(SimpleFactions.plugin, "campaign_vote_hour");
			NamespacedKey warKey = new NamespacedKey(SimpleFactions.plugin, "campaign_vote_war");
			meta.getPersistentDataContainer().set(hourKey, PersistentDataType.INTEGER, hour);
			meta.getPersistentDataContainer().set(warKey, PersistentDataType.INTEGER, war.getId());
		}
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createVotingHelpItem() {
		ItemStack item = new ItemStack(Material.WRITABLE_BOOK, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex(CampaignUiCopy.VALUE + "Battle hour voting"));
		meta.setLore(List.of(
				StringFormatter.formatHex(CampaignUiCopy.LABEL + "Pick any hours you can fight"),
				StringFormatter.formatHex(CampaignUiCopy.LABEL + "One vote per eligible player"),
				StringFormatter.formatHex(CampaignUiCopy.LABEL + "Most-voted hour wins after vote close"),
				StringFormatter.formatHex(CampaignUiCopy.LABEL + "Coalition totals shown as Attacker/Defender counts")));
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createUnusedHourSlotItem() {
		ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex(CampaignUiCopy.LABEL + "Battle hour slot"));
		meta.setLore(List.of(StringFormatter.formatHex(CampaignUiCopy.LABEL + "No vote hour on this slot")));
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createAutoresolveProposeButton(War war, BelligerentRole side) {
		ItemStack item = new ItemStack(Material.GRAY_BANNER, 1);
		ItemMeta meta = item.getItemMeta();
		String sideLabel = side == BelligerentRole.ATTACKER ? "Attacker" : "Defender";
		meta.setDisplayName(StringFormatter.formatHex(CampaignUiCopy.VALUE + sideLabel + " autoresolve"));
		meta.setLore(List.of(
				StringFormatter.formatHex(CampaignUiCopy.LABEL + "Propose skipping the battle vote"),
				StringFormatter.formatHex(CampaignUiCopy.LABEL + "Opposing war leader must accept"),
				StringFormatter.formatHex(CampaignUiCopy.LABEL + "(60s request, before vote close)")));
		NamespacedKey warKey = new NamespacedKey(SimpleFactions.plugin, "campaign_autoresolve_war");
		NamespacedKey sideKey = new NamespacedKey(SimpleFactions.plugin, "campaign_autoresolve_side");
		meta.getPersistentDataContainer().set(warKey, PersistentDataType.INTEGER, war.getId());
		meta.getPersistentDataContainer().set(sideKey, PersistentDataType.STRING, side.name());
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createSurrenderButton(War war) {
		ItemStack item = new ItemStack(Material.RED_BANNER, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex(CampaignUiCopy.REMOVE + "Surrender"));
		meta.setLore(List.of(
				StringFormatter.formatHex(CampaignUiCopy.LABEL + "Your coalition loses the war"),
				StringFormatter.formatHex(CampaignUiCopy.LABEL + "Opponent wins")));
		NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "campaign_surrender");
		meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, war.getId());
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createAcceptPeaceButton(War war) {
		ItemStack item = new ItemStack(Material.WHITE_BANNER, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex(CampaignUiCopy.SELECT + "Accept white peace"));
		meta.setLore(List.of(
				StringFormatter.formatHex(CampaignUiCopy.LABEL + "End the war with no goal"),
				StringFormatter.formatHex(CampaignUiCopy.LABEL + "No reparations")));
		NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "campaign_accept_peace");
		meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, war.getId());
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createPushButton(War war) {
		ItemStack item = new ItemStack(Material.LIME_CONCRETE, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex(CampaignUiCopy.SELECT + "Push"));
		meta.setLore(List.of(StringFormatter.formatHex(CampaignUiCopy.LABEL + "Continue the offensive")));
		NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "campaign_push");
		meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, war.getId());
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createHoldButton(War war) {
		ItemStack item = new ItemStack(Material.YELLOW_CONCRETE, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex(CampaignUiCopy.STATUS_HIGHLIGHT + "Hold"));
		meta.setLore(List.of(
				StringFormatter.formatHex(CampaignUiCopy.LABEL + "Hold the front"),
				StringFormatter.formatHex(CampaignUiCopy.LABEL + "Auto-propose white peace")));
		NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "campaign_hold");
		meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, war.getId());
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createLoserAttackButton(War war) {
		ItemStack item = new ItemStack(Material.RED_CONCRETE, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex(CampaignUiCopy.REMOVE + "Attack"));
		meta.setLore(List.of(StringFormatter.formatHex(CampaignUiCopy.LABEL + "Schedule battle at the held front")));
		NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "campaign_attack");
		meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, war.getId());
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createLoserAcceptPeaceButton(War war) {
		ItemStack item = new ItemStack(Material.WHITE_CONCRETE, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex(CampaignUiCopy.SELECT + "Accept white peace"));
		meta.setLore(List.of(StringFormatter.formatHex(CampaignUiCopy.LABEL + "End the war with no goal")));
		NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "campaign_loser_peace");
		meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, war.getId());
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createPageButton(String label, int warId, int page) {
		ItemStack item = new ItemStack(Material.ARROW, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex(CampaignUiCopy.VALUE + label));
		NamespacedKey warKey = new NamespacedKey(SimpleFactions.plugin, "campaign_page_war");
		NamespacedKey pageKey = new NamespacedKey(SimpleFactions.plugin, "campaign_page");
		meta.getPersistentDataContainer().set(warKey, PersistentDataType.INTEGER, warId);
		meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);
		item.setItemMeta(meta);
		return item;
	}
}
