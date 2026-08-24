package me.Plugins.SimpleFactions.War.battle.dev;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.War.core.Side;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.War.battle.engine.core.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleManager;
import me.Plugins.SimpleFactions.War.battle.military.BattlePoolService;
import me.Plugins.SimpleFactions.War.battle.persistence.BattlePersistenceService;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;
import me.Plugins.SimpleFactions.War.battle.warband.Warband;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;

public final class BattleDevMode {
	private static final String[] DUMMY_NAME_POOL = {
			"Aldric", "Bryn", "Corwyn", "Eryndor", "Hale", "Ilyra", "Kaelis", "Mira",
			"Orin", "Pella", "Sylvi", "Thandor", "Vaelis", "Eldwyn", "Maelor", "Serapha",
			"Tarina", "Nyssara", "Boreas", "Eryn", "Dorn", "Lysa", "Keth"
	};

	private static boolean enabled;

	private BattleDevMode() {}

	public static boolean isEnabled() {
		return enabled;
	}

	public static int setEnabled(boolean value) {
		enabled = value;
		if (enabled) {
			return refreshCampaignBattleDummies();
		}
		return clearAllDummyMembers();
	}

	public static void resetForTests() {
		enabled = false;
	}

	/** Refills campaign side warbands for active auto battles (volatile; not persisted). */
	public static int refreshCampaignBattleDummies() {
		if (!enabled) {
			return 0;
		}
		int sidesFilled = 0;
		for (Battle battle : BattleManager.get()) {
			Integer warId = battle.getWarId();
			if (warId == null) {
				continue;
			}
			War war = WarManager.getById(warId);
			if (war == null || !war.isActive()) {
				continue;
			}
			sidesFilled += refreshCampaignSideWarband(war, battle, BattleTemplate.ATTACKER_SIDE);
			sidesFilled += refreshCampaignSideWarband(war, battle, BattleTemplate.DEFENDER_SIDE);
		}
		return sidesFilled;
	}

	/** Removes devmode dummy members from every loaded warband. */
	public static int clearAllDummyMembers() {
		int cleared = 0;
		for (Warband warband : WarbandManager.get()) {
			if (warband == null || warband.getDummyMemberCount() <= 0) {
				continue;
			}
			warband.clearDummyMembers();
			cleared++;
			if (warband.isFaction()) {
				BattlePersistenceService.persistWarband(warband);
			}
		}
		return cleared;
	}

	private static int refreshCampaignSideWarband(War war, Battle battle, String battleSideId) {
		Warband warband = WarbandManager.getByString(
				Warband.campaignSideWarbandId(war.getId(), battleSideId));
		if (warband == null) {
			return 0;
		}
		seedCampaignSideIfEnabled(warband, war, battle, battleSideId);
		return warband.getDummyMemberCount() > 0 ? 1 : 0;
	}

	public static UUID dummyMemberId(String warbandId, int index) {
		return UUID.nameUUIDFromBytes(
				("battle_phantom:" + warbandId + ":" + index).getBytes(StandardCharsets.UTF_8));
	}

	/** @deprecated use {@link #dummyMemberId(String, int)} */
	public static UUID phantomId(String warbandId, int index) {
		return dummyMemberId(warbandId, index);
	}

	public static String dummyDisplayName(String warbandId, int index) {
		int poolIndex = Math.floorMod(
				(warbandId + ":" + index).hashCode(),
				DUMMY_NAME_POOL.length);
		return DUMMY_NAME_POOL[poolIndex];
	}

	public static void seedDummyMembers(Warband warband, int count) {
		if (warband == null || count <= 0) {
			return;
		}
		List<UUID> dummyIds = new ArrayList<>(count);
		Map<UUID, String> displayNames = new HashMap<>();
		for (int index = 0; index < count; index++) {
			UUID id = dummyMemberId(warband.getId(), index);
			dummyIds.add(id);
			displayNames.put(id, dummyDisplayName(warband.getId(), index));
		}
		warband.addDummyMembers(dummyIds, displayNames);
	}

	/** @deprecated use {@link #seedDummyMembers(Warband, int)} */
	public static void seedPhantoms(Warband warband, int count) {
		seedDummyMembers(warband, count);
	}

	public static void seedDummyMembersIfEnabled(Warband warband) {
		if (!enabled || warband == null) {
			return;
		}
		seedDummyMembers(warband, Cache.battleDevmodePhantomCount);
	}

	/** @deprecated use {@link #seedDummyMembersIfEnabled(Warband)} */
	public static void seedPhantomsIfEnabled(Warband warband) {
		seedDummyMembersIfEnabled(warband);
	}

	public static void seedDummyMembersIfEnabled(Warband warband, War war, Battle battle, String battleSideId) {
		if (!enabled || warband == null || war == null || battle == null) {
			return;
		}
		seedCampaignDummyMembers(warband, war, battle, battleSideId);
	}

	/** @deprecated use {@link #seedDummyMembersIfEnabled(Warband, War, Battle, String)} */
	public static void seedPhantomsIfEnabled(Warband warband, War war, Battle battle, String battleSideId) {
		seedDummyMembersIfEnabled(warband, war, battle, battleSideId);
	}

	public static void seedDummyMembersOnFirstSignupIfEnabled(
			Warband warband,
			War war,
			Battle battle,
			String battleSideId) {
		if (!enabled || warband == null || war == null || battle == null) {
			return;
		}
		if (warband.getDummyMemberCount() > 0) {
			return;
		}
		seedCampaignSideIfEnabled(warband, war, battle, battleSideId);
	}

	/** @deprecated use {@link #seedDummyMembersOnFirstSignupIfEnabled(Warband, War, Battle, String)} */
	public static void seedPhantomsOnFirstSignupIfEnabled(
			Warband warband,
			War war,
			Battle battle,
			String battleSideId) {
		seedDummyMembersOnFirstSignupIfEnabled(warband, war, battle, battleSideId);
	}

	public static void seedCampaignSideIfEnabled(
			Warband warband,
			War war,
			Battle battle,
			String battleSideId) {
		if (!enabled || warband == null || war == null || battle == null) {
			return;
		}
		int before = warband.getDummyMemberCount();
		seedCampaignDummyMembers(warband, war, battle, battleSideId);
		if (warband.getDummyMemberCount() > before && warband.isPendingLeader()) {
			warband.setLeaderId(dummyMemberId(warband.getId(), 0));
		}
	}

	private static void seedCampaignDummyMembers(Warband warband, War war, Battle battle, String battleSideId) {
		Integer provinceId = battle.getProvinceId();
		if (provinceId == null) {
			seedDummyMembers(warband, Cache.battleDevmodePhantomCount);
			return;
		}
		Side side = resolveWarSide(war, battleSideId);
		if (side == null) {
			seedDummyMembers(warband, Cache.battleDevmodePhantomCount);
			return;
		}
		int committedRegiments = BattlePoolService.totalCommittedRegiments(war, provinceId, side);
		int poolLives = Cache.warBattleLivesPerRegiment * committedRegiments;
		int count = Math.min(
				Cache.battleDevmodePhantomCount,
				Math.max(0, poolLives - warband.getMemberCount()));
		seedDummyMembers(warband, count);
	}

	private static Side resolveWarSide(War war, String battleSideId) {
		if (BattleTemplate.ATTACKER_SIDE.equalsIgnoreCase(battleSideId)) {
			return war.getAttackers();
		}
		if (BattleTemplate.DEFENDER_SIDE.equalsIgnoreCase(battleSideId)) {
			return war.getDefenders();
		}
		return null;
	}

}
