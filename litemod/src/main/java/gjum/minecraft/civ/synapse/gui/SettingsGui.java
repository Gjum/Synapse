package gjum.minecraft.civ.synapse.gui;

import gjum.minecraft.civ.synapse.LiteModSynapse;
import gjum.minecraft.civ.synapse.Standing;
import gjum.minecraft.civ.synapse.config.*;
import gjum.minecraft.gui.*;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.function.*;

import static gjum.minecraft.civ.synapse.gui.MainGui.*;
import static gjum.minecraft.gui.Label.Alignment.ALIGN_LEFT;
import static gjum.minecraft.gui.Vec2.Direction.COLUMN;
import static gjum.minecraft.gui.Vec2.Direction.ROW;
import static java.lang.Float.parseFloat;

public class SettingsGui extends GuiRoot {
	private static final String[] toggleOptions = new String[]{"§aON", "§1off"};

	private static boolean toggleToBool(String value) {
		String lastWord = value.substring(1 + value.lastIndexOf(' '));
		return toggleOptions[0].equals(lastWord);
	}

	private static String boolToToggle(boolean value) {
		return value ? toggleOptions[0] : toggleOptions[1];
	}

	private static String boolToToggle(String text, boolean value) {
		return text + ": " + (value ? toggleOptions[0] : toggleOptions[1]);
	}

	private final LiteModSynapse mod = LiteModSynapse.instance;

	public SettingsGui(GuiScreen parentScreen) {
		super(parentScreen);
	}

	@Override
	public GuiElement build() {
		final FlexListLayout titleRow = makeTitleRow(this, LiteModSynapse.MOD_NAME + ": Settings");

		final FlexListLayout scroll = new FlexListLayout(COLUMN);

		scroll.add(new Label("Global settings:", ALIGN_LEFT).setColor(Color.YELLOW));
		scroll.add(new Spacer(spacer));

		scroll.add(buildHudConfig());
		scroll.add(new Spacer(spacer));
		scroll.add(buildPlayerDecorationsConfig());
		scroll.add(new Spacer(spacer));
		scroll.add(buildWaypointsConfig());
		scroll.add(new Spacer(spacer));
		scroll.add(buildNameReplacementConfig());
		scroll.add(new Spacer(spacer));
		scroll.add(buildRadarSoundConfig());
		scroll.add(new Spacer(spacer));

		scroll.add(configFormatRow("Radar messages", "Show a chat message when a player enters or leaves the render distance",
				mod.config::getRadarVisibilityFormat,
				mod.config::setRadarVisibilityFormat,
				mod.config::getRadarStandingFilter,
				mod.config::setRadarStandingFilter,
				mod.config::getRadarFormat,
				mod.config::setRadarFormat
		));
		scroll.add(new Spacer(spacer));
		scroll.add(configFormatRow("Skynet messages", "Show a chat message when a player joins or leaves the server",
				mod.config::getSkynetVisibilityFormat,
				mod.config::setSkynetVisibilityFormat,
				mod.config::getSkynetStandingFilter,
				mod.config::setSkynetStandingFilter,
				mod.config::getSkynetFormat,
				mod.config::setSkynetFormat
		));
		scroll.add(new Spacer(spacer));

		scroll.add(configFormatRow("Snitch replacement", "Replace the default snitch messages",
				mod.config::getSnitchHitVisibilityFormat,
				mod.config::setSnitchHitVisibilityFormat,
				mod.config::getSnitchHitStandingFilter,
				mod.config::setSnitchHitStandingFilter,
				mod.config::getSnitchHitFormat,
				mod.config::setSnitchHitFormat
		));
		scroll.add(new Spacer(spacer));
		scroll.add(configFormatRow("Pearl Location replacement", "Replace the default pearl broadcast messages",
				mod.config::getPearlLocationVisibilityFormat,
				mod.config::setPearlLocationVisibilityFormat,
				null, null,
				mod.config::getPearlLocationFormat,
				mod.config::setPearlLocationFormat));
		scroll.add(new Spacer(spacer));
		scroll.add(configFormatRow("Combat Tag replacement", "Replace the default combat tag messages",
				mod.config::getCombatTagVisibilityFormat,
				mod.config::setCombatTagVisibilityFormat,
				null, null,
				mod.config::getCombatTagFormat,
				mod.config::setCombatTagFormat));
		scroll.add(new Spacer(spacer));
		scroll.add(configFormatRow("Group Chat replacement", "Replace the default group chat messages",
				mod.config::getGroupChatVisibilityFormat,
				mod.config::setGroupChatVisibilityFormat,
				null, null,
				mod.config::getGroupChatFormat,
				mod.config::setGroupChatFormat));
		scroll.add(new Spacer(spacer));

		scroll.add(configFormatRow("Focus messages", "Incoming messages from commanders declaring the focused player",
				mod.config::getFocusAnnouncementVisibilityFormat,
				mod.config::setFocusAnnouncementVisibilityFormat,
				null, null,
				mod.config::getFocusAnnouncementFormat,
				mod.config::setFocusAnnouncementFormat));
		scroll.add(new Spacer(spacer));

		scroll.add(buildStandingColorsConfig());
		scroll.add(new Spacer(spacer));

		final ServerConfig serverConfig = mod.serverConfig;
		final PersonsConfig personsConfig = mod.personsConfig;
		if (serverConfig != null) {
			scroll.add(new Tooltip("Address of your team's communications server", new FlexListLayout(ROW)
					.add(new Label("Comms address:", ALIGN_LEFT))
					.add(new TextField(text -> {
						text = text.trim();
						final boolean valid = text.matches("([^:/]+:[0-9]+)?");
						if (valid) serverConfig.setCommsAddress(text);
						return valid;
					}, serverConfig.getCommsAddress()))));
			scroll.add(new Spacer(spacer));

			scroll.add(new Tooltip("Proxy for connecting to communications server.", new FlexListLayout(ROW)
					.add(new Label("Proxy (SOCKS5):", ALIGN_LEFT))
					.add(new TextField(text -> {
						text = text.trim();
						final boolean valid = text.matches("([^:/]+:[0-9]+)?");
						if (valid) serverConfig.setProxyAddress(text);
						return valid;
					}, serverConfig.getProxyAddress()))));
			scroll.add(new Spacer(spacer));
		}

		scroll.add(new Button(() -> {
			mod.config.load(null);
			mod.accountsConfig.load(null);
			if (serverConfig != null) serverConfig.load(null);
			if (personsConfig != null) personsConfig.load(null);
			rebuild();
		}, "Reload config files"));

		final FlexListLayout content = new FlexListLayout(COLUMN);
		content.add(new Spacer(spacer));
		content.add(titleRow);
		content.add(new Spacer(spacer));
		content.add(new ScrollBox(scroll)
				.setWeight(new Vec2(1, 1)));
		content.add(new Spacer(spacer));

		return new FlexListLayout(ROW)
				.add(new Spacer(spacer))
				.add(content)
				.add(new Spacer(spacer));
	}

	@Nonnull
	private GuiElement buildStandingColorsConfig() {
		final FlexListLayout standingColorsRow = new FlexListLayout(ROW);
		standingColorsRow.add(new Label("Standing colors: ", ALIGN_LEFT));
		for (Standing standing : Standing.values()) {
			standingColorsRow.add(new Spacer(spacer));
			standingColorsRow.add(new Label(standing.name().toLowerCase() + ": ", ALIGN_LEFT));
			standingColorsRow.add(stretchingTextField(text -> {
				final TextFormatting color = TextFormatting.getValueByName(text);
				if (color != null) mod.config.setStandingColor(standing, color);
				return color != null;
			}, mod.config.getStandingColor(standing).getFriendlyName()));
		}
		return standingColorsRow;
	}

	@Nonnull
	private GuiElement buildRadarSoundConfig() {
		final FlexListLayout standingSoundsRow = new FlexListLayout(ROW);
		standingSoundsRow.add(configToggle("Radar sound",
				mod.config::setPlayRadarSound,
				mod.config::isPlayRadarSound));
		for (Standing standing : Standing.values()) {
			standingSoundsRow.add(new Spacer(spacer));
			standingSoundsRow.add(new Label(standing.name().toLowerCase() + ": ", ALIGN_LEFT));
			standingSoundsRow.add(stretchingTextField(text -> {
				text = text.trim();
				final boolean valid = text.isEmpty() || SoundEvent.REGISTRY.containsKey(new ResourceLocation(text));
				if (valid) mod.config.setStandingSound(standing, text);
				return valid;
			}, mod.config.getStandingSound(standing)));
		}
		return new Tooltip("Play a sound when a player becomes visible, according to their standing.\nEmpty = no sound for that standing",
				standingSoundsRow);
	}

	private static final DecimalFormat wpAgeFloatFmt = new DecimalFormat("0.#");

	@Nonnull
	private GuiElement buildWaypointsConfig() {
		return new Tooltip("Track players by radar, snitch alerts, pearl broadcasts, and other chat messages", new FlexListLayout(ROW)
				.add(new Label("Waypoints:", ALIGN_LEFT))
				.add(new Spacer(spacer))
				.add(configToggle("JourneyMap",
						mod.config::setUseJourneyMap,
						mod.config::isUseJourneyMap))
				.add(new Spacer(spacer))
				.add(configToggle("VoxelMap",
						mod.config::setUseVoxelMap,
						mod.config::isUseVoxelMap))
				.add(new Spacer(spacer))
				.add(new Label("Hide after: ", ALIGN_LEFT))
				.add(new TextField(text -> {
					try {
						mod.config.setMaxWaypointAge((long) (60000f * parseFloat(text)));
						return true;
					} catch (NumberFormatException e) {
						return false;
					}
				}, wpAgeFloatFmt.format(mod.config.getMaxWaypointAge() / 60000f))
						.setFixedSize(new Vec2(60, 20)))
				.add(new Label("min", ALIGN_LEFT))
		);
	}

	@Nonnull
	private GuiElement buildPlayerDecorationsConfig() {
		final FlexListLayout row = new FlexListLayout(ROW);
		row.add(new Label("Player decorations:", ALIGN_LEFT));
		row.add(new Spacer(spacer));
//		row.add(configToggle("Glow",
//				mod.config::setPlayerGlow,
//				mod.config::isPlayerGlow));
//		row.add(new Spacer(spacer));
		row.add(configToggle("Middle hoop",
				mod.config::setPlayerMiddleHoop,
				mod.config::isPlayerMiddleHoop));
		row.add(new Spacer(spacer));
		row.add(configToggle("Top/bottom hoops",
				mod.config::setPlayerOuterHoops,
				mod.config::isPlayerOuterHoops));
		row.add(new Spacer(spacer));
		row.add(configToggle("Hitbox",
				mod.config::setPlayerBox,
				mod.config::isPlayerBox));
		row.add(new Spacer(spacer));
		row.add(new Label("Line width: ", ALIGN_LEFT));
		row.add(new TextField(text -> {
			try {
				mod.config.setPlayerLineWidth(parseFloat(text));
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		}, String.valueOf(mod.config.getPlayerLineWidth()))
				.setFixedSize(new Vec2(60, 20)));
		return new Tooltip("Rendered around each player and colored by standing",
				row);
	}

	private GuiElement buildNameReplacementConfig() {
		return new FlexListLayout(ROW)
				.add(new Label("Colored names:", ALIGN_LEFT))
				.add(new Spacer(spacer))
				.add(new Tooltip("Above player, show \"Account (Nickname)\" colored by standing",
						configToggle("Name plates",
								mod.config::setReplaceNamePlates,
								mod.config::isReplaceNamePlates)))
				.add(new Tooltip("In online player list, show \"Account (Nickname)\" colored by standing",
						configToggle("Tablist",
								mod.config::setReplaceTablistColors,
								mod.config::isReplaceTablistColors)))
				.add(new Spacer(spacer))
				.add(configToggle("Show main next to account",
						mod.config::setShowPersonNextToAccount,
						mod.config::isShowPersonNextToAccount));
	}

	private GuiElement buildHudConfig() {
		return new FlexListLayout(ROW)
				.add(new Label("Left HUD:", ALIGN_LEFT))
				.add(new Spacer(spacer))
				.add(configToggle("Health pot count",
						mod.config::setShowHudHealthPotCount,
						mod.config::isShowHudHealthPotCount))
				.add(new Spacer(spacer))
				.add(configToggle("Nearby player count",
						mod.config::setShowHudRadarPlayerCount,
						mod.config::isShowHudRadarPlayerCount))
				.add(new Spacer(spacer))
				.add(configToggle("Focused account",
						mod.config::setShowHudFocus,
						mod.config::isShowHudFocus));
	}

	private TextField stretchingTextField(Predicate<String> validator, String text) {
		final TextField textField = new TextField(validator, text);
		textField.setWeight(stretchX);
		return textField;
	}

	private CycleButton<String> configToggle(String name, Consumer<Boolean> setter, Supplier<Boolean> getter) {
		return new CycleButton<>(
				value -> setter.accept(toggleToBool(value)),
				boolToToggle(name, getter.get()),
				boolToToggle(name, true),
				boolToToggle(name, false));
	}

	private GuiElement configFormatRow(
			@Nonnull String name,
			@Nullable String tooltip,
			@Nonnull Supplier<GlobalConfig.VisibilityFormat> visibilityFormatGetter,
			@Nonnull Consumer<GlobalConfig.VisibilityFormat> visibilityFormatSetter,
			@Nullable Supplier<GlobalConfig.StandingFilter> standingFilterGetter,
			@Nullable Consumer<GlobalConfig.StandingFilter> standingFilterSetter,
			@Nonnull Supplier<String> formatGetter,
			@Nonnull Consumer<String> formatSetter
	) {
		final FlexListLayout row = new FlexListLayout(ROW);

		GuiElement label = new Label(name + ":", ALIGN_LEFT);
		if (tooltip != null && !tooltip.trim().isEmpty()) {
			label = new Tooltip(tooltip, label);
		}
		row.add(label);
		row.add(new Spacer(spacer));

		final GuiElement visibilityFormatSelector = new CycleButton<>(
				visibilityFormatSetter,
				visibilityFormatGetter.get(),
				GlobalConfig.VisibilityFormat.values());
		row.add(new Tooltip("Formatted: Use the following format to display the messages."
				+ "\nOriginal: Show the message as received from the server."
				+ "\nHidden: Don't show this kind of message at all.",
				visibilityFormatSelector));
		row.add(new Spacer(spacer));

		if (standingFilterSetter != null && standingFilterGetter != null) {
			final GuiElement standingFilterSelector = new CycleButton<>(
					standingFilterSetter,
					standingFilterGetter.get(),
					GlobalConfig.StandingFilter.values());
			row.add(new Label("From: ", ALIGN_LEFT));
			row.add(new Tooltip("Only display messages for players with this standing.",
					standingFilterSelector));
			row.add(new Spacer(spacer));
		}

		row.add(new Label("Format: ", ALIGN_LEFT));

		// XXX preview formats with dummy data
		// TODO show available format params in tooltip
		row.add(stretchingTextField(text -> {
			formatSetter.accept(text);
			return true;
		}, formatGetter.get()));
		return row;
	}
}
