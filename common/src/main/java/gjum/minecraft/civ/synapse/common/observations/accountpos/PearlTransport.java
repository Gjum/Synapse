package gjum.minecraft.civ.synapse.common.observations.accountpos;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.common.observations.game.PearlLocation;

import javax.annotation.Nonnull;

public class PearlTransport extends PearlLocation implements AccountPosObservation {
	@Expose
	public static final String msgType = "PearlTransport";

	public PearlTransport(
			@Nonnull String witness,
			@Nonnull Pos pos,
			@Nonnull String world,
			@Nonnull String prisoner,
			@Nonnull String holder
	) {
		super(witness, pos, world, prisoner, holder);
		if (!isPlayerHolder()) {
			throw new IllegalArgumentException(
					"Pearl holder '" + holder + "' is not a player");
		}
	}

	@Nonnull
	@Override
	public String getAccount() {
		return holder;
	}

	@Nonnull
	@Override
	public Pos getPos() {
		return pos;
	}

	@Nonnull
	@Override
	public String getWorld() {
		return world;
	}
}
