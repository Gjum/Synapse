package gjum.minecraft.civ.synapse.common.packet;

import gjum.minecraft.civ.synapse.common.Pos;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import static gjum.minecraft.civ.synapse.common.Util.nonNullOr;

public abstract class Packet {
	public abstract void write(ByteBuf buf);

	@Nonnull
	protected static String readString(@Nonnull ByteBuf in) {
		return nonNullOr(readOptionalString(in), "");
	}

	@Nullable
	protected static String readOptionalString(@Nonnull ByteBuf in) {
		final int length = in.readInt();
		if (length <= 0) return null;
		final byte[] bytes = new byte[length];
		in.readBytes(bytes);
		return new String(bytes);
	}

	protected static void writeOptionalString(@Nonnull ByteBuf out, @Nullable String string) {
		if (string == null || string.isEmpty()) {
			out.writeInt(0);
			return;
		}
		final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		out.writeInt(bytes.length);
		out.writeBytes(bytes);
	}

	@Nullable
	protected static Pos readOptionalPos(@Nonnull ByteBuf in) {
		Pos pos = null;
		if (in.readBoolean()) {
			pos = new Pos(
					in.readInt(),
					in.readInt(),
					in.readInt());
		}
		return pos;
	}

	protected static void writeOptionalPos(@Nonnull ByteBuf out, @Nullable Pos pos) {
		out.writeBoolean(pos != null);
		if (pos != null) {
			out.writeInt(pos.x);
			out.writeInt(pos.y);
			out.writeInt(pos.z);
		}
	}

	protected static byte[] readByteArray(ByteBuf in) {
		final int length = in.readInt();
		final byte[] array = new byte[length];
		in.readBytes(array);
		return array;
	}

	protected static void writeByteArray(ByteBuf out, byte[] array) {
		out.writeInt(array.length);
		out.writeBytes(array);
	}

	@Nonnull
	protected static PublicKey readKey(ByteBuf in) {
		try {
			final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(readByteArray(in));
			final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return keyFactory.generatePublic(keySpec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
