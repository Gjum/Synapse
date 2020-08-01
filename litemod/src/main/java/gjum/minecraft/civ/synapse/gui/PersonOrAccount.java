package gjum.minecraft.civ.synapse.gui;

import gjum.minecraft.civ.synapse.*;
import gjum.minecraft.civ.synapse.config.ServerConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

class PersonOrAccount {
	// each Nonnull is enforced with check in constructor
	@Nonnull
	private final PersonsRegistry personsRegistry = LiteModSynapse.instance.getPersonsRegistry();
	@Nonnull
	private final ServerConfig serverConfig = LiteModSynapse.instance.serverConfig;

	// if person is null, account is non-null
	@Nullable
	String account;
	// if account is null, person is non-null
	@Nullable
	Person person;

	public PersonOrAccount(@Nonnull Person person) {
		if (personsRegistry == null) throw new IllegalStateException("not on server at the moment");
		this.person = person;
	}

	public PersonOrAccount(@Nonnull String account) {
		if (personsRegistry == null) throw new IllegalStateException("not on server at the moment");
		this.account = account;
		this.person = personsRegistry.personByAccountName(account);
	}

	// TODO We use .equals to facilitate observation deduplication. As a result, this class FAILS this assumption: "If two objects are equal according to the equals(Object) method, then calling the hashCode method on each of the two objects must produce the same integer result."
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PersonOrAccount)) return false;
		final PersonOrAccount other = (PersonOrAccount) obj;
		return person == other.person || Objects.equals(account.toLowerCase(), other.account.toLowerCase())
				|| (person != null && person.hasAccount(other.account) != null)
				|| (other.person != null && other.person.hasAccount(account) != null);
	}

	@Override
	public int hashCode() {
		if (person != null) return person.hashCode() << 1;
		return (account.hashCode() << 1) + 1;
	}

	@Nonnull
	public Person personOrCreate() {
		if (person == null) {
			person = personsRegistry.personByAccountNameOrCreate(account);
		}
		return person;
	}

	@Nonnull
	public Standing getStanding() {
		if (person == null) return serverConfig.getAccountStanding(account);
		return serverConfig.getStanding(person);
	}

	@Nonnull
	public String getName() {
		if (person == null) return account;
		return person.getName();
	}

	@Nonnull
	public Collection<String> getAccounts() {
		if (person == null) return Collections.singletonList(account);
		return person.getAccounts();
	}

	@Nonnull
	public Collection<String> getFactions() {
		if (person == null) return Collections.emptyList();
		return person.getFactions();
	}
}
