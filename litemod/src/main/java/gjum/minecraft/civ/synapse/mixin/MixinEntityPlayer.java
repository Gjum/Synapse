package gjum.minecraft.civ.synapse.mixin;

import gjum.minecraft.civ.synapse.LiteModSynapse;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends Entity {
	public MixinEntityPlayer(World worldIn) {
		super(worldIn);
	}

	@Inject(method = "getTeam", at = @At("HEAD"), cancellable = true)
	protected void getTeamHandler(CallbackInfoReturnable<Team> cir) {
		try {
			if (!LiteModSynapse.instance.isModActive()) return;
			final EntityPlayer entity = (EntityPlayer) ((Object) this);
			final Team team = LiteModSynapse.instance.config.getStandingTeam(LiteModSynapse.instance.getStanding(entity.getName()));
			if (team != null) cir.setReturnValue(team);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		// continue method normally
	}

	@Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
	protected void getDisplayNameHandler(CallbackInfoReturnable<ITextComponent> cir) {
		try {
			if (!LiteModSynapse.instance.isModActive()) return;
			ITextComponent displayName = LiteModSynapse.instance.getDisplayNameForAccount(getName());
			if (displayName != null) cir.setReturnValue(displayName);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		// continue method normally
	}
}
