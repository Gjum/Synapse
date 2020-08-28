package gjum.minecraft.civ.synapse.server;

import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;
import gjum.minecraft.civ.synapse.common.observations.PlayerTracker;
import gjum.minecraft.civ.synapse.common.observations.accountpos.AccountPosObservation;
import gjum.minecraft.civ.synapse.common.observations.game.Skynet;
import gjum.minecraft.civ.synapse.common.packet.JsonPacket;
import gjum.minecraft.civ.synapse.common.packet.Packet;
import gjum.minecraft.civ.synapse.common.packet.client.CHandshake;
import gjum.minecraft.civ.synapse.common.packet.client.CWhitelist;
import gjum.minecraft.civ.synapse.server.connection.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.*;
import java.io.File;
import java.security.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static gjum.minecraft.civ.synapse.common.Util.intOrNull;
import static gjum.minecraft.civ.synapse.common.Util.nonNullOr;

public class Server {
	private final long connectRateLimitWindow = nonNullOr(intOrNull(System.getenv("CONNECT_RATE_LIMIT_WINDOW")), 60 * 1000); // 1min
	private final int connectRateLimitCount = nonNullOr(intOrNull(System.getenv("CONNECT_RATE_LIMIT_COUNT")), 7); // 7 connections over the past rateLimitWindow

	private static final Logger logger = LoggerFactory.getLogger("Server");
	private KeyPair keyPair;
	private final int port;
	private final String latestModVersion;
	private final String allowedModVersionPart;
	private final String modUpdateLink;
	private final String gameAddressSuffix;
	private final long statsInterval;

	public final UuidsConfig uuidMapper = new UuidsConfig();
	private final AccountsListConfig userList = new AccountsListConfig(uuidMapper);
	private final AccountsListConfig adminList = new AccountsListConfig(uuidMapper);

	private final HashMap<UUID, ClientSession> connectedPlayers = new HashMap<>();
	private final PlayerTracker playerTracker = new PlayerTracker(null);

	private final HashMap<String, Collection<Long>> rateLimitTracker = new HashMap<>();
	private long ingressCountInLastInterval = 0;
	private long egressCountInLastInterval = 0;

	public static void main(String[] args) {
		try {
			Server server = new Server();
			server.run();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public Server() throws NoSuchAlgorithmException {
		port = Integer.parseInt(nonNullOr(System.getenv("PORT"), "22001"));
		latestModVersion = nonNullOr(System.getenv("LATEST_MOD_VERSION"), "2.0.0");
		allowedModVersionPart = nonNullOr(System.getenv("ALLOWED_MOD_VERSION_PART"),
				String.join(".", Arrays.copyOf(latestModVersion.split("\\.|-"), 2)));
		modUpdateLink = nonNullOr(System.getenv("MOD_UPDATE_LINK"), "github.com/Gjum/Synapse");
		gameAddressSuffix = nonNullOr(System.getenv("GAME_ADDRESS"), "civrealms.com").toLowerCase(); // empty string: allow all
		statsInterval = 1000 * Integer.parseInt(nonNullOr(System.getenv("STATS_INTERVAL"), "300"));

		String uuidMapperPath = nonNullOr(System.getenv("UUID_MAPPER_PATH"), "uuids.tsv");
		new File(uuidMapperPath).getAbsoluteFile().getParentFile().mkdirs();
		uuidMapper.load(new File(uuidMapperPath)); // must be loaded first; others depend on it during loading
		uuidMapper.saveLater(null);

		String userListPath = nonNullOr(System.getenv("USER_LIST_PATH"), "users.tsv");
		new File(userListPath).getAbsoluteFile().getParentFile().mkdirs();
		userList.load(new File(userListPath));
		userList.saveLater(null);

		String adminListPath = nonNullOr(System.getenv("ADMIN_LIST_PATH"), "admins.tsv");
		new File(adminListPath).getAbsoluteFile().getParentFile().mkdirs();
		adminList.load(new File(adminListPath));
		adminList.saveLater(null);

		logger.info("Starting server. git=@GIT_REF@" +
				" PORT=" + port +
				" ALLOWED_MOD_VERSION_PART=" + allowedModVersionPart +
				" GAME_ADDRESS=" + gameAddressSuffix);
		generateKeyPair();
	}

	public void run() throws InterruptedException {
		EventLoopGroup parentGroup = new NioEventLoopGroup();
		EventLoopGroup childGroup = new NioEventLoopGroup();
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(parentGroup, childGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) {
						getOrCreateClient(ch);

						if (checkRateLimit(ch.remoteAddress().getHostString())) {
							final String ipAddressFull = ch.remoteAddress().toString();
							log(ipAddressFull, Level.WARNING, "Hit rate limit, disconnecting");
							ch.close();
							return;
						}

						ch.closeFuture().addListener((ChannelFutureListener) f -> {
							handleClientDisconnected(getOrCreateClient(f.channel()));
						});

						ch.pipeline()
								.addLast("decoder", new ClientPacketDecoder())
								.addLast("encoder", new ServerPacketEncoder())
								.addLast("handler", new ServerHandler(Server.this));
					}
				})
				.option(ChannelOption.SO_BACKLOG, 128)
				.childOption(ChannelOption.SO_KEEPALIVE, true);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			childGroup.shutdownGracefully();
			parentGroup.shutdownGracefully();
		}));

		log("Server", Level.INFO, "Started.");

		// Bind and start to accept incoming connections.
		ChannelFuture f = bootstrap.bind(port);
		f.sync();

		final String intervalMinStr = new DecimalFormat("#.#").format(statsInterval / 60000f) + "min";
		final long initialDelay = statsInterval - (System.currentTimeMillis() % statsInterval);
		parentGroup.scheduleAtFixedRate(() -> {
			if (ingressCountInLastInterval != 0 && egressCountInLastInterval != 0) {
				logger.info("ingress: " + ingressCountInLastInterval
						+ " egress: " + egressCountInLastInterval
						+ " over past " + intervalMinStr);
			}
			ingressCountInLastInterval = 0;
			egressCountInLastInterval = 0;
		}, initialDelay, statsInterval, TimeUnit.MILLISECONDS);

		// Wait until the server socket is closed.
		f.channel().closeFuture().sync();
	}

	@Nonnull
	synchronized public ClientSession getOrCreateClient(@Nonnull Channel channel) {
		final Attribute<ClientSession> attribute = channel.attr(AttributeKey.valueOf("client"));
		ClientSession client = attribute.get();
		if (client == null) {
			client = new ClientSession(channel);
			attribute.set(client);
			handleClientConnected(client);
		}
		return client;
	}

	/**
	 * @return true iff the connection should be denied
	 */
	synchronized private boolean checkRateLimit(String source) {
		Collection<Long> lastConnectTimes = rateLimitTracker.computeIfAbsent(source, s -> new ArrayList<>(2));
		final long now = System.currentTimeMillis();
		lastConnectTimes.removeIf(t -> t < now - connectRateLimitWindow);
		lastConnectTimes.add(now);
		return lastConnectTimes.size() > connectRateLimitCount;
	}

	synchronized private void generateKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(1024);
		keyPair = generator.genKeyPair();
	}

	public PublicKey getPublicKey() {
		return keyPair.getPublic();
	}

	public byte[] decrypt(byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException,
			BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
		return cipher.doFinal(data);
	}

	public static void log(@Nonnull ClientSession client, @Nonnull Level level, @Nonnull String msg) {
		log(getSessionDescriptor(client), level, msg, null);
	}

	public static void log(@Nonnull ClientSession client, @Nonnull Level level, @Nonnull String msg, @Nullable Throwable e) {
		log(getSessionDescriptor(client), level, msg, e);
	}

	public static void log(@Nonnull String sessionDescriptor, @Nonnull Level level, @Nonnull String msg) {
		logToLogger(sessionDescriptor, level, msg, null);
	}

	public static void log(@Nonnull String sessionDescriptor, @Nonnull Level level, @Nonnull String msg, @Nullable Throwable e) {
		logToLogger(sessionDescriptor, level, msg, e);
	}

	public static void logToLogger(@Nonnull String sessionDescriptor, @Nonnull Level level, @Nonnull String msg, @Nullable Throwable e) {
		// TODO use slf4j directly
		final String msgFull = "[" + sessionDescriptor + "] " + msg;
		if (Level.FINE.equals(level)) {
			logger.debug(msgFull);
		} else if (Level.INFO.equals(level)) {
			logger.info(msgFull);
		} else if (Level.WARNING.equals(level)) {
			logger.warn(msgFull);
		} else if (Level.SEVERE.equals(level)) {
			logger.error(msgFull, e);
		} else {
			logger.error("[" + level + "] " + msgFull);
		}
	}

	private static String getSessionDescriptor(@Nonnull ClientSession client) {
		String sessionDescriptor = client.channel.id().toString();
		if (client.getCivRealmsAccount() != null) {
			sessionDescriptor += " " + client.getCivRealmsAccount();
		} else if (client.clientAccount != null) {
			sessionDescriptor += " (" + client.clientAccount + ")";
		}
		return sessionDescriptor;
	}

	public void kick(@Nonnull ClientSession client, @Nonnull String reason) {
		client.addDisconnectReason("Kicked: " + reason);
		log(client, Level.WARNING, client.disconnectReason);
		client.channel.disconnect();
	}

	private boolean isAllowedGameServer(@Nonnull ClientSession client) {
		if (client.gameAddress == null) return false;
		return client.gameAddress.toLowerCase()
				.split(":")[0] // allow any port
				.endsWith(gameAddressSuffix); // allow subdomains
	}

	public void handleClientConnected(ClientSession client) {
		log(client, Level.INFO, "Connected");
	}

	synchronized public String handleClientHandshaking(ClientSession client, CHandshake handshake) {
		client.gameAddress = handshake.gameAddress;
		if (!isAllowedGameServer(client)) {
			kick(client, "Invalid game server: " + handshake.gameAddress);
		}

		client.whitelisted = true;
		log(client, Level.INFO, "Handshaking: " + handshake);

		String statusMessage = "Daily reminder: CANTINA SUCKS COCK.";
		@Nullable final String versionPart = client.synapseVersion == null ? null
				: client.synapseVersion.split("\\+-", 2)[0]; // allow any build info
		if (versionPart == null || (!versionPart.startsWith(allowedModVersionPart)
				&& !versionPart.equals("@" + "VERSION" + "@")
				&& !versionPart.equals("Development" + "_" + "Build")
		)) {
			statusMessage = "Update available: " + latestModVersion + " " + modUpdateLink;
		}
		return statusMessage;
	}

	synchronized public void handleClientAuthenticated(ClientSession client) {
		// allow maximum of one connection per player
		synchronized (this) {
			ClientSession prevSession = connectedPlayers.put(client.getMojangUuid(), client);
			if (prevSession != null) {
				kick(prevSession, "Connected from another location");
			}
		}

		UUID mojUuid = client.getMojangUuid();
		String mojAccount = client.getMojangAccount();
		log(client, Level.INFO, "Authenticated as " + mojAccount
				+ " " + mojUuid
				+ " CivRealms name: " + client.getCivRealmsAccount());

		if (!userList.contains(client.getMojangUuid())) {
			kick(client, "Not whitelisted: " + mojUuid + " as " + mojAccount + " (?)");
		}
	}

	synchronized private void handleClientDisconnected(ClientSession client) {
		log(client, Level.INFO, "Disconnected");
		connectedPlayers.remove(client.getMojangUuid());
	}

	synchronized public void handleJsonPacket(ClientSession client, JsonPacket packet) {
		final long now = System.currentTimeMillis();
		ingressCountInLastInterval++;

		if (!client.whitelisted) return;
		if (client.getCivRealmsAccount() == null) return;
		final Object payload = packet.getPayload();
		if (payload == null) return;

		if (payload instanceof ObservationImpl) {
			final ObservationImpl observation = (ObservationImpl) payload;

			// in case client is broken or malicious
			observation.witness = client.getCivRealmsAccount();
			observation.time = Math.max(now - 5000, Math.min(observation.time, now));

			final boolean isNew = playerTracker.recordObservation(observation);
			if (!isNew) return;

			if (observation instanceof AccountPosObservation) {
				broadcastPacket(client, new JsonPacket(observation));

				// TODO relay snitches+groupchat to team discord channel
			} else if (observation instanceof Skynet) {
				// TODO relay skynet to team discord channel
			}
		}
	}

	private void broadcastPacket(ClientSession origin, Packet packet) {
		for (ClientSession receiver : new ArrayList<>(connectedPlayers.values())) {
			if (!receiver.whitelisted) continue;
			if (receiver == origin) continue;
			if (!isAllowedGameServer(receiver)) continue;

			receiver.send(packet);
			egressCountInLastInterval++;
		}
	}

	synchronized public void handleWhitelistPacket(ClientSession admin, CWhitelist whitelistPkt) {
		adminList.load(null); // load before every check, may have been changed manually
		if (!adminList.contains(admin.getMojangUuid())) {
			log(admin, Level.WARNING, "Disallowed changing whitelist to: " + String.join(" ", whitelistPkt.accounts));
			return;
		}

		userList.setList(whitelistPkt.accounts);
		log(admin, Level.WARNING, "Changed whitelist to: " + String.join(" ", whitelistPkt.accounts));

		for (ClientSession client : connectedPlayers.values()) {
			if (client != admin && !userList.contains(client.getMojangUuid())) {
				kick(client, "Removed from whitelist");
			}
		}
	}
}
