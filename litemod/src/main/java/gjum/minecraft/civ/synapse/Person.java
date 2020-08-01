package gjum.minecraft.civ.synapse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;

import static gjum.minecraft.civ.synapse.common.Util.containsIgnoreCase;
import static gjum.minecraft.civ.synapse.common.Util.sortedUniqListIgnoreCase;

public class Person implements Cloneable {
	@Nonnull
	private final PersonsRegistry registry;
	@Nonnull
	private String name;
	@Nonnull
	private Collection<String> factions;
	@Nonnull
	private Collection<String> accounts;
	@Nullable
	private String notes;

	public Person(
			@Nonnull PersonsRegistry registry,
			@Nonnull String name,
			@Nonnull Collection<String> factions,
			@Nonnull Collection<String> accounts,
			@Nullable String notes
	) {
		this.registry = registry;
		this.name = name;
		this.factions = sortedUniqListIgnoreCase(factions);
		this.accounts = sortedUniqListIgnoreCase(accounts);
		this.notes = notes;
	}

	private Person cloneMe() {
		try {
			return (Person) clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Nonnull
	public String getName() {
		return name;
	}

	public boolean setName(String name) {
		final Person oldPerson = cloneMe();
		this.name = name;
		boolean accepted = registry.propagatePersonChange(oldPerson, this);
		if (!accepted) this.name = oldPerson.name;
		return accepted;
	}

	public boolean isMain(String account) {
		if (name.toLowerCase().equals(account.toLowerCase())) return true;
		final String personClean = name.toLowerCase().replaceAll("_+", "");
		final String accountClean = account.toLowerCase().replaceAll("_+", "");
		return accountClean.startsWith(personClean) || accountClean.endsWith(personClean)
				|| personClean.startsWith(accountClean) || personClean.endsWith(accountClean);
	}

	@Nonnull
	public Collection<String> getAccounts() {
		return accounts;
	}

	@Nullable
	public String hasAccount(@Nullable String account) {
		return containsIgnoreCase(account, accounts);
	}

	public void addAccount(String account) {
		final Person oldPerson = cloneMe();
		oldPerson.accounts = new ArrayList<>(accounts);
		accounts.add(account);
		accounts = sortedUniqListIgnoreCase(accounts);
		registry.propagatePersonChange(oldPerson, this);
	}

	public void removeAccount(String account) {
		account = hasAccount(account);
		if (account == null) return;
		final Person oldPerson = cloneMe();
		oldPerson.accounts = new ArrayList<>(accounts);
		accounts.remove(account);
		registry.propagatePersonChange(oldPerson, this);
	}

	public void setAccounts(Collection<String> newAccounts) {
		final Person oldPerson = cloneMe();
		oldPerson.accounts = new ArrayList<>(this.accounts);
		this.accounts = sortedUniqListIgnoreCase(newAccounts);
		registry.propagatePersonChange(oldPerson, this);
	}

	@Nonnull
	public Collection<String> getFactions() {
		return new ArrayList<>(factions);
	}

	@Nullable
	public String hasFaction(@Nullable String faction) {
		return containsIgnoreCase(faction, factions);
	}

	public void addFaction(String faction) {
		final Person oldPerson = cloneMe();
		oldPerson.factions = new ArrayList<>(factions);
		factions.add(faction);
		factions = sortedUniqListIgnoreCase(factions);
		registry.propagatePersonChange(oldPerson, this);
	}

	public void removeFaction(String faction) {
		faction = hasFaction(faction);
		if (faction == null) return;
		final Person oldPerson = cloneMe();
		oldPerson.factions = new ArrayList<>(factions);
		factions.remove(faction);
		registry.propagatePersonChange(oldPerson, this);
	}

	public void setFactions(Collection<String> newFactions) {
		final Person oldPerson = cloneMe();
		oldPerson.factions = new ArrayList<>(this.factions);
		this.factions = sortedUniqListIgnoreCase(newFactions);
		registry.propagatePersonChange(oldPerson, this);
	}

	@Nonnull
	public String getNotes() {
		if (notes == null) return "";
		return notes;
	}

	public void setNotes(@Nullable String notes) {
		final Person oldPerson = cloneMe();
		this.notes = notes;
		registry.propagatePersonChange(oldPerson, this);
	}
}
