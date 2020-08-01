package gjum.minecraft.civ.synapse.common.observations.game;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.observations.AccountObservation;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;

import javax.annotation.Nonnull;
import java.util.Objects;

public class CombatTagChat extends ObservationImpl implements AccountObservation {
	@Expose
	public static final String msgType = "CombatTagChat";

	@Expose
	@Nonnull
	public final String other;

	public CombatTagChat(
			@Nonnull String witness,
			@Nonnull String other
	) {
		super(witness);
		this.other = other;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof CombatTagChat)) return false;
		if (!super.equals(o)) return false;
		CombatTagChat other = (CombatTagChat) o;
		return Objects.equals(this.other, other.other);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), other);
	}

	@Override
	public String getMsgType() {
		return msgType;
	}

	@Nonnull
	@Override
	public String getAccount() {
		return other;
	}
}
