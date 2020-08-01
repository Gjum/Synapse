package gjum.minecraft.civ.synapse.server.connection;

import gjum.minecraft.civ.synapse.common.packet.Packet;
import gjum.minecraft.civ.synapse.common.packet.server.SEncryptionRequest;
import gjum.minecraft.civ.synapse.common.packet.JsonPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ServerPacketEncoder extends MessageToByteEncoder<Packet> {
	public static int getClientPacketId(Packet packet) {
		if (packet instanceof SEncryptionRequest) return 0;
		if (packet instanceof JsonPacket) return 1;
		throw new IllegalArgumentException("Unknown server packet class " + packet);
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) {
		out.writeByte(getClientPacketId(packet));
		packet.write(out);
	}
}
