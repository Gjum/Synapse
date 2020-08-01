package gjum.minecraft.civ.synapse.common.observations;

import javax.annotation.Nonnull;

public interface Observation {
	long getTime();

	@Nonnull
	String getWitness();

	String getMsgType();
}
