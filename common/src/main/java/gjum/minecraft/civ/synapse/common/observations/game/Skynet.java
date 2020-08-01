package gjum.minecraft.civ.synapse.common.observations.game;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.observations.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public class Skynet extends ObservationImpl implements AccountObservation {
	@Expose
	public static final String msgType = "Skynet";

	@Expose
	@Nonnull
	public final UUID uuid;

	@Expose
	@Nonnull
	public final String account;

	@Expose
	@Nonnull
	public final Action action;

	@Expose
	@Nullable
	public final Integer gamemode;

	public Skynet(
			@Nonnull String witness,
			@Nonnull UUID uuid,
			@Nonnull String account,
			@Nonnull Action action,
			@Nullable Integer gamemode
	) {
		super(witness);
		this.uuid = uuid;
		this.account = account;
		this.action = action;
		this.gamemode = gamemode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Skynet)) return false;
		if (!super.equals(o)) return false;
		Skynet other = (Skynet) o;
		return Objects.equals(uuid, other.uuid)
				&& action == other.action
				&& Objects.equals(account, other.account);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), uuid, action, account);
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
