package gjum.minecraft.civ.synapse.common.observations.game;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;
import gjum.minecraft.civ.synapse.common.observations.PosObservation;

import javax.annotation.Nonnull;
import java.util.Objects;

public class PearlLocation extends ObservationImpl implements PosObservation {
	@Expose
	public static final String msgType = "PearlLocation";

	@Expose
	@Nonnull
	public final Pos pos;
	@Expose
	@Nonnull
	public final String world;
	@Expose
	@Nonnull
	public final String prisoner;
	@Expose
	@Nonnull
	public final String holder;

	public PearlLocation(
			@Nonnull String witness,
			@Nonnull Pos pos,
			@Nonnull String world,
			@Nonnull String prisoner,
			@Nonnull String holder
	) {
		super(witness);
		this.pos = pos;
		this.world = world;
		this.prisoner = prisoner;
		this.holder = holder;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		PearlLocation other = (PearlLocation) o;
		return prisoner.equals(other.prisoner) &&
				holder.equals(other.holder) &&
				pos.equals(other.pos) &&
				world.equals(other.world);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), prisoner, holder, pos, world);
	}

	public static boolean isPlayerHolder(@Nonnull String holder) {
		return !holder.equals("nobody") && !holder.contains(" ");
	}

	public boolean isPlayerHolder() {
		return isPlayerHolder(holder);
	}

	@Override
	public String getMsgType() {
		return msgType;
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
