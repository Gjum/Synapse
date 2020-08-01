package gjum.minecraft.civ.synapse.common.observations;

import gjum.minecraft.civ.synapse.common.Pos;

import javax.annotation.Nonnull;

public interface PosObservation extends Observation {
	@Nonnull
	Pos getPos();

	@Nonnull
	String getWorld();
}
