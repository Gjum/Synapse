package gjum.minecraft.civ.synapse.connection;

import gjum.minecraft.civ.synapse.LiteModSynapse;
import gjum.minecraft.civ.synapse.common.packet.JsonPacket;
import gjum.minecraft.civ.synapse.common.packet.Packet;
import gjum.minecraft.civ.synapse.common.packet.client.CHandshake;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static gjum.minecraft.civ.synapse.LiteModSynapse.MOD_NAME;

public class Client {
	final Logger logger = LogManager.getLogger(this);
	@Nonnull
	public String address;
	private int retrySec = 5;
	private boolean autoReconnect = true;
	private boolean isEncrypted = false;
	@Nullable
	private Channel channel;
	@Nullable
	private EventLoopGroup workerGroup;
	private final ArrayList<Packet> queue = new ArrayList<>();

	public Client(@Nonnull String address) {
		this.address = address;
	}

	public Client connect() {
		try {
			ChannelFuture channelFuture;
			synchronized (this) {
				if (address == null || address.trim().isEmpty() || !address.contains(":")) {
					logger.warn("[" + MOD_NAME + "] Ignoring connection request to invalid address: '" + address + "'");
					return this;
				}
				if (isConnected()) {
					final String currentAddress = channel.remoteAddress().toString().split("/")[0].toLowerCase();
					if (address.toLowerCase().equals(currentAddress)) {
						logger.warn("[" + MOD_NAME + "] Already connected to '"
								+ currentAddress + "'. Reconnecting to '" + address + "'.");
					}
					disconnect();
				}
				String[] split = address.split(":");
				if (split.length != 2) {
					throw new IllegalArgumentException("Expected host:port but got " + address);
				}
				final String host = split[0];
				final int port = Integer.parseInt(split[1]);

				isEncrypted = false;
				autoReconnect = true;

				Bootstrap bootstrap = new Bootstrap();
				if (workerGroup != null) workerGroup.shutdownGracefully();
				workerGroup = new NioEventLoopGroup();
				bootstrap.group(workerGroup);
				bootstrap.channel(NioSocketChannel.class);
				bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
				bootstrap.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) {
						ch.pipeline().addLast(
								new ServerPacketDecoder(),
								new ClientPacketEncoder(),
								new ClientHandler(Client.this));
					}
				});

				channelFuture = bootstrap.connect(host, port);
				channelFuture.addListener((ChannelFutureListener) future -> {
					if (!future.isSuccess()) {
						handleDisconnect(future.cause());
					}
				});
				channel = channelFuture.channel();
			}
			channelFuture.sync();

			if (channel != null) {
				logger.info("[" + MOD_NAME + "] Connected to " + address);

				channel.writeAndFlush(new CHandshake(
						LiteModSynapse.instance.getVersion(),
						Minecraft.getMinecraft().getSession().getUsername(),
						LiteModSynapse.instance.gameAddress));

				LiteModSynapse.instance.handleCommsConnected();
			}
		} catch (Throwable e) {
			if (e.getMessage() == null || !e.getMessage().startsWith("Connection refused: ")) { // reduce spam
				logger.error("[" + MOD_NAME + "] Connection to '" + address + "' failed: " + e);
				e.printStackTrace();
			}
		}
		return this;
	}

	void handleDisconnect(@Nonnull Throwable cause) {
		isEncrypted = false;
		LiteModSynapse.instance.handleCommsDisconnected(cause);
		if (!autoReconnect) {
			logger.warn("[" + MOD_NAME + "] Connection to '" + address + "' failed." +
					" Won't retry (autoReconnect=false). Cause: " + cause);
		} else if (workerGroup == null) {
			logger.warn("[" + MOD_NAME + "] Connection to '" + address + "' failed." +
					" Won't retry (workerGroup=null). Cause: " + cause);
		} else {
			workerGroup.schedule(this::connect, retrySec, TimeUnit.SECONDS);
			if (!cause.getMessage().startsWith("Connection refused: ")) { // reduce spam
				logger.warn("[" + MOD_NAME + "] Connection to '" + address + "' failed." +
						" Retrying in " + retrySec + " sec. Cause: " + cause);
			}
		}
	}

	public void handleEncryptionSuccess(String message) {
		isEncrypted = true;
		LiteModSynapse.instance.handleCommsEncryptionSuccess(message);
		if (!queue.isEmpty() && isConnected()) {
			final ArrayList<Packet> packets;
			synchronized (queue) {
				packets = new ArrayList<>(queue);
				queue.clear();
			}
			packets.forEach(this::sendEncrypted);
		}
	}

	boolean isConnected() {
		return channel != null && channel.isActive();
	}

	public boolean isEncrypted() {
		return isConnected() && isEncrypted;
	}

	public void sendEncrypted(Packet packet) {
		sendEncrypted(packet, true);
	}

	public void sendEncrypted(Packet packet, boolean flush) {
		try {
			if (isEncrypted() && channel != null && channel.isActive()) {
				if (flush) channel.writeAndFlush(packet);
				else channel.write(packet);
				return;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		synchronized (queue) {
			queue.add(packet);
			if (queue.size() > 1000) {
				final ArrayList<Packet> all = new ArrayList<>(queue);
				queue.clear();
				queue.addAll(all.subList(500, all.size()));
			}
		}
	}

	public synchronized void disconnect() {
		autoReconnect = false;
		if (channel != null) {
			channel.disconnect();
			channel.eventLoop().shutdownGracefully();
			channel = null;
		}
		if (workerGroup != null && !workerGroup.isShuttingDown()) {
			workerGroup.shutdownGracefully();
			workerGroup = null;
		}
	}

	public void handleJsonPacket(JsonPacket packet) {
		LiteModSynapse.instance.handleCommsJson(packet.getPayload());
	}
}
