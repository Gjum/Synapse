package gjum.minecraft.civ.synapse.common.observations.game;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.observations.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class GroupChat extends ObservationImpl implements AccountObservation, GroupObservation {
	@Expose
	public static final String msgType = "GroupChat";

	@Expose
	@Nullable
	public final String group;
	@Expose
	@Nonnull
	public final String account;
	@Expose
	@Nonnull
	public final String message;

	public GroupChat(
			@Nonnull String witness,
			@Nullable String group,
			@Nonnull String account,
			@Nonnull String message
	) {
		super(witness);
		this.group = group;
		this.account = account;
		this.message = message;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		GroupChat other = (GroupChat) o;
		return Objects.equals(group, other.group) &&
				Objects.equals(account, other.account) &&
				Objects.equals(message, other.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), group, account, message);
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

	@Nullable
	@Override
	public String getGroup() {
		return group;
	}
}
