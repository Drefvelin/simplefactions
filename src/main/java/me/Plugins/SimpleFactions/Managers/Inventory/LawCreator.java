package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.FactionModifier;
import me.Plugins.SimpleFactions.enums.Region;
import me.Plugins.SimpleFactions.enums.Rules;
import me.Plugins.SimpleFactions.enums.Scope;
import me.Plugins.SimpleFactions.keys.Keys;
import me.Plugins.SimpleFactions.laws.Law;
import me.Plugins.SimpleFactions.laws.LawEffect;
import me.Plugins.SimpleFactions.laws.LawGroup;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class LawCreator {

	private static final String CHECK = "✔";
	private static final String CROSS = "✖";

	private static final String GREEN = "#87d65c";
	private static final String RED   = "#d65c5c";
	private static final String GRAY  = "#6f776a";
	private static final String LIGHT_GRAY  = "#9cb68c";

    public ItemStack createLawItem(Player p, Faction f, LawGroup group) {
        Law current = group.getCurrent();
		ItemStack i = current.getIcon();
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(group.getName());

		List<String> lore = new ArrayList<>();

		// ---- Group description (top) ----
		if (group.hasDescription()) {
			lore.addAll(group.getDescription());
			lore.add("");
		}

		// ---- Current law highlight ----
		lore.add(StringFormatter.formatHex("#9cb68cCurrent Policy"));
		lore.add(StringFormatter.formatHex("  #87d65c" + current.getName()));
		lore.add("");

		// ---- Effects header ----
		if (current.hasEffects()) {
			for (Map.Entry<Scope, LawEffect> entry : current.getScopedEffects().entrySet()) {

				Scope scope = entry.getKey();
				LawEffect effect = entry.getValue();
				boolean factionScope = scope == Scope.FACTION;

				// Scope header
				if (!factionScope) {
					lore.add(StringFormatter.formatHex(GRAY + scope.getDisplay() + ":"));
				}

				String indent = factionScope ? "  " : "    ";

				// ---- Rules ----
				if (effect.hasRules()) {
					for (Map.Entry<Rules, Boolean> ruleEntry : effect.getRules().entrySet()) {

						Rules rule = ruleEntry.getKey();
						boolean value = ruleEntry.getValue();

						String symbol = value ? GREEN + CHECK : RED + CROSS;

						lore.add(StringFormatter.formatHex(
								indent + symbol + " #d4c9ae" + rule.getDisplay()
						));
					}
				}

				// ---- Global modifiers ----
				if (effect.hasGlobalModifiers()) {
					for (FactionModifier mod : effect.getGlobalModifiers()) {
						lore.add(indent + mod.getString());
					}
				}

				// ---- Region modifiers ----
				if (effect.hasRegionModifiers()) {
					for (Map.Entry<Region, List<FactionModifier>> regionEntry
							: effect.getRegionModifiers().entrySet()) {

						Region region = regionEntry.getKey();

						lore.add(StringFormatter.formatHex(
								indent + LIGHT_GRAY + region.getDisplay() + ":"
						));

						for (FactionModifier mod : regionEntry.getValue()) {
							lore.add(indent + "  " + mod.getString());
						}
					}
				}
			}
			lore.add("");
		}
		// ---- Law description ----
		if (current.hasDescription()) {
			lore.addAll(current.getDescription());
		}

		meta.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, group.getId());
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
}
