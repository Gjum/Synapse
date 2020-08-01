package gjum.minecraft.civ.synapse;

/**
 * sorted by confidence
 */
public enum Standing {
	FOCUS, HOSTILE, FRIENDLY, NEUTRAL, UNSET;

	public boolean moreConfidentThan(Standing other) {
		if (other == null) return true;
		return this.compareTo(other) < 0;
	}
}
