package gjum.minecraft.civ.synapse.common.observations.accountpos;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Info about players on radar.
 */
public class PlayerState extends ObservationImpl implements AccountPosObservation {
	@Expose
	public static final String msgType = "PlayerState";

	@Expose
	@Nonnull
	public final String account;
	@Expose
	@Nonnull
	public final Pos pos;
	@Expose
	@Nonnull
	public final String world;

	public PlayerState(
			@Nonnull String witness,
			@Nonnull String account,
			@Nonnull Pos pos,
			@Nonnull String world
	) {
		super(witness);
		this.account = account;
		this.pos = pos;
		this.world = world;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		PlayerState other = (PlayerState) o;
		return account.equals(other.account) &&
				pos.equals(other.pos) &&
				world.equals(other.world);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), account, pos, world);
	}

	@Override
	public String getMsgType() {
		return msgType;
	}

	@Nonnull
	@Override
	public String getAccount() {
		return account;
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
