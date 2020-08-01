package gjum.minecraft.civ.synapse.common.packet.client;

import gjum.minecraft.civ.synapse.common.packet.Packet;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.*;

public class CWhitelist extends Packet {
	@Nonnull
	public Collection<String> accounts;
	@Nonnull
	public Collection<UUID> uuids;

	public CWhitelist(
			@Nonnull Collection<String> accounts,
			@Nonnull Collection<UUID> uuids
	) {
		this.accounts = accounts;
		this.uuids = uuids;
	}

	public static Packet read(ByteBuf buf) {
		final List<String> accounts = Arrays.asList(readString(buf).split(" "));
		final Collection<UUID> uuids = new ArrayList<>();
		int numUuids = buf.readInt();
		for (int i = 0; i < numUuids; i++) {
			uuids.add(new UUID(buf.readLong(), buf.readLong()));
		}
		return new CWhitelist(accounts, uuids);
	}

	@Override
	public void write(ByteBuf buf) {
		writeOptionalString(buf, String.join(" ", accounts));
		buf.writeInt(uuids.size());
		for (UUID uuid : uuids) {
			buf.writeLong(uuid.getMostSignificantBits());
			buf.writeLong(uuid.getLeastSignificantBits());
		}
	}

	@Override
	public String toString() {
		return "CWhitelist{accounts=" + String.join(" ", accounts) + '}';
	}
}
