package gjum.minecraft.civ.synapse.common.observations;

import gjum.minecraft.civ.synapse.common.observations.accountpos.AccountPosObservation;
import gjum.minecraft.civ.synapse.common.observations.game.Skynet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class PlayerTracker {
	public static final int closeObservationDistance = 33; // ground distance between diagonally adjacent snitches, rounded up
	public static final int recentObservationMs = 60 * 1000; // messages older than this are not considered duplicates

	@Nullable
	public final String gameAddress;

	private List<Observation> recentObservations = new ArrayList<>();
	private final HashMap<String, List<AccountObservation>> observationsByAccount = new HashMap<>();

	public PlayerTracker(@Nullable String gameAddress) {
		this.gameAddress = gameAddress;
	}

	public boolean recordObservation(@Nonnull Observation observation) {
		boolean isNew = isNew(observation);
		if (!isNew) return isNew;

		recentObservations.add(observation);
		if (recentObservations.size() > 250) {
			// too big, clean up
			recentObservations = new ArrayList<>(recentObservations.subList(126, recentObservations.size()));
		}

		if (observation instanceof AccountObservation) {
			final AccountObservation accObs = ((AccountObservation) observation);
			final String accountLower = accObs.getAccount().toLowerCase();

			final List<AccountObservation> accObservations = observationsByAccount.computeIfAbsent(
					accountLower, k -> new ArrayList<>());
			accObservations.add(accObs);
			if (accObservations.size() > 250) {
				// too big, clean up
				observationsByAccount.put(accountLower, new ArrayList<>(
						accObservations.subList(126, accObservations.size())));
			}
		}
		return isNew;
	}

	private boolean isNew(@Nonnull Observation observation) {
		final long now = System.currentTimeMillis();
		final Iterator<Observation> iterator = recentObservations.stream()
				.sorted(Comparator.comparing(o -> -o.getTime()))
				.iterator();
		while (iterator.hasNext()) {
			final Observation candidate = iterator.next();
			if (candidate.getTime() < now - recentObservationMs) {
				return true; // no matching msgs in recent history
			}
			if (candidate.getWitness().equals(observation.getWitness())) {
				return true; // same account seen twice quickly in succession by the same client
			} else if (candidate.equals(observation)) {
				return false; // same msg was already seen by other client
			}
		}
		return true; // no matching msgs in recorded history
	}

	/**
	 * Get the last pos observation far enough from the given newer observation
	 * where the movement indicates a significant heading.
	 * E.g., if latestObs is a snitch hit, the observation before that has to be far outside that snitch field;
	 * if latestObs is a radar observation and so is the one before that, they can be very close.
	 */
	@Nullable
	public AccountPosObservation getLastObservationBeforeWithSignificantMove(@Nonnull AccountPosObservation latestObs) {
		final List<AccountObservation> prevObs = observationsByAccount.get(latestObs.getAccount().toLowerCase());
		if (prevObs == null) return null;
		final int closeDistSq = closeObservationDistance * closeObservationDistance;
		// start with latest observation, then go back in time
		for (int i = prevObs.size() - 1; i >= 0; i--) {
			final Observation observation = prevObs.get(i);
			if (observation instanceof AccountPosObservation) {
				final AccountPosObservation apo = (AccountPosObservation) observation;
				// TODO if it's an exact pos and so is latestObs, they can be very close
				if (closeDistSq < latestObs.getPos().distanceSq(apo.getPos())) {
					return apo;
				}
			}
		}
		return null; // all known locations were too close to get a good estimate
	}

	@Nullable
	public AccountPosObservation getMostRecentPosObservationForAccounts(@Nonnull Collection<String> accounts) {
		AccountPosObservation result = null;
		for (String account : accounts) {
			final AccountPosObservation last = getMostRecentPosObservationForAccount(account);
			if (last == null) continue;
			if (result == null || result.getTime() < last.getTime()) {
				result = last;
			}
		}
		return result;
	}

	@Nullable
	public AccountPosObservation getMostRecentPosObservationForAccount(@Nonnull String account) {
		final List<AccountObservation> recentAccObs = observationsByAccount.get(account);
		if (recentAccObs != null) {
			// start with latest observation, then go back in time
			for (int i = recentAccObs.size() - 1; i >= 0; i--) {
				final Observation observation = recentAccObs.get(i);
				if (observation instanceof AccountPosObservation) {
					return (AccountPosObservation) observation;
				}
			}
		}
		return null;
	}

	@Nullable
	public Long getLastLoginout(@Nonnull String account, @Nullable Action action) {
		final List<AccountObservation> observations = observationsByAccount.get(account.toLowerCase());
		if (observations == null) return null;
		// start with latest observation, then go back in time
		for (int i = observations.size() - 1; i >= 0; i--) {
			final Observation observation = observations.get(i);
			if (observation instanceof Skynet) {
				final Skynet loginout = (Skynet) observation;
				if (action != null && loginout.action != action) continue;
				return loginout.time;
			}
		}
		return null; // no matching loginout stored
	}
}
