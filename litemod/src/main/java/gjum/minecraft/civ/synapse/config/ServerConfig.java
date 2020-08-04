package gjum.minecraft.civ.synapse.config;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.*;
import net.minecraft.util.Tuple;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static gjum.minecraft.civ.synapse.common.Util.mapNonNull;
import static gjum.minecraft.civ.synapse.common.Util.nonNullOr;

public class ServerConfig extends JsonConfig {
	/**
	 * Whether to enable the mod on this server.
	 */
	@Expose
	private boolean enabled = true;

	private static final String defaultCommsAddress = "invalid";
	@Expose
	private String commsAddress = defaultCommsAddress;

	/**
	 * Decides which factions are important and which ones are secondary/ignored etc.
	 * Relevant for rendering (colors) and alerting.
	 */
	@Expose
	private HashMap<String, Standing> factionStandings = new HashMap<>();

	@Expose
	@Nonnull
	private String defaultFriendlyFaction = "(friendly)";
	@Expose
	@Nonnull
	private String defaultHostileFaction = "(hostile)";
	@Expose
	@Nonnull
	private String defaultNeutralFaction = "(neutral)";

	private Collection<PersonChangeHandler> changeHandlers = new HashSet<>();

	public ServerConfig() {
		factionStandings.put(defaultFriendlyFaction.toLowerCase(), Standing.FRIENDLY);
		factionStandings.put(defaultHostileFaction.toLowerCase(), Standing.HOSTILE);
		factionStandings.put(defaultNeutralFaction.toLowerCase(), Standing.NEUTRAL);
	}

	@Override
	protected Object getData() {
		return this;
	}

	@Override
	protected void setData(Object data) {
		final ServerConfig other = ((ServerConfig) data);

		enabled = other.enabled;
		final HashMap<String, Standing> oldFactionStandings = factionStandings;
		factionStandings = other.factionStandings;

		factionStandings.put(defaultFriendlyFaction.toLowerCase(), Standing.FRIENDLY);
		factionStandings.put(defaultHostileFaction.toLowerCase(), Standing.HOSTILE);
		factionStandings.put(defaultNeutralFaction.toLowerCase(), Standing.NEUTRAL);

		final PersonsRegistry personsRegistry = LiteModSynapse.instance.getPersonsRegistry();
		if (personsRegistry != null) {
			final List<Person> personsWithAffectedFactions = personsRegistry.getPersons().stream().filter(person -> {
				for (String faction : person.getFactions()) {
					if (factionStandings.get(faction.toLowerCase()) != null) return true;
					if (oldFactionStandings.get(faction.toLowerCase()) != null) return true;
				}
				return false;
			}).collect(Collectors.toList());
			propagateLargeChange(personsWithAffectedFactions);
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) return;
		this.enabled = enabled;
		saveLater(null);
		LiteModSynapse.instance.checkModActive();
	}

	public String getCommsAddress() {
		return !commsAddress.isEmpty() ? commsAddress : defaultCommsAddress;
	}

	public void setCommsAddress(String address) {
		commsAddress = address;
		LiteModSynapse.instance.checkCommsAddress();
		saveLater(null);
	}

	@Nonnull
	public String getDefaultFriendlyFaction() {
		return defaultFriendlyFaction;
	}

	@Nonnull
	public String getDefaultHostileFaction() {
		return defaultHostileFaction;
	}

	@Nonnull
	public String getDefaultNeutralFaction() {
		return defaultNeutralFaction;
	}

	public void setFactionStanding(@Nonnull String factionName, @Nullable Standing standing) {
		final String factionLower = factionName.toLowerCase();

		if (standing == null || standing == Standing.UNSET) {
			factionStandings.remove(factionLower);
		} else {
			factionStandings.put(factionLower, standing);
		}
		saveLater(null);

		final PersonsRegistry personsRegistry = LiteModSynapse.instance.getPersonsRegistry();
		if (personsRegistry != null) {
			propagateLargeChange(personsRegistry.personsInFaction(factionLower));
		}
	}

	/**
	 * Returns the faction with the most confident {@link Standing}.
	 */
	@Nullable
	public String getMostRelevantFaction(@Nonnull Collection<String> factions) {
		Standing mostConfidentStanding = null;
		String mostConfidentFaction = null;
		for (String faction : factions) {
			final Standing standing = getFactionStanding(faction);
			if (standing.moreConfidentThan(mostConfidentStanding)) {
				mostConfidentStanding = standing;
				mostConfidentFaction = faction;
			}
		}
		return mostConfidentFaction;
	}

	@Nonnull
	public Standing getFactionStanding(@Nonnull String faction) {
		return nonNullOr(factionStandings.get(faction.toLowerCase()), Standing.UNSET);
	}

	@Nonnull
	public Standing getStanding(@Nullable Person person) {
		if (person == null) return Standing.UNSET;
		final boolean isPersonFocused = person.getAccounts().stream()
				.anyMatch(LiteModSynapse.instance::isFocusedAccount);
		if (isPersonFocused) return Standing.FOCUS;
		final String faction = getMostRelevantFaction(person.getFactions());
		final Standing standing = mapNonNull(faction, this::getFactionStanding);
		return nonNullOr(standing, Standing.UNSET);
	}

	@Nonnull
	public Standing getAccountStanding(@Nonnull String account) {
		if (LiteModSynapse.instance.isFocusedAccount(account)) return Standing.FOCUS;
		final PersonsRegistry personsRegistry = LiteModSynapse.instance.getPersonsRegistry();
		if (personsRegistry == null) return Standing.UNSET;
		return nonNullOr(mapNonNull(personsRegistry.personByAccountName(account),
				this::getStanding), Standing.UNSET);
	}

	/**
	 * Adds/removes factions such that it gets the desired {@link Standing}.
	 * If the standing is already as desired, no factions are changed. Otherwise:
	 * If FRIENDLY, any HOSTILE factions are removed. Then, if necessary, the default FriendlyFaction is added.
	 * If HOSTILE, any FRIENDLY factions are removed. Then, if necessary, the default HostileFaction is added.
	 * If NEUTRAL, any FRIENDLY and HOSTILE factions are removed.
	 * If UNSET, all factions with a configured Standing are removed.
	 */
	public void setPersonStanding(@Nonnull Person person, @Nonnull Standing standing) {
		if (standing == Standing.FOCUS) {
			LiteModSynapse.instance.announceFocusedAccounts(person.getAccounts());
			return;
		}
		if (standing == getStanding(person)) return;

		final Tuple<Collection<String>, String> changes = simulateSetPersonStanding(person, standing);
		final Collection<String> removedFactions = changes.getFirst();
		final String addedFaction = changes.getSecond();

		for (String faction : removedFactions) {
			person.removeFaction(faction);
		}

		//noinspection ConstantConditions - we reuse Tuple class but actually allow null here
		if (addedFaction != null) person.addFaction(addedFaction);
	}

	public Tuple<Collection<String>, String> simulateSetPersonStanding(@Nonnull Person person, @Nonnull Standing standing) {
		Collection<String> removedFactions = new ArrayList<>();
		String addedFaction = null;
		if (standing == getStanding(person)) {
			//noinspection ConstantConditions - we reuse Tuple class but actually allow null here
			return new Tuple<>(removedFactions, addedFaction);
		}
		// TODO since we use the lowercase keys here, faction name upper/lowercase is lost
		switch (standing) {
			case FOCUS:
				// no factions added/removed
				break;
			case FRIENDLY:
				for (Map.Entry<String, Standing> e : factionStandings.entrySet()) {
					if (e.getValue() == Standing.HOSTILE) removedFactions.add(e.getKey());
				}
				if (standing != getStanding(person)) addedFaction = getDefaultFriendlyFaction();
				break;
			case HOSTILE:
				for (Map.Entry<String, Standing> e : factionStandings.entrySet()) {
					if (e.getValue() == Standing.FRIENDLY) removedFactions.add(e.getKey());
				}
				if (standing != getStanding(person)) addedFaction = getDefaultHostileFaction();
				break;
			case NEUTRAL:
				for (Map.Entry<String, Standing> e : factionStandings.entrySet()) {
					if (e.getValue() == Standing.FRIENDLY || e.getValue() == Standing.HOSTILE) {
						removedFactions.add(e.getKey());
					}
				}
				if (standing != getStanding(person)) addedFaction = getDefaultNeutralFaction();
				break;
			case UNSET:
				removedFactions.addAll(factionStandings.keySet());
				break;
		}
		//noinspection ConstantConditions - we reuse Tuple class but actually allow null here
		return new Tuple<>(removedFactions, addedFaction);
	}

	public void registerChangeHandler(@Nonnull PersonChangeHandler changeHandler) {
		changeHandlers.add(changeHandler);
	}

	private void propagateLargeChange(Collection<Person> persons) {
		for (PersonChangeHandler changeHandler : changeHandlers) {
			changeHandler.handleLargeChange(persons);
		}
	}
}
