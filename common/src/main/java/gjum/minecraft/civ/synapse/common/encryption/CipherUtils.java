package gjum.minecraft.civ.synapse.common.encryption;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

public class CipherUtils {
	private final Cipher cipher;

	protected CipherUtils(Cipher cipher) {
		this.cipher = cipher;
	}

	protected ByteBuf decipher(ChannelHandlerContext ctx, ByteBuf in) throws ShortBufferException {
		final int inLen = in.readableBytes();
		final byte[] inBytes = bufToBytes(in);
		final ByteBuf outBuf = ctx.alloc().heapBuffer(cipher.getOutputSize(inLen));
		outBuf.writerIndex(cipher.update(inBytes, 0, inLen, outBuf.array(), outBuf.arrayOffset()));
		return outBuf;
	}

	protected void encipher(ByteBuf in, ByteBuf out) throws ShortBufferException {
		final int inLen = in.readableBytes();
		final int outLen = cipher.getOutputSize(inLen);
		final byte[] outBytes = new byte[outLen];
		final byte[] inBytes = bufToBytes(in);
		out.writeBytes(outBytes, 0, cipher.update(inBytes, 0, inLen, outBytes));
	}

	private byte[] bufToBytes(ByteBuf in) {
		final int i = in.readableBytes();
		final byte[] inputBuffer = new byte[i];
		in.readBytes(inputBuffer, 0, i);
		return inputBuffer;
	}
}
