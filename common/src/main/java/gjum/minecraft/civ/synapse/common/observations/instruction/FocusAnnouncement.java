package gjum.minecraft.civ.synapse.common.observations.instruction;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Objects;

import static gjum.minecraft.civ.synapse.common.Util.sortedUniqListIgnoreCase;

public class FocusAnnouncement extends ObservationImpl {
	@Expose
	public static final String msgType = "FocusAnnouncement";

	@Expose
	@Nonnull
	public final Collection<String> accounts;

	public FocusAnnouncement(
			@Nonnull String witness,
			@Nonnull Collection<String> accounts
	) {
		super(witness);
		accounts = sortedUniqListIgnoreCase(accounts);
		this.accounts = accounts;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof FocusAnnouncement)) return false;
		if (!super.equals(o)) return false;
		FocusAnnouncement other = (FocusAnnouncement) o;
		return Objects.equals(accounts, other.accounts);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), accounts);
	}

	@Override
	public String getMsgType() {
		return msgType;
	}
}
