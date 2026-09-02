package me.Plugins.SimpleFactions.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import me.Plugins.SimpleFactions.SimpleFactions;

/**
 * Warns players who type camelCase names (GreenWrathTribe) instead of using
 * {@code _} for spaces. First attempt blocks; repeating the same name proceeds.
 */
public final class DisplayNameGate implements Listener {

	public enum NameOperation {
		FACTION_CREATE,
		GUILD_CREATE,
		FACTION_RENAME,
		GUILD_RENAME,
		SETTLEMENT_FOUND,
		COMPANY_FOUND
	}

	public enum Result {
		OK,
		NEEDS_CONFIRM
	}

	private static final long TIMEOUT_TICKS = 20L * 60;

	private static final Map<UUID, Pending> pending = new HashMap<>();

	private static final class Pending {
		private final NameOperation op;
		private final String raw;

		private Pending(NameOperation op, String raw) {
			this.op = op;
			this.raw = raw;
		}
	}

	public static boolean looksLikeMissingSpaces(String raw) {
		if (raw == null || raw.isBlank()) {
			return false;
		}
		String plain = Formatter.formatId(raw).trim();
		if (plain.contains("_") || plain.contains(" ")) {
			return false;
		}
		for (int i = 1; i < plain.length(); i++) {
			if (Character.isLowerCase(plain.charAt(i - 1))
					&& Character.isUpperCase(plain.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	public static String suggestUnderscores(String raw) {
		if (raw == null) {
			return "";
		}
		String plain = Formatter.formatId(raw).trim();
		StringBuilder out = new StringBuilder(plain.length() + 4);
		for (int i = 0; i < plain.length(); i++) {
			char c = plain.charAt(i);
			if (i > 0
					&& Character.isLowerCase(plain.charAt(i - 1))
					&& Character.isUpperCase(c)) {
				out.append('_');
			}
			out.append(c);
		}
		return out.toString();
	}

	public static Result check(Player player, NameOperation op, String raw) {
		return check(player, op, raw, false);
	}

	public static Result check(Player player, NameOperation op, String raw, boolean chatConfirm) {
		if (player == null || op == null || !looksLikeMissingSpaces(raw)) {
			if (player != null) {
				pending.remove(player.getUniqueId());
			}
			return Result.OK;
		}
		UUID id = player.getUniqueId();
		Pending prev = pending.get(id);
		if (prev != null && prev.op == op && prev.raw.equals(raw)) {
			pending.remove(id);
			return Result.OK;
		}
		Pending next = new Pending(op, raw);
		pending.put(id, next);
		sendHint(player, raw, chatConfirm);
		scheduleTimeout(player, next);
		return Result.NEEDS_CONFIRM;
	}

	private static void sendHint(Player player, String raw, boolean chatConfirm) {
		String shown = Formatter.formatId(raw).trim();
		String suggestion = suggestUnderscores(raw);
		player.sendMessage("§cThat name looks like multiple words run together (it will show as "
				+ shown + ").");
		player.sendMessage("§7Use §e_ §7for spaces, e.g. §e" + suggestion + "§7.");
		if (chatConfirm) {
			player.sendMessage("§7Type the same name again in chat to keep it anyway.");
		} else {
			player.sendMessage("§7Run the same command again to keep this name anyway.");
		}
	}

	private static void scheduleTimeout(Player player, Pending next) {
		UUID id = player.getUniqueId();
		new BukkitRunnable() {
			@Override
			public void run() {
				Pending current = pending.get(id);
				if (current != next) {
					return;
				}
				pending.remove(id);
			}
		}.runTaskLater(SimpleFactions.getInstance(), TIMEOUT_TICKS);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		pending.remove(event.getPlayer().getUniqueId());
	}
}
