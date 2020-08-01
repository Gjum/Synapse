package gjum.minecraft.civ.synapse.mixin;

import gjum.minecraft.civ.synapse.LiteModSynapse;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static gjum.minecraft.civ.synapse.McUtil.getDisplayNameFromTablist;

@Mixin(GuiPlayerTabOverlay.class)
public abstract class MixinGuiPlayerTabOverlay {
	@Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
	protected void getPlayerNameHandler(NetworkPlayerInfo info, CallbackInfoReturnable<String> cir) {
		try {
			if (!LiteModSynapse.instance.isModActive()) return;
			if (!LiteModSynapse.instance.config.isReplaceTablistColors()) return;
			final String account = getDisplayNameFromTablist(info);
			ITextComponent displayName = LiteModSynapse.instance.getDisplayNameForAccount(account);
			if (displayName != null) cir.setReturnValue(displayName.getFormattedText());
		} catch (Throwable e) {
			e.printStackTrace();
		}
		// continue method normally
	}
}
