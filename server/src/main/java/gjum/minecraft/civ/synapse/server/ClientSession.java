package gjum.minecraft.civ.synapse.server;

import gjum.minecraft.civ.synapse.common.packet.Packet;
import io.netty.channel.Channel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;

public class ClientSession {
	public final long connectTime = System.currentTimeMillis();
	@Nonnull
	public final Channel channel;
	@Nullable
	public String synapseVersion = null;
	/**
	 * arbitrary, as sent by client during handshake. use with caution
	 */
	@Nullable
	public String clientAccount = null;
	@Nullable
	public byte[] verifyToken = null;
	@Nullable
	private UUID mojangUuid = null;
	@Nullable
	private String mojangAccount = null;
	@Nullable
	private String civRealmsAccount = null;
	public boolean whitelisted = false;
	public String gameAddress = null;
	@Nullable
	public String disconnectReason = null;

	public ClientSession(@Nonnull Channel channel) {
		this.channel = channel;
	}

	@Nullable
	public UUID getMojangUuid() {
		return mojangUuid;
	}

	@Nullable
	public String getMojangAccount() {
		return mojangAccount;
	}

	public void setAccountInfo(
			@Nullable UUID mojangUuid,
			@Nullable String mojangAccount,
			@Nullable String civRealmsAccount
	) {
		this.mojangUuid = mojangUuid;
		this.mojangAccount = mojangAccount;
		this.civRealmsAccount = civRealmsAccount;
	}

	@Nullable
	public String getCivRealmsAccount() {
		return civRealmsAccount;
	}

	public boolean isHandshaked() {
		return verifyToken != null;
	}

	public boolean isAuthenticated() {
		return mojangUuid != null && mojangAccount != null;
	}

	public void send(Packet packet) {
		if (channel.isOpen()) {
			channel.writeAndFlush(packet);
		} else {
			Server.log(this, Level.WARNING, "Connection already closed; dropping packet " + packet);
		}
	}

	public void addDisconnectReason(String reason) {
		if (disconnectReason != null) {
			reason += " - " + disconnectReason;
		}
		disconnectReason = reason;
	}
}
