package gjum.minecraft.civ.synapse;

import net.minecraft.util.Tuple;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gjum.minecraft.civ.synapse.common.Util.identityFloatFunction;
import static gjum.minecraft.civ.synapse.common.Util.scoreSimilarity;

public class PersonsRegistry {
	/**
	 * indexed by lower case person name
	 */
	private HashMap<String, Person> personsByName = new HashMap<>();
	/**
	 * indexed by lower case account name
	 */
	private HashMap<String, Person> personsByAccount = new HashMap<>();

	private Collection<PersonChangeHandler> changeHandlers = new HashSet<>();

	public void remove(Person person) {
		person.getAccounts().stream()
				.map(String::toLowerCase)
				.forEach(personsByAccount::remove);
		personsByName.remove(person.getName().toLowerCase());
		propagatePersonChange(person, null);
	}

	@Nonnull
	public Collection<Person> getPersons() {
		return personsByName.values();
	}

	@Nullable
	public Person personByName(@Nullable String name) {
		if (name == null) return null;
		return personsByName.get(name.toLowerCase());
	}

	public Person personByNameOrCreate(String name) {
		Person person = personByName(name);
		if (person == null) {
			person = new Person(this, name, new HashSet<>(), new HashSet<>(), null);
			propagatePersonChange(null, person);
		}
		return person;
	}

	@Nullable
	public Person personByAccountName(@Nullable String accountName) {
		if (accountName == null) return null;
		return personsByAccount.get(accountName.toLowerCase());
	}

	@Nonnull
	public Person personByAccountNameOrCreate(@Nonnull String account) {
		Person person = personByAccountName(account);
		if (person == null) {
			final HashSet<String> accounts = new HashSet<>(Collections.singleton(account));
			String name = account;
			while (personByName(name) != null) {
				// there is already a person with that name, but it does not have the account
				name = "~" + name;
			}
			person = new Person(this, name, new HashSet<>(), accounts, null);
			propagatePersonChange(null, person);
		}
		return person;
	}

	@Nullable
	public Person personByNameOrAccount(@Nonnull String nameOrAccount) {
		final Person person = personByName(nameOrAccount);
		if (person != null) return person;
		return personByAccountName(nameOrAccount);
	}

	@Nonnull
	public Collection<Person> personsInFaction(@Nonnull String faction) {
		return getPersons().stream()
				.filter(p -> p.hasFaction(faction) != null)
				.collect(Collectors.toList());
	}

	public List<Tuple<Person, Float>> findSimilarScored(String query) {
		return findSimilarScoredStream(query).limit(10).collect(Collectors.toList());
	}

	public Stream<Tuple<Person, Float>> findSimilarScoredStream(String query) {
		final String queryLower = query.toLowerCase();
		return personsByName.values().stream()
				.map(p -> {
					final float nameScore = scoreSimilarity(queryLower, p.getName().toLowerCase());
					final float accountsScore = p.getAccounts().stream()
							.map(account -> scoreSimilarity(queryLower, account.toLowerCase()))
							.max(Comparator.comparing(identityFloatFunction)).orElse(0f);
					final float factionsScore = p.getFactions().stream()
							.map(faction -> scoreSimilarity(queryLower, faction.toLowerCase()))
							.max(Comparator.comparing(identityFloatFunction)).orElse(0f);
					return new Tuple<>(p, Math.max(nameScore, Math.max(accountsScore, factionsScore)));
				})
				.sorted(Comparator.comparing(Tuple<Person, Float>::getSecond).reversed());
	}

	public void registerChangeHandler(@Nonnull PersonChangeHandler changeHandler) {
		changeHandlers.add(changeHandler);
	}

	/**
	 * @return true if the change was accepted, false if e.g. the new name already exists
	 */
	public boolean propagatePersonChange(@Nullable Person oldPerson, @Nullable Person newPerson) {
		if (newPerson != null) {
			final Person conflictingNamePerson = personByName(newPerson.getName());
			if (conflictingNamePerson != null
					&& conflictingNamePerson != oldPerson
					&& conflictingNamePerson != newPerson
			) {
				return false;
			}

			final Set<Person> prevAssociatedPersons = new HashSet<>();
			for (String account : newPerson.getAccounts()) {
				final Person person = personByAccountName(account);
				if (person != null && person != oldPerson && person != newPerson) {
					prevAssociatedPersons.add(person);
					person.removeAccount(account);
				}
			}

			propagateLargeChange(prevAssociatedPersons);
		}

		if (oldPerson != null) {
			personsByName.remove(oldPerson.getName().toLowerCase());
			oldPerson.getAccounts().stream()
					.map(String::toLowerCase)
					.forEach(personsByAccount::remove);
		}

		if (newPerson != null) {
			personsByName.put(newPerson.getName().toLowerCase(), newPerson);
			for (String account : newPerson.getAccounts()) {
				personsByAccount.put(account.toLowerCase(), newPerson);
			}
		}

		for (PersonChangeHandler changeHandler : changeHandlers) {
			try {
				changeHandler.handlePersonChange(oldPerson, newPerson);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	public void propagateLargeChange(Collection<Person> persons) {
		for (PersonChangeHandler changeHandler : changeHandlers) {
			try {
				changeHandler.handleLargeChange(persons);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	public void loadPersons(Collection<Person> persons) {
		personsByName.clear();
		personsByAccount.clear();
		for (Person person : persons) {
			personsByName.put(person.getName().toLowerCase(), person);
			for (String accountName : person.getAccounts()) {
				personsByAccount.put(accountName.toLowerCase(), person);
			}
		}
		propagateLargeChange(personsByName.values());
	}
}
