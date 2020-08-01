package gjum.minecraft.civ.synapse.server;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * file format: ACCOUNT\tUUID\n... or ACCOUNT\n...
 */
public class UuidsConfig extends LoggingLinesConfig {
	private Map<UUID, String> accountByUuid = new HashMap<>();
	/**
	 * lowercase account as key
	 */
	private Map<String, UUID> uuidByAccount = new HashMap<>();

	@Override
	protected Collection<String> getLines() {
		return accountByUuid.entrySet().stream()
				.map(e -> e.getValue() + "\t" + e.getKey())
				.sorted()
				.collect(Collectors.toList());
	}

	@Override
	protected void setLines(Stream<String> newAccounts) {
		accountByUuid.clear();
		uuidByAccount.clear();

		accountByUuid = newAccounts.distinct()
				.map(l -> l.split("\t"))
				.collect(Collectors.toMap(
						t -> UUID.fromString(t[1]),
						t -> t[0],
						(a1, a2) -> a1));
		for (Map.Entry<UUID, String> entry : accountByUuid.entrySet()) {
			uuidByAccount.put(entry.getValue().toLowerCase(), entry.getKey());
		}

		logger.info(getClass().getSimpleName() +
				" Loaded " + accountByUuid.size() + " account uuids");
	}

	@Nullable
	public UUID getUuidForAccount(@Nullable String account) {
		load(null); // always reload before query; file may have been changed manually
		if (account == null) return null;
		return uuidByAccount.get(account.toLowerCase());
	}

	@Nullable
	public String getAccountForUuid(@Nullable UUID uuid) {
		return accountByUuid.get(uuid);
	}
}
