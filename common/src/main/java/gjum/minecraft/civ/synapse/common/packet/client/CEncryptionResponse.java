package gjum.minecraft.civ.synapse.common.packet.client;

import gjum.minecraft.civ.synapse.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public class CEncryptionResponse extends Packet {
	public final byte[] sharedSecret;
	public final byte[] verifyToken;

	public CEncryptionResponse(byte[] sharedSecret, byte[] verifyToken) {
		this.sharedSecret = sharedSecret;
		this.verifyToken = verifyToken;
	}

	public static Packet read(ByteBuf buf) {
		return new CEncryptionResponse(readByteArray(buf), readByteArray(buf));
	}

	@Override
	public void write(ByteBuf out) {
		writeByteArray(out, sharedSecret);
		writeByteArray(out, verifyToken);
	}
}
