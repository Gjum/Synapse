package gjum.minecraft.civ.synapse.config;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.*;
import gjum.minecraft.civ.synapse.common.observations.Observation;
import gjum.minecraft.civ.synapse.common.observations.accountpos.RadarChange;
import gjum.minecraft.civ.synapse.common.observations.accountpos.SnitchHit;
import gjum.minecraft.civ.synapse.common.observations.game.*;
import gjum.minecraft.civ.synapse.common.observations.instruction.FocusAnnouncement;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;

import static gjum.minecraft.civ.synapse.McUtil.isJourneyMapLoaded;
import static gjum.minecraft.civ.synapse.integrations.VoxelMapHelper.isVoxelMapActive;

public class GlobalConfig extends JsonConfig {
	@Expose
	private boolean modEnabled = true;

	@Expose
	private boolean useVoxelMap = true;

	@Expose
	private boolean useJourneyMap = true;

	@Expose
	private long maxWaypointAge = 10 * 60 * 1000;

	@Expose
	private boolean showPersonNextToAccount = true;

	@Expose
	private boolean replaceTablistColors = true;

	@Expose
	private boolean replaceNamePlates = true;

	@Expose
	private boolean showHudHealthPotCount = true;

	@Expose
	private boolean showHudRadarPlayerCount = true;

	@Expose
	private boolean showHudFocus = true;

	@Expose
	private boolean playRadarSound = true;

	@Expose
	private boolean playerMiddleHoop = false;

	@Expose
	private boolean playerOuterHoops = false;

	@Expose
	private boolean playerBox = true;

	@Expose
	private boolean playerGlow = false;

	@Expose
	private float playerLineWidth = 2;

	@Expose
	private long syncInterval = 1000;

	public enum VisibilityFormat {
		FORMATTED("Formatted"),
		ORIGINAL("Original"),
		HIDDEN("Hidden");

		private final String buttonText;

		VisibilityFormat(String buttonText) {
			this.buttonText = buttonText;
		}

		@Override
		public String toString() {
			return buttonText;
		}
	}

	public enum StandingFilter {
		FOCUSED("Focused only"),
		HOSTILE("Hostile only"),
		HOSTILE_FRIENDLY("Hostile+Friendly"),
		NOT_FRIENDLY ("Not Friendly"),
		EVERYONE("All players");

		private final String buttonText;

		StandingFilter(String buttonText) {
			this.buttonText = buttonText;
		}

		@Override
		public String toString() {
			return buttonText;
		}
	}

	@Expose
	private VisibilityFormat skynetVisibilityFormat = VisibilityFormat.FORMATTED;
	@Expose
	private StandingFilter skynetStandingFilter = StandingFilter.EVERYONE;
	@Expose
	private String skynetFormat = "[Skynet]%ACTIONSHORT% %ACCOUNTPERSON% %ACTION%";

	@Expose
	private VisibilityFormat radarVisibilityFormat = VisibilityFormat.FORMATTED;
	@Expose
	private StandingFilter radarStandingFilter = StandingFilter.EVERYONE;
	@Expose
	private String radarFormat = "[Radar]%ACTIONSHORT% %ACCOUNTPERSON% %DISTANCEDELTA%%RELATIVE% %DISTANCE% [%XYZ%]%OFFWORLD% heading %HEADING%";

	@Expose
	private VisibilityFormat snitchHitVisibilityFormat = VisibilityFormat.FORMATTED;
	@Expose
	private StandingFilter snitchHitStandingFilter = StandingFilter.EVERYONE;
	@Expose
	private String snitchHitFormat = "[Snitch]%ACTIONSHORT% %ACCOUNTPERSON% %DISTANCEDELTA%%RELATIVE% %DISTANCE% [%XYZ%]%OFFWORLD% heading %HEADING% [%GROUP%] %SNITCH%";

	@Expose
	private VisibilityFormat pearlLocationVisibilityFormat = VisibilityFormat.FORMATTED;
	@Expose
	private String pearlLocationFormat = "[Pearl] %PRISONER% held by %HOLDER% %DISTANCEDELTA%%RELATIVE% %DISTANCE% [%XYZ%]%OFFWORLD% heading %HEADING%";

	@Expose
	private VisibilityFormat combatTagVisibilityFormat = VisibilityFormat.FORMATTED;
	@Expose
	private String combatTagFormat = "[Combat] with %ACCOUNTPERSON%";

	@Expose
	private VisibilityFormat groupChatVisibilityFormat = VisibilityFormat.FORMATTED;
	@Expose
	private String groupChatFormat = "[%GROUP%] %ACCOUNTPERSON%: §f%MESSAGE%";

	@Expose
	private VisibilityFormat focusAnnouncementVisibilityFormat = VisibilityFormat.FORMATTED;
	@Expose
	private String focusAnnouncementFormat = "[Focus] %ACCOUNTPERSON% %DISTANCEDELTA%%RELATIVE% %DISTANCE% [%XYZ%]%OFFWORLD% heading %HEADING%";

	@Expose
	private HashMap<Standing, String> standingSounds = new HashMap<>();

	@Expose
	private HashMap<Standing, TextFormatting> standingColors = new HashMap<>();

	private HashMap<Standing, ScorePlayerTeam> standingTeams = new HashMap<>();

	private static final Scoreboard scoreboard = new Scoreboard();

	@Expose
	private Visibility visibilityNearbyFriendly = Visibility.DULL;
	@Expose
	private Visibility visibilityNearbyHostile = Visibility.ALERT;
	@Expose
	private Visibility visibilityNearbyNeutral = Visibility.SHOW;
	@Expose
	private Visibility visibilityNearbyUnsetStanding = Visibility.SHOW;

	@Expose
	private Visibility visibilityFarFriendly = Visibility.DULL;
	@Expose
	private Visibility visibilityFarHostile = Visibility.SHOW;
	@Expose
	private Visibility visibilityFarNeutral = Visibility.DULL;
	@Expose
	private Visibility visibilityFarUnsetStanding = Visibility.DULL;

	public GlobalConfig() {
		//initialize();
	}

	protected void fillDefaults() {
		//
		if (getStandingColor(Standing.FOCUS) == null)
			setStandingColor(Standing.FOCUS, TextFormatting.YELLOW);
		if (getStandingColor(Standing.FRIENDLY) == null)
			setStandingColor(Standing.FRIENDLY, TextFormatting.AQUA);
		if (getStandingColor(Standing.HOSTILE) == null)
			setStandingColor(Standing.HOSTILE, TextFormatting.RED);
		if (getStandingColor(Standing.NEUTRAL) == null)
			setStandingColor(Standing.NEUTRAL, TextFormatting.DARK_GREEN);
		if (getStandingColor(Standing.UNSET) == null)
			setStandingColor(Standing.UNSET, TextFormatting.WHITE);

		if (getStandingSound(Standing.FRIENDLY) == null)
			setStandingSound(Standing.FRIENDLY, "block.note.chime");
		if (getStandingSound(Standing.HOSTILE) == null)
			setStandingSound(Standing.HOSTILE, "block.note.pling");
		if (getStandingSound(Standing.NEUTRAL) == null)
			setStandingSound(Standing.NEUTRAL, "block.note.harp");
		if (getStandingSound(Standing.UNSET) == null)
			setStandingSound(Standing.UNSET, getStandingSound(Standing.NEUTRAL));
		if (getStandingSound(Standing.FOCUS) == null)
			setStandingSound(Standing.FOCUS, getStandingSound(Standing.HOSTILE));
	}

	@Override
	protected Object getData() {
		return this;
	}

	@Override
	protected void setData(Object data) {
		final GlobalConfig newConf = (GlobalConfig) data;
		newConf.saveLater(saveLocation);
		LiteModSynapse.instance.config = newConf;
		newConf.fillDefaults();
	}

	@Nonnull
	public GlobalConfig.VisibilityFormat getVisibilityFormat(@Nonnull Observation observation) {
		if (observation instanceof Skynet) return getSkynetVisibilityFormat();
		if (observation instanceof RadarChange) return getRadarVisibilityFormat();
		if (observation instanceof SnitchHit) return getSnitchHitVisibilityFormat();
		if (observation instanceof PearlLocation) return getPearlLocationVisibilityFormat();
		if (observation instanceof CombatTagChat) return getCombatTagVisibilityFormat();
		if (observation instanceof GroupChat) return getGroupChatVisibilityFormat();
		if (observation instanceof FocusAnnouncement) return getFocusAnnouncementVisibilityFormat();
		return VisibilityFormat.ORIGINAL;
	}

	@Nonnull
	public GlobalConfig.StandingFilter getStandingFilter(@Nonnull Observation observation) {
		if (observation instanceof Skynet) return getSkynetStandingFilter();
		if (observation instanceof RadarChange) return getRadarStandingFilter();
		if (observation instanceof SnitchHit) return getSnitchHitStandingFilter();
		return StandingFilter.EVERYONE;
	}

	public boolean matchesStandingFilter(@Nonnull Standing standing, @Nonnull StandingFilter standingFilter) {
		switch (standingFilter) {
			case FOCUSED:
				return standing == Standing.FOCUS;
			case HOSTILE:
				return standing == Standing.HOSTILE || standing == Standing.FOCUS;
			case HOSTILE_FRIENDLY:
				return standing == Standing.FRIENDLY || standing == Standing.HOSTILE || standing == Standing.FOCUS;
			case NOT_FRIENDLY:
				return standing != Standing.FRIENDLY;
			default:
			case EVERYONE:
				return true;
		}
	}

	public Visibility getChatVisibility(boolean isClose, @Nullable Standing standing) {
		if (standing == null) standing = Standing.UNSET;
		switch (standing) {
			case FRIENDLY:
				return isClose ? getVisibilityNearbyFriendly() : getVisibilityFarFriendly();
			case FOCUS:
			case HOSTILE:
				return isClose ? getVisibilityNearbyHostile() : getVisibilityFarHostile();
			case NEUTRAL:
				return isClose ? getVisibilityNearbyNeutral() : getVisibilityFarNeutral();
			default:
			case UNSET:
				return isClose ? getVisibilityNearbyUnsetStanding() : getVisibilityFarUnsetStanding();
		}
	}

	public boolean isModEnabled() {
		return modEnabled;
	}

	public void setModEnabled(boolean enabled) {
		modEnabled = enabled;
		saveLater(null);
		LiteModSynapse.instance.checkModActive();
	}

	public boolean isUseVoxelMap() {
		return useVoxelMap && isVoxelMapActive();
	}

	public void setUseVoxelMap(boolean enabled) {
		useVoxelMap = enabled;
		saveLater(null);
		if (LiteModSynapse.instance.waypointManager != null) {
			LiteModSynapse.instance.waypointManager.updateAllWaypoints();
		}
	}

	public boolean isUseJourneyMap() {
		return useJourneyMap && isJourneyMapLoaded();
	}

	public void setUseJourneyMap(boolean enabled) {
		useJourneyMap = enabled;
		saveLater(null);
		if (LiteModSynapse.instance.waypointManager != null) {
			LiteModSynapse.instance.waypointManager.updateAllWaypoints();
		}
	}

	public long getMaxWaypointAge() {
		return maxWaypointAge;
	}

	public void setMaxWaypointAge(long ageMs) {
		maxWaypointAge = ageMs;
		saveLater(null);
		if (LiteModSynapse.instance.waypointManager != null) {
			LiteModSynapse.instance.waypointManager.updateAllWaypoints();
		}
	}

	public boolean isShowPersonNextToAccount() {
		return showPersonNextToAccount;
	}

	public void setShowPersonNextToAccount(boolean enabled) {
		this.showPersonNextToAccount = enabled;
		saveLater(null);
	}

	public boolean isReplaceTablistColors() {
		return replaceTablistColors;
	}

	public void setReplaceTablistColors(boolean enabled) {
		this.replaceTablistColors = enabled;
		saveLater(null);
	}

	public VisibilityFormat getSkynetVisibilityFormat() {
		return skynetVisibilityFormat;
	}

	public void setSkynetVisibilityFormat(VisibilityFormat enabled) {
		skynetVisibilityFormat = enabled;
		saveLater(null);
	}

	public StandingFilter getSkynetStandingFilter() {
		return skynetStandingFilter;
	}

	public void setSkynetStandingFilter(StandingFilter enabled) {
		skynetStandingFilter = enabled;
		saveLater(null);
	}

	public String getSkynetFormat() {
		return skynetFormat;
	}

	public void setSkynetFormat(String skynetFormat) {
		this.skynetFormat = skynetFormat;
		saveLater(null);
	}

	public VisibilityFormat getRadarVisibilityFormat() {
		return radarVisibilityFormat;
	}

	public void setRadarVisibilityFormat(VisibilityFormat enabled) {
		radarVisibilityFormat = enabled;
		saveLater(null);
	}

	public StandingFilter getRadarStandingFilter() {
		return radarStandingFilter;
	}

	public void setRadarStandingFilter(StandingFilter enabled) {
		radarStandingFilter = enabled;
		saveLater(null);
	}

	public String getRadarFormat() {
		return radarFormat;
	}

	public void setRadarFormat(String radarFormat) {
		this.radarFormat = radarFormat;
		saveLater(null);
	}

	public boolean isPlayRadarSound() {
		return playRadarSound;
	}

	public void setPlayRadarSound(boolean enabled) {
		playRadarSound = enabled;
		saveLater(null);
	}

	public VisibilityFormat getSnitchHitVisibilityFormat() {
		return snitchHitVisibilityFormat;
	}

	public void setSnitchHitVisibilityFormat(VisibilityFormat enabled) {
		snitchHitVisibilityFormat = enabled;
		saveLater(null);
	}

	public StandingFilter getSnitchHitStandingFilter() {
		return snitchHitStandingFilter;
	}

	public void setSnitchHitStandingFilter(StandingFilter enabled) {
		snitchHitStandingFilter = enabled;
		saveLater(null);
	}

	public String getSnitchHitFormat() {
		return snitchHitFormat;
	}

	public void setSnitchHitFormat(String format) {
		snitchHitFormat = format;
		saveLater(null);
	}

	public VisibilityFormat getPearlLocationVisibilityFormat() {
		return pearlLocationVisibilityFormat;
	}

	public void setPearlLocationVisibilityFormat(VisibilityFormat enabled) {
		this.pearlLocationVisibilityFormat = enabled;
		saveLater(null);
	}

	public String getPearlLocationFormat() {
		return pearlLocationFormat;
	}

	public void setPearlLocationFormat(String format) {
		this.pearlLocationFormat = format;
		saveLater(null);
	}

	public VisibilityFormat getCombatTagVisibilityFormat() {
		return combatTagVisibilityFormat;
	}

	public void setCombatTagVisibilityFormat(VisibilityFormat enabled) {
		this.combatTagVisibilityFormat = enabled;
		saveLater(null);
	}

	public String getCombatTagFormat() {
		return combatTagFormat;
	}

	public void setCombatTagFormat(String format) {
		this.combatTagFormat = format;
		saveLater(null);
	}

	public VisibilityFormat getGroupChatVisibilityFormat() {
		return groupChatVisibilityFormat;
	}

	public void setGroupChatVisibilityFormat(VisibilityFormat enabled) {
		this.groupChatVisibilityFormat = enabled;
		saveLater(null);
	}

	public String getGroupChatFormat() {
		return groupChatFormat;
	}

	public void setGroupChatFormat(String format) {
		this.groupChatFormat = format;
		saveLater(null);
	}

	public VisibilityFormat getFocusAnnouncementVisibilityFormat() {
		return focusAnnouncementVisibilityFormat;
	}

	public void setFocusAnnouncementVisibilityFormat(VisibilityFormat enabled) {
		this.focusAnnouncementVisibilityFormat = enabled;
		saveLater(null);
	}

	public String getFocusAnnouncementFormat() {
		return focusAnnouncementFormat;
	}

	public void setFocusAnnouncementFormat(String format) {
		this.focusAnnouncementFormat = format;
		saveLater(null);
	}

	public TextFormatting getStandingColor(@Nullable Standing standing) {
		if (standing == null) standing = Standing.UNSET;
		return standingColors.get(standing);
	}

	public void setStandingColor(Standing standing, @Nonnull TextFormatting color) {
		standingColors.put(standing, color);
		saveLater(null);
		final ScorePlayerTeam team = standingTeams.computeIfAbsent(standing, s -> new ScorePlayerTeam(scoreboard, standing.name()));
		team.setPrefix(color.toString());
		team.setColor(color);
	}

	@Nullable
	public String getStandingSound(Standing standing) {
		if (standing == null) standing = Standing.UNSET;
		return standingSounds.get(standing);
	}

	public void setStandingSound(Standing standing, String sound) {
		sound = sound.trim();
		standingSounds.put(standing, sound.isEmpty() ? null : sound);
		saveLater(null);
	}

	public Visibility getVisibilityNearbyFriendly() {
		return visibilityNearbyFriendly;
	}

	public Visibility getVisibilityNearbyHostile() {
		return visibilityNearbyHostile;
	}

	public Visibility getVisibilityNearbyNeutral() {
		return visibilityNearbyNeutral;
	}

	public Visibility getVisibilityNearbyUnsetStanding() {
		return visibilityNearbyUnsetStanding;
	}

	public Visibility getVisibilityFarFriendly() {
		return visibilityFarFriendly;
	}

	public Visibility getVisibilityFarHostile() {
		return visibilityFarHostile;
	}

	public Visibility getVisibilityFarNeutral() {
		return visibilityFarNeutral;
	}

	public Visibility getVisibilityFarUnsetStanding() {
		return visibilityFarUnsetStanding;
	}

	public ScorePlayerTeam getStandingTeam(Standing standing) {
		return standingTeams.get(standing);
	}

	public boolean isPlayerGlow() {
		return false;
	}

	public void setPlayerGlow(boolean playerGlow) {
		this.playerGlow = playerGlow;
		saveLater(null);
	}

	public boolean isPlayerMiddleHoop() {
		return playerMiddleHoop;
	}

	public void setPlayerMiddleHoop(boolean playerMiddleHoop) {
		this.playerMiddleHoop = playerMiddleHoop;
		saveLater(null);
	}

	public boolean isPlayerOuterHoops() {
		return playerOuterHoops;
	}

	public void setPlayerOuterHoops(boolean playerOuterHoops) {
		this.playerOuterHoops = playerOuterHoops;
		saveLater(null);
	}

	public boolean isPlayerBox() {
		return playerBox;
	}

	public void setPlayerBox(boolean playerBox) {
		this.playerBox = playerBox;
		saveLater(null);
	}

	public float getPlayerLineWidth() {
		if (playerLineWidth <= 0) playerLineWidth = 2;
		return playerLineWidth;
	}

	public void setPlayerLineWidth(float playerLineWidth) {
		this.playerLineWidth = playerLineWidth;
		saveLater(null);
	}

	public long getSyncInterval() {
		return syncInterval;
	}

	public void setSyncInterval(long ms) {
		syncInterval = ms;
		saveLater(null);
	}

	public boolean isReplaceNamePlates() {
		return replaceNamePlates;
	}

	public void setReplaceNamePlates(boolean replaceNamePlates) {
		this.replaceNamePlates = replaceNamePlates;
		saveLater(null);
	}

	public boolean isShowHudHealthPotCount() {
		return showHudHealthPotCount;
	}

	public void setShowHudHealthPotCount(boolean showHudHealthPotCount) {
		this.showHudHealthPotCount = showHudHealthPotCount;
		saveLater(null);
	}

	public boolean isShowHudRadarPlayerCount() {
		return showHudRadarPlayerCount;
	}

	public void setShowHudRadarPlayerCount(boolean showHudRadarPlayerCount) {
		this.showHudRadarPlayerCount = showHudRadarPlayerCount;
		saveLater(null);
	}

	public boolean isShowHudFocus() {
		return showHudFocus;
	}

	public void setShowHudFocus(boolean showHudFocus) {
		this.showHudFocus = showHudFocus;
		saveLater(null);
	}
}
