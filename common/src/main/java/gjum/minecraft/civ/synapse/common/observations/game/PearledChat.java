package gjum.minecraft.civ.synapse.common.observations.game;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.observations.AccountObservation;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class PearledChat extends ObservationImpl implements AccountObservation {
	@Expose
	public static final String msgType = "PearledChat";

	@Expose
	@Nonnull
	public final String holder;
	@Expose
	@Nullable
	public final String pearlType;

	public PearledChat(
			@Nonnull String witness,
			@Nonnull String holder,
			@Nullable String pearlType
	) {
		super(witness);
		this.holder = holder;
		this.pearlType = pearlType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PearledChat)) return false;
		if (!super.equals(o)) return false;
		PearledChat other = (PearledChat) o;
		return Objects.equals(this.holder, other.holder)
				&& Objects.equals(this.pearlType, other.pearlType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), holder, pearlType);
	}

	@Override
	public String getMsgType() {
		return msgType;
	}

	@Nonnull
	@Override
	public String getAccount() {
		return holder;
	}
}
