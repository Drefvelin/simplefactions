package me.Plugins.SimpleFactions.Managers.Inventory;

import java.time.Instant;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.pathfinder.TitleManagerProvinceOwnerLookup;
import me.Plugins.SimpleFactions.War.progression.BelligerentRole;
import me.Plugins.SimpleFactions.War.progression.CampaignProgressionService;
import me.Plugins.SimpleFactions.War.progression.CampaignRouteRenderer;
import me.Plugins.SimpleFactions.War.schedule.BattleHourTally;
import me.Plugins.SimpleFactions.War.schedule.BattleQuorumService;
import me.Plugins.SimpleFactions.War.schedule.BattleScheduleLookups;
import me.Plugins.SimpleFactions.War.schedule.BattleScheduleService;
import me.Plugins.SimpleFactions.War.schedule.BattleVoteService;
import me.Plugins.SimpleFactions.War.schedule.BattleWindowService;
import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class CampaignCreator {
	private final TitleManagerProvinceOwnerLookup owners = new TitleManagerProvinceOwnerLookup();

	public ItemStack createRouteProvinceItem(War war, Faction viewer, int provinceId, int axisIndex) {
		Material material = CampaignRouteRenderer.resolveMaterial(war, viewer, provinceId, owners);
		ItemStack item = new ItemStack(material, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex("#d4c9aeProvince " + provinceId));
		List<String> lore = new ArrayList<>(CampaignRouteRenderer.buildRouteLore(war, provinceId, axisIndex));
		List<Integer> actions = CampaignRouteRenderer.actionProvinceIds(war);
		if (actions.size() == 1 && actions.get(0) == provinceId) {
			lore.add(StringFormatter.formatHex("#7fbd73Next battle"));
		} else if (actions.size() == 2 && actions.contains(provinceId)) {
			if (provinceId == war.getCampaignProvinces().get(war.getCursorIndex())) {
				lore.add(StringFormatter.formatHex("#e6c84aHold front"));
			} else {
				lore.add(StringFormatter.formatHex("#e6c84aCounter-push"));
			}
		}
		meta.setLore(lore);
		NamespacedKey provinceKey = new NamespacedKey(SimpleFactions.plugin, "campaign_province");
		meta.getPersistentDataContainer().set(provinceKey, PersistentDataType.INTEGER, provinceId);
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
		meta.setDisplayName(StringFormatter.formatHex("#d4c9aeCampaign status"));
		List<String> lore = new ArrayList<>();
		lore.add(StringFormatter.formatHex("#a39ba8Attacker initiative: #d4c9ae" + war.getInitiativeAttacker()));
		lore.add(StringFormatter.formatHex("#a39ba8Defender initiative: #d4c9ae" + war.getInitiativeDefender()));
		lore.add(StringFormatter.formatHex("#a39ba8Phase: #d4c9ae" + formatPhase(war)));
		if (war.isWhitePeaceProposedByAttacker()) {
			lore.add(StringFormatter.formatHex("#e6c84aAttacker proposed white peace"));
		}
		if (war.isWhitePeaceProposedByDefender()) {
			lore.add(StringFormatter.formatHex("#e6c84aDefender proposed white peace"));
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

		String battleDay = war.getBattleDay() != null ? war.getBattleDay().toString() : "-";
		lines.add(StringFormatter.formatHex("#a39ba8Battle day: #d4c9ae" + battleDay));
		lines.add(StringFormatter.formatHex(
				"#a39ba8Vote closes: #d4c9ae" + Cache.warVoteCloseHour + ":00 UTC"));
		BattleSchedulePhase schedulePhase = war.getBattleSchedulePhase();
		lines.add(StringFormatter.formatHex(
				"#a39ba8Schedule: #d4c9ae"
						+ (schedulePhase != null ? schedulePhase.toJson() : BattleSchedulePhase.IDLE.toJson())));

		Function<UUID, Faction> factionLookup = uuidToFaction != null
				? uuidToFaction
				: BattleScheduleLookups.uuidToFactionForWar(war);

		if (schedulePhase == BattleSchedulePhase.VOTING) {
			lines.add(StringFormatter.formatHex("#e6c84aHour votes:"));
			Map<Integer, BattleHourTally> tally = BattleVoteService.buildHourTally(war, factionLookup);
			for (int hour : BattleWindowService.listValidHours()) {
				BattleHourTally counts = tally.getOrDefault(hour, new BattleHourTally(0, 0));
				lines.add(StringFormatter.formatHex(
						"#a39ba8" + hour + ":00 #d4c9aeA" + counts.attackerCount() + " / D" + counts.defenderCount()));
			}
			lines.add(StringFormatter.formatHex(
					"#a39ba8Total voters: #d4c9ae" + BattleQuorumService.countDistinctVoters(war)));
		}

		Set<Integer> selections = viewerUuid != null
				? new TreeSet<>(BattleVoteService.getPlayerSelections(war, viewerUuid))
				: Set.of();
		String yourHours = selections.isEmpty()
				? "-"
				: selections.stream().map(h -> h + ":00").collect(Collectors.joining(", "));
		lines.add(StringFormatter.formatHex("#a39ba8Your hours: #d4c9ae" + yourHours));

		if (schedulePhase == BattleSchedulePhase.SCHEDULED) {
			Instant fightAt = war.getScheduledBattleAt();
			if (fightAt != null) {
				lines.add(StringFormatter.formatHex("#a39ba8Fight at: #d4c9ae" + fightAt + " UTC"));
			}
			if (war.getScheduledBattleProvinceId() != null) {
				lines.add(StringFormatter.formatHex(
						"#a39ba8Battle province: #d4c9ae" + war.getScheduledBattleProvinceId()));
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
		meta.setDisplayName(StringFormatter.formatHex(
				(selected ? "#7fbd73" : "#d4c9ae") + hour + ":00 UTC"));
		int attackerVotes = tally != null ? tally.attackerCount() : 0;
		int defenderVotes = tally != null ? tally.defenderCount() : 0;
		List<String> lore = new ArrayList<>();
		lore.add(StringFormatter.formatHex("#a39ba8Attacker votes: #d4c9ae" + attackerVotes));
		lore.add(StringFormatter.formatHex("#a39ba8Defender votes: #d4c9ae" + defenderVotes));
		if (clickable) {
			lore.add(StringFormatter.formatHex(
					selected ? "#7fbd73Click to remove this hour" : "#d4c9aeClick to select this hour"));
		} else {
			lore.add(StringFormatter.formatHex("#a39ba8Voting closed or you are not eligible"));
		}
		meta.setLore(lore);
		if (clickable) {
			NamespacedKey hourKey = new NamespacedKey(SimpleFactions.plugin, "campaign_vote_hour");
			NamespacedKey warKey = new NamespacedKey(SimpleFactions.plugin, "campaign_vote_war");
			meta.getPersistentDataContainer().set(hourKey, PersistentDataType.INTEGER, hour);
			meta.getPersistentDataContainer().set(warKey, PersistentDataType.INTEGER, war.getId());
		}
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createUnusedHourSlotItem() {
		ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex("#a39ba8Battle hour slot"));
		meta.setLore(List.of(StringFormatter.formatHex("#a39ba8No vote hour on this slot")));
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createAutoresolveProposeButton(War war, BelligerentRole side) {
		ItemStack item = new ItemStack(Material.GRAY_BANNER, 1);
		ItemMeta meta = item.getItemMeta();
		String sideLabel = side == BelligerentRole.ATTACKER ? "Attacker" : "Defender";
		meta.setDisplayName(StringFormatter.formatHex("#d4c9ae" + sideLabel + " autoresolve"));
		meta.setLore(List.of(
				StringFormatter.formatHex("#a39ba8Propose skipping the battle vote"),
				StringFormatter.formatHex("#a39ba8Opposing war leader must accept"),
				StringFormatter.formatHex("#a39ba8(60s request, before vote close)")));
		NamespacedKey warKey = new NamespacedKey(SimpleFactions.plugin, "campaign_autoresolve_war");
		NamespacedKey sideKey = new NamespacedKey(SimpleFactions.plugin, "campaign_autoresolve_side");
		meta.getPersistentDataContainer().set(warKey, PersistentDataType.INTEGER, war.getId());
		meta.getPersistentDataContainer().set(sideKey, PersistentDataType.STRING, side.name());
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createAcceptPeaceButton(War war) {
		ItemStack item = new ItemStack(Material.WHITE_BANNER, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex("#7fbd73Accept white peace"));
		meta.setLore(List.of(
				StringFormatter.formatHex("#a39ba8End the war with no goal"),
				StringFormatter.formatHex("#a39ba8No reparations")));
		NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "campaign_accept_peace");
		meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, war.getId());
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createHoldButton(War war) {
		ItemStack item = new ItemStack(Material.YELLOW_CONCRETE, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex("#e6c84aHold front"));
		meta.setLore(List.of(StringFormatter.formatHex("#a39ba8No counter-offensive")));
		NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "campaign_hold");
		meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, war.getId());
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createCounterPushButton(War war) {
		ItemStack item = new ItemStack(Material.ORANGE_CONCRETE, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex("#e6c84aCounter-push"));
		meta.setLore(List.of(StringFormatter.formatHex("#a39ba8Push toward attacker capital")));
		NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "campaign_counter");
		meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, war.getId());
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createPageButton(String label, int warId, int page) {
		ItemStack item = new ItemStack(Material.ARROW, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex("#d4c9ae" + label));
		NamespacedKey warKey = new NamespacedKey(SimpleFactions.plugin, "campaign_page_war");
		NamespacedKey pageKey = new NamespacedKey(SimpleFactions.plugin, "campaign_page");
		meta.getPersistentDataContainer().set(warKey, PersistentDataType.INTEGER, warId);
		meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);
		item.setItemMeta(meta);
		return item;
	}

	private String formatPhase(War war) {
		if (war.getCampaignPhase() == null) {
			return "invasion";
		}
		return war.getCampaignPhase().toJson();
	}

	public boolean isDefenderChoiceActive(War war) {
		return CampaignProgressionService.isAttackerInitiativeExhausted(war)
				&& war.getCampaignPhase() == me.Plugins.SimpleFactions.War.enums.CampaignPhase.INVASION
				&& CampaignRouteRenderer.actionProvinceIds(war).size() == 2;
	}
}
