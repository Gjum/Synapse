package gjum.minecraft.civ.synapse.common.observations.accountpos;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.common.observations.*;

import javax.annotation.Nonnull;
import java.util.Objects;

public class RadarChange extends ObservationImpl implements AccountPosObservation {
	@Expose
	public static final String msgType = "RadarChange";

	@Expose
	@Nonnull
	public final String account;
	@Expose
	@Nonnull
	public final Pos pos;
	@Expose
	@Nonnull
	public final String world;
	@Expose
	@Nonnull
	public final Action action;

	public RadarChange(
			@Nonnull String witness,
			@Nonnull String account,
			@Nonnull Pos pos,
			@Nonnull String world,
			@Nonnull Action action
	) {
		super(witness);
		this.account = account;
		this.pos = pos;
		this.world = world;
		this.action = action;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		RadarChange other = (RadarChange) o;
		return account.equals(other.account) &&
				pos.equals(other.pos) &&
				world.equals(other.world) &&
				action.equals(other.action);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), account, pos, world, action);
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
