package gjum.minecraft.civ.synapse.server.connection;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import gjum.minecraft.civ.synapse.common.encryption.DecryptStage;
import gjum.minecraft.civ.synapse.common.encryption.EncryptStage;
import gjum.minecraft.civ.synapse.common.packet.JsonPacket;
import gjum.minecraft.civ.synapse.common.packet.client.*;
import gjum.minecraft.civ.synapse.common.packet.server.SEncryptionRequest;
import gjum.minecraft.civ.synapse.server.ClientSession;
import gjum.minecraft.civ.synapse.server.Server;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import io.netty.util.internal.ThreadLocalRandom;
import okhttp3.*;

import javax.annotation.Nullable;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;

import static gjum.minecraft.civ.synapse.common.Util.addDashesToUuid;
import static gjum.minecraft.civ.synapse.common.Util.bytesToHex;

public class ServerHandler extends ChannelInboundHandlerAdapter {
	private static final Gson gson = new Gson();

	private final Server server;
	private final OkHttpClient httpClient = new OkHttpClient();

	public ServerHandler(Server server) {
		this.server = server;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		server.getOrCreateClient(ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (cause instanceof IOException && "Connection reset by peer".equals(cause.getMessage())) return;

		cause.printStackTrace();

		if (cause instanceof DecoderException) {
			ClientSession client = server.getOrCreateClient(ctx.channel());
			client.addDisconnectReason("DecoderException: " + cause);
			ctx.channel().disconnect();
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object packet) {
		ClientSession client = server.getOrCreateClient(ctx.channel());
		if (!client.isHandshaked()) {
			if (!(packet instanceof CHandshake)) {
				server.kick(client, "Expected Handshake, got " + packet);
				return;
			}

			CHandshake handshake = (CHandshake) packet;
			client.synapseVersion = handshake.synapseVersion;
			client.clientAccount = handshake.username;

			if (!isValidUsername(handshake.username)) {
				server.kick(client, "Handshake username contains illegal characters: '" + handshake.username + "'");
				return;
			}

			client.verifyToken = new byte[4];
			ThreadLocalRandom.current().nextBytes(client.verifyToken);

			final String infoMessage = server.handleClientHandshaking(client, handshake);

			client.send(new SEncryptionRequest(
					server.getPublicKey(),
					client.verifyToken,
					infoMessage));
			return;
		}
		if (!client.isAuthenticated()) {
			if (!(packet instanceof CEncryptionResponse)) {
				Server.log(client, Level.WARNING, "Expected encryption response, received " + packet);
				ctx.disconnect();
				return;
			}
			final CEncryptionResponse encryptionResponse = (CEncryptionResponse) packet;

			final byte[] sharedSecret;
			final byte[] verifyToken;
			try {
				sharedSecret = server.decrypt(encryptionResponse.sharedSecret);
				verifyToken = server.decrypt(encryptionResponse.verifyToken);
			} catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
				Server.log(client, Level.WARNING, "Could not decrypt shared secret/verify token: " + e);
				ctx.disconnect();
				return;
			}
			if (!Arrays.equals(verifyToken, client.verifyToken)) {
				Server.log(client, Level.WARNING, "Verify token invalid. Got: '" + bytesToHex(verifyToken)
						+ "' expected: '" + bytesToHex(client.verifyToken) + "'");
				ctx.disconnect();
				return;
			}

			final String sha;
			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-1");
				digest.update(sharedSecret);
				digest.update(server.getPublicKey().getEncoded());
				sha = new BigInteger(digest.digest()).toString(16);
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}

			final Request hasJoined = new Request.Builder()
					.url("https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + client.clientAccount + "&serverId=" + sha)
					.build();
			try (Response response = httpClient.newCall(hasJoined).execute()) {
				if (response.code() != 200) {
					Server.log(client, Level.WARNING, "Mojang response not OK. code: " + response.code());
					ctx.disconnect();
					return;
				}

				final JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
				final String mojangAccount = json.get("name").getAsString();
				final UUID mojangUuid = UUID.fromString(addDashesToUuid(json.get("id").getAsString()));
				if (!mojangAccount.equalsIgnoreCase(client.clientAccount)) {
					server.kick(client, "Username mismatch. Client: '" + client.clientAccount + "' Mojang: '" + mojangAccount + "'");
					return;
				}
				final String civRealmsAccount = server.uuidMapper.getAccountForUuid(mojangUuid);
				client.setAccountInfo(mojangUuid, mojangAccount, civRealmsAccount);

				final SecretKey key = new SecretKeySpec(sharedSecret, "AES");
				ctx.pipeline()
						.addFirst("encrypt", new EncryptStage(key))
						.addFirst("decrypt", new DecryptStage(key));

				server.handleClientAuthenticated(client);
			} catch (IOException e) {
				Server.log(client, Level.WARNING, "Error while authenticating");
				e.printStackTrace();
				ctx.disconnect();
			}
			return;
		}
		if (packet instanceof JsonPacket) {
			server.handleJsonPacket(client, (JsonPacket) packet);
			return;
		} else if (packet instanceof CWhitelist) {
			server.handleWhitelistPacket(client, (CWhitelist) packet);
			return;
		}
		Server.log(client, Level.WARNING, "Received unexpected packet " + packet);
	}

	private static boolean isValidUsername(@Nullable String name) {
		if (name == null || name.length() > 16 || name.isEmpty()) {
			return false;
		}
		for (char c : name.toCharArray()) {
			if (!('a' <= c && c <= 'z')
					&& !('A' <= c && c <= 'Z')
					&& !('0' <= c && c <= '9')
					&& c != '_') {
				return false;
			}
		}
		return true;
	}
}
