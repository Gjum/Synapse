package gjum.minecraft.civ.synapse.server.connection;

import gjum.minecraft.civ.synapse.common.packet.JsonPacket;
import gjum.minecraft.civ.synapse.common.packet.Packet;
import gjum.minecraft.civ.synapse.common.packet.client.CEncryptionResponse;
import gjum.minecraft.civ.synapse.common.packet.client.CHandshake;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.io.IOException;
import java.util.List;

public class ClientPacketDecoder extends ReplayingDecoder<Void> {
	public static Packet constructClientPacket(int id, ByteBuf buf) {
		switch (id) {
			case 0:
				return CHandshake.read(buf);
			case 1:
				return CEncryptionResponse.read(buf);
			case 2:
				return JsonPacket.read(buf);
			default:
				return null;
		}
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
		final byte id = buf.readByte();
		final Packet packet = constructClientPacket(id, buf);
		if (packet == null) {
			throw new IOException("Unknown client packet id " + id + " 0x" + Integer.toHexString(id));
		}
		out.add(packet);
	}
}
