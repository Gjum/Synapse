package gjum.minecraft.civ.synapse.common.observations;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Something a player can do.
 */
public enum Action {
	ENTERED(""), LOGIN("+"), LOGOUT("-"), CTLOG("c"),
	NEWSPAWN("n"), APPEARED("+"), DISAPPEARED("-");

	public final String shortName;

	Action(String shortName) {
		this.shortName = shortName;
	}

	@Nullable
	public static Action fromString(@Nonnull String actionStr) {
		switch (actionStr) {
			case "entered":
				return ENTERED;
			case "logged in":
				return LOGIN;
			case "logged out":
				return LOGOUT;
			case "new":
				return NEWSPAWN;
			default:
				System.err.println("Unexpected action " + actionStr);
				return null;
		}
	}
}
