package gjum.minecraft.civ.synapse.common.observations.game;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.observations.AccountObservation;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;

import javax.annotation.Nonnull;
import java.util.Objects;

public class BrandNew extends ObservationImpl implements AccountObservation {
	@Expose
	public static final String msgType = "BrandNew";

	@Expose
	@Nonnull
	public final String account;

	public BrandNew(
			@Nonnull String witness,
			@Nonnull String account
	) {
		super(witness);
		this.account = account;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof BrandNew)) return false;
		if (!super.equals(o)) return false;
		final BrandNew other = (BrandNew) o;
		return account.equals(other.account);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), account);
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
}
