package gjum.minecraft.civ.synapse.common.observations.accountpos;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.common.observations.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class SnitchHit extends ObservationImpl implements AccountPosObservation, GroupObservation {
	@Expose
	public static final String msgType = "SnitchHit";

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
	@Expose
	@Nonnull
	public final String snitchName;
	@Expose
	@Nullable
	public final String group;
	@Expose
	@Nullable
	public final String snitchType;

	public SnitchHit(
			@Nonnull String witness,
			@Nonnull String account,
			@Nonnull Pos pos,
			@Nonnull String world,
			@Nonnull Action action,
			@Nonnull String snitchName,
			@Nullable String group,
			@Nullable String snitchType
	) {
		super(witness);
		this.account = account;
		this.pos = pos;
		this.world = world;
		this.action = action;
		this.snitchName = snitchName;
		this.group = group;
		this.snitchType = snitchType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		SnitchHit other = (SnitchHit) o;
		return account.equals(other.account) &&
				pos.equals(other.pos) &&
				world.equals(other.world) &&
				action == other.action &&
				snitchName.equals(other.snitchName) &&
				Objects.equals(group, other.group) &&
				Objects.equals(snitchType, other.snitchType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(account, pos, world, action, snitchName, group, snitchType);
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

	@Nullable
	@Override
	public String getGroup() {
		return group;
	}
}
