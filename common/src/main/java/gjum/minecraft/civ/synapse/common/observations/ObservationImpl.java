package gjum.minecraft.civ.synapse.common.observations;

import com.google.gson.annotations.Expose;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ObservationImpl implements Observation {
	@Expose
	@Nonnull
	public String witness;

	@Expose
	public long time = System.currentTimeMillis();

	@Expose
	@Nullable
	public String messagePlain;

	public ObservationImpl(@Nonnull String witness) {
		this.witness = witness;
	}

	@Override
	public long getTime() {
		return time;
	}

	@Nonnull
	@Override
	public String getWitness() {
		return witness;
	}

	public ObservationImpl setMessagePlain(String messagePlain) {
		this.messagePlain = messagePlain;
		return this;
	}
}
