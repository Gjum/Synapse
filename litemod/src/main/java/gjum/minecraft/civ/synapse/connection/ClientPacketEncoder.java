package gjum.minecraft.civ.synapse.connection;

import gjum.minecraft.civ.synapse.common.packet.JsonPacket;
import gjum.minecraft.civ.synapse.common.packet.Packet;
import gjum.minecraft.civ.synapse.common.packet.client.CEncryptionResponse;
import gjum.minecraft.civ.synapse.common.packet.client.CHandshake;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ClientPacketEncoder extends MessageToByteEncoder<Packet> {
	public static int getClientPacketId(Packet packet) {
		if (packet instanceof CHandshake) return 0;
		if (packet instanceof CEncryptionResponse) return 1;
		if (packet instanceof JsonPacket) return 2;
		throw new IllegalArgumentException("Unknown client packet class " + packet);
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) {
		out.writeByte(getClientPacketId(packet));
		packet.write(out);
	}
}
