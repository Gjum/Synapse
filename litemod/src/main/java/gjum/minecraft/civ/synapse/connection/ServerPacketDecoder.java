package gjum.minecraft.civ.synapse.connection;

import com.mumfrey.liteloader.util.log.LiteLoaderLogger;
import gjum.minecraft.civ.synapse.common.packet.JsonPacket;
import gjum.minecraft.civ.synapse.common.packet.Packet;
import gjum.minecraft.civ.synapse.common.packet.server.SEncryptionRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

public class ServerPacketDecoder extends ReplayingDecoder<Void> {
	public static Packet constructServerPacket(int id, ByteBuf buf) {
		switch (id) {
			case 0:
				return SEncryptionRequest.read(buf);
			case 1:
				return JsonPacket.read(buf);
			default:
				return null;
		}
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
		byte id = buf.readByte();
		final Packet packet = constructServerPacket(id, buf);
		if (packet == null) {
			LiteLoaderLogger.severe("[ServerPacketDecoder] " +
					"Unknown server packet id " + id + " 0x" + Integer.toHexString(id));
			return;
		}
		out.add(packet);
	}
}
