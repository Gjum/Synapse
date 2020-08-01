package gjum.minecraft.civ.synapse.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * file format: ACCOUNT\tUUID\n... or ACCOUNT\n...
 */
public class AccountsListConfig extends LoggingLinesConfig {
	private final UuidsConfig uuidMapper;

	private Set<UUID> uuids = new HashSet<>();

	public AccountsListConfig(UuidsConfig uuidMapper) {
		this.uuidMapper = uuidMapper;
	}

	@Override
	protected Collection<String> getLines() {
		return uuids.stream()
				.map(uuid -> uuidMapper.getAccountForUuid(uuid)
						+ "\t" + uuid)
				.sorted()
				.collect(Collectors.toList());
	}

	@Override
	protected void setLines(Stream<String> newLines) {
		uuids = newLines
				.map(l -> {
					final String[] split = l.split("\t");
					if (split.length < 2 || split[1].trim().isEmpty()) {
						return uuidMapper.getUuidForAccount(split[0].trim());
					} else return UUID.fromString(split[1].trim());
				})
				.collect(Collectors.toSet());
		logger.info(getClass().getSimpleName() +
				" Loaded list of " + uuids.size() + " accounts by uuid");
	}

	public void setList(@Nonnull Collection<String> accounts) {
		uuids = accounts.stream()
				.map(uuidMapper::getUuidForAccount)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		saveLater(null);
	}

	public boolean contains(@Nullable UUID uuid) {
		return uuid != null && uuids.contains(uuid);
	}
}
