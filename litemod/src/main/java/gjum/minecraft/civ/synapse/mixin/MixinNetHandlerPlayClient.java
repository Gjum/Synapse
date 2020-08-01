package gjum.minecraft.civ.synapse.mixin;

import com.mumfrey.liteloader.common.ducks.IChatPacket;
import gjum.minecraft.civ.synapse.LiteModSynapse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {
	@Inject(method = "handleChat", at = @At("HEAD"), cancellable = true)
	protected void onHandleChat(SPacketChat packetIn, CallbackInfo ci) {
		if (!Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
			// waiting for mc to call this again from the mc thread
			// continue method normally
			return;
		}
		try {
			final ITextComponent original = packetIn.getChatComponent();
			final ITextComponent replacement = LiteModSynapse.instance.handleChat(original);
			if (replacement == null) {
				ci.cancel(); // drop packet
				return;
			}
			if (replacement.equals(original)) return;
			((IChatPacket) packetIn).setChatComponent(replacement);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		// continue method normally
	}
}
