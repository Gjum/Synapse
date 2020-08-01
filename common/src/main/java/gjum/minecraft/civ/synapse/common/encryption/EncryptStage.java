package gjum.minecraft.civ.synapse.common.encryption;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import java.security.GeneralSecurityException;
import java.security.Key;

public class EncryptStage extends MessageToByteEncoder<ByteBuf> {
	private final CipherUtils encipherUtils;

	public EncryptStage(Key key) {
		try {
			final Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(key.getEncoded()));
			encipherUtils = new CipherUtils(cipher);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws ShortBufferException {
		encipherUtils.encipher(in, out);
	}
}
