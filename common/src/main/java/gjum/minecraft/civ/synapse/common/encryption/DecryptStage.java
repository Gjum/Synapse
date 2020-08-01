package gjum.minecraft.civ.synapse.common.encryption;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.List;

public class DecryptStage extends MessageToMessageDecoder<ByteBuf> {
	private final CipherUtils decipherUtils;

	public DecryptStage(Key key) {
		try {
			final Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(key.getEncoded()));
			decipherUtils = new CipherUtils(cipher);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws ShortBufferException {
		out.add(decipherUtils.decipher(ctx, in));
	}
}
