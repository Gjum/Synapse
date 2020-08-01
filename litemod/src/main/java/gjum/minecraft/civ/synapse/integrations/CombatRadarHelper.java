package gjum.minecraft.civ.synapse.integrations;

import com.aleksey.combatradar.LiteModCombatRadar;
import com.aleksey.combatradar.config.PlayerType;
import com.aleksey.combatradar.config.RadarConfig;
import com.mumfrey.liteloader.core.LiteLoader;
import gjum.minecraft.civ.synapse.*;
import gjum.minecraft.civ.synapse.config.ServerConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Collection;

public class CombatRadarHelper implements PersonChangeHandler {
	@Override
	public void handlePersonChange(@Nullable Person oldPerson, @Nullable Person newPerson) {
		final ServerConfig serverConfig = LiteModSynapse.instance.serverConfig;
		final RadarConfig radarConfig = getRadarConfig();
		if (radarConfig == null || serverConfig == null) return;

		final Standing standingNew = serverConfig.getStanding(newPerson);
		final PlayerType newType = getPlayerTypeFromStanding(standingNew);

//		// mark removed accounts as NEUTRAL
//		if (oldPerson != null) {
//			for (String account : oldPerson.getAccounts()) {
//				if (newPerson == null || newPerson.hasAccount(account) == null) {
//					// account was removed and is now not associated anymore
//					radarConfig.setPlayerType(account, PlayerType.Neutral);
//				}
//			}
//		}

		if (newPerson != null) {
			for (String account : newPerson.getAccounts()) {
				radarConfig.setPlayerType(account, newType);
			}
		}
	}

	@Override
	public void handleLargeChange(@Nonnull Collection<Person> persons) {
		final ServerConfig serverConfig = LiteModSynapse.instance.serverConfig;
		final RadarConfig radarConfig = getRadarConfig();
		if (radarConfig == null || serverConfig == null) return;
		for (Person person : persons) {
			final Standing standing = serverConfig.getStanding(person);
			final PlayerType playerType = getPlayerTypeFromStanding(standing);
			// leave alone the existing accounts that don't exist in PersonsRegistry
			for (String account : person.getAccounts()) {
				radarConfig.setPlayerType(account, playerType);
			}
		}
	}

	@Nonnull
	private static PlayerType getPlayerTypeFromStanding(Standing standing) {
		switch (standing) {
			case FRIENDLY:
				return PlayerType.Ally;
			case HOSTILE:
				return PlayerType.Enemy;
			default:
			case NEUTRAL:
				return PlayerType.Neutral;
		}
	}

	@Nullable
	public static RadarConfig getRadarConfig() {
		try {
			if (!isCombatRadarActive()) return null;

			final LiteModCombatRadar combatRadar = LiteLoader.getInstance().getMod(LiteModCombatRadar.class);
			if (combatRadar == null) return null;
			final Field field = LiteModCombatRadar.class.getDeclaredField("_config");
			field.setAccessible(true);
			return (RadarConfig) field.get(combatRadar);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static boolean isCombatRadarActive() {
		try {
			Class.forName("com.aleksey.combatradar.LiteModCombatRadar");
			return LiteLoader.getInstance().isModActive("Combat Radar");
		} catch (NoClassDefFoundError | ClassNotFoundException ignored) {
		}
		return false;
	}
}
