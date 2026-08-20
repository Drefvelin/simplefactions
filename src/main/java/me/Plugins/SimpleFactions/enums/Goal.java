package me.Plugins.SimpleFactions.enums;

/**
 * Legacy war goal types from {@code wargoals.yml}.
 * <p>
 * v2 declare goals are {@link me.Plugins.SimpleFactions.War.enums.WarGoalType}:
 * {@code DE_JURE_ANNEX}, {@code SUBJUGATE}, {@code TRANSFER_SUBJECT}.
 * Values such as {@link #REVOLT}, {@link #USURP}, and {@link #WAR_REPARATIONS}
 * are legacy-only until step 62 or later.
 */
public enum Goal {
	/** Legacy YAML id {@code annex}; maps to v2 {@code de_jure_annex}. */
	ANNEX,
	SUBJUGATE,
	/** @deprecated Legacy only; not a v2 declare goal. */
	@Deprecated REVOLT,
	/** @deprecated Legacy only; not a v2 declare goal. */
	@Deprecated TRIBUTARY,
	/** @deprecated Legacy only; not a v2 declare goal. */
	@Deprecated INDEPENDENCE,
	/** @deprecated Legacy only; not a v2 declare goal. */
	@Deprecated WAR_REPARATIONS,
	TRANSFER_SUBJECT,
	/** @deprecated Legacy only; not a v2 declare goal. */
	@Deprecated USURP,
}
