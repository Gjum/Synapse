package gjum.minecraft.civ.synapse;

import gjum.minecraft.civ.synapse.common.observations.PlayerTracker;
import gjum.minecraft.civ.synapse.common.observations.accountpos.AccountPosObservation;
import gjum.minecraft.civ.synapse.common.observations.accountpos.PlayerState;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static gjum.minecraft.civ.synapse.McUtil.*;

public class PlayerTrackerIngame extends PlayerTracker {
	public PlayerTrackerIngame(@Nullable String gameAddress) {
		super(gameAddress);
	}

	@Override
	@Nullable
	public AccountPosObservation getMostRecentPosObservationForAccount(@Nonnull String account) {
		if (getMc().world != null) {
			final EntityPlayer player = getMc().world.getPlayerEntityByName(account);
			if (player != null) {
				return new PlayerState(getSelfAccount(),
						account, getEntityPosition(player), LiteModSynapse.instance.worldName);
			}
		}
		return super.getMostRecentPosObservationForAccount(account);
	}

	// TODO override getLastObservationBeforeWithSignificantMove and use radar pos
}
