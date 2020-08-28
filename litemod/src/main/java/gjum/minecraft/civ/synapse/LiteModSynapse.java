package gjum.minecraft.civ.synapse;

import com.mojang.authlib.GameProfile;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mumfrey.liteloader.*;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.util.EntityUtilities;
import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.common.observations.*;
import gjum.minecraft.civ.synapse.common.observations.accountpos.*;
import gjum.minecraft.civ.synapse.common.observations.game.*;
import gjum.minecraft.civ.synapse.common.observations.instruction.FocusAnnouncement;
import gjum.minecraft.civ.synapse.common.packet.JsonPacket;
import gjum.minecraft.civ.synapse.config.*;
import gjum.minecraft.civ.synapse.connection.Client;
import gjum.minecraft.civ.synapse.gui.MainGui;
import gjum.minecraft.civ.synapse.gui.PanelGui;
import gjum.minecraft.civ.synapse.integrations.CombatRadarHelper;
import gjum.minecraft.civ.synapse.integrations.WaypointManager;
import gjum.minecraft.gui.GuiRoot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.world.GameType;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static gjum.minecraft.civ.synapse.McUtil.*;
import static gjum.minecraft.civ.synapse.ObservationFormatter.addCoordClickEvent;
import static gjum.minecraft.civ.synapse.ObservationFormatter.formatObservationStatic;
import static gjum.minecraft.civ.synapse.common.Util.*;

public class LiteModSynapse implements Tickable, Configurable, EntityRenderListener, HUDRenderListener, JoinGameListener, PacketHandler {
	public static final String MOD_NAME = "Synapse";

	public static KeyBinding chatPosKeybind = new KeyBinding("Pre-fill position into chat", Keyboard.KEY_NONE, MOD_NAME);
	public static KeyBinding openGuiKeybind = new KeyBinding("Open settings GUI", Keyboard.KEY_NONE, MOD_NAME);
	public static KeyBinding toggleEnabledKeybind = new KeyBinding("Toggle enabled/disabled", Keyboard.KEY_NONE, MOD_NAME);
	public static KeyBinding setFocusEntityKeybind = new KeyBinding("Set focus on player under crosshair", Keyboard.KEY_NONE, MOD_NAME);

	public static final String waypointCommandJourneyMap = "/jm wpedit [name:%s, x:%s, y:%s, z:%s]";
	public static final String waypointCommandVoxelMap = "/newWaypoint name:%s, x:%s, y:%s, z:%s";

	public static double closeDistance = 64;

	@Nullable
	public String gameAddress;
	private long loginTime = 0;
	@Nullable
	public String worldName = null;

	@Nonnull
	public GlobalConfig config = new GlobalConfig();
	@Nonnull
	public AccountsConfig accountsConfig = new AccountsConfig();
	@Nullable
	public ServerConfig serverConfig;
	@Nullable
	public PersonsConfig personsConfig;
	@Nullable
	public WaypointManager waypointManager;

	@Nonnull
	private PlayerTracker playerTracker = new PlayerTrackerIngame(null);
	private long lastSync = 0;

	@Nonnull
	private Client comms = new Client("none?", "none?");

	@Nonnull
	private Collection<String> focusedAccountNames = Collections.emptyList();

	@Nullable
	public GuiScreen gui = null;

	public static LiteModSynapse instance;

	private static final File modConfigDir = new File(getMc().mcDataDir, MOD_NAME);

	public LiteModSynapse() {
		instance = this;
	}

	@Override
	public String getName() {
		return MOD_NAME;
	}

	@Override
	public String getVersion() {
		return "Development_Build";
	}

	@Override
	public Class<? extends com.mumfrey.liteloader.modconfig.ConfigPanel> getConfigPanelClass() {
		return PanelGui.class;
	}

	@Override
	public void init(File configPath) {
		LiteLoader.getInput().registerKeyBinding(chatPosKeybind);
		LiteLoader.getInput().registerKeyBinding(openGuiKeybind);
		LiteLoader.getInput().registerKeyBinding(setFocusEntityKeybind);
		LiteLoader.getInput().registerKeyBinding(toggleEnabledKeybind);

		LiteLoader.getInstance().writeConfig(this);

		// move old config if it exists
		File oldConfigDir = new File(getMc().mcDataDir, "Hydrate");
		if (oldConfigDir.exists() && !modConfigDir.exists()) {
			oldConfigDir.renameTo(modConfigDir);
		}

		modConfigDir.mkdirs();
		config.load(new File(modConfigDir, "config.json"));
		config.saveLater(null);
		accountsConfig.load(new File(modConfigDir, "accounts.txt"));
		accountsConfig.saveLater(null);

		// enabled by default on this server; but don't override existing config
		final File civRealmsConfigDir = new File(modConfigDir, "servers/civrealms.com");
		if (civRealmsConfigDir.mkdirs()) {
			new ServerConfig().saveLater(new File(civRealmsConfigDir, "server.json"));
			new PersonsConfig().saveLater(new File(civRealmsConfigDir, "persons.json"));
		}

		//comms.connect();
	}

	@Override
	public void upgradeSettings(String version, File configPath, File oldConfigPath) {
	}

	public boolean isConnectedToGame() {
		return loginTime > 0 && gameAddress != null;
	}

	public boolean isServerEnabled() {
		return serverConfig != null && serverConfig.isEnabled();
	}

	public void setServerEnabled(boolean enabled) {
		if (serverConfig == null && enabled) {
			loadServerRelatedConfigs(true);
		}
		if (serverConfig != null) serverConfig.setEnabled(enabled);
		checkModActive();
	}

	public boolean isModActive() {
		return config.isModEnabled() && isServerEnabled();
	}

	public void checkModActive() {
		if (!config.isModEnabled()) {
			onModDeactivated();
			return;
		}
		if (serverConfig == null) {
			loadServerRelatedConfigs(false);
		}
		if (!isModActive()) {
			onModDeactivated();
			return;
		}

		// TODO only call onModActivated if it was not active before
		onModActivated();
	}

	private void onModActivated() {
		checkCommsAddress();

		if (Objects.equals(playerTracker.gameAddress, gameAddress)) {
			return; // mod is already active
		}

		playerTracker = new PlayerTrackerIngame(gameAddress);
		if (waypointManager != null) waypointManager.updateAllWaypoints();
	}

	public void checkCommsAddress() {
		if (serverConfig != null) {
			if (comms != null && (!serverConfig.getCommsAddress().equals(comms.address)
					|| !serverConfig.getProxyAddress().equals(comms.proxy_address))) {
				comms.disconnect();
				comms.address = serverConfig.getCommsAddress();
				comms.proxy_address = serverConfig.getProxyAddress();
			}
			if (comms == null) comms = new Client(serverConfig.getCommsAddress(), serverConfig.getProxyAddress());
			if (!comms.isEncrypted()) comms.connect();
		}
	}

	private void onModDeactivated() {
		final WorldClient world = getMc().world;
		if (world != null) {
			for (EntityPlayer player : world.playerEntities) {
				player.setGlowing(false);
			}
		}
		if (waypointManager != null) waypointManager.updateAllWaypoints();
		comms.disconnect();
	}

	private void loadServerRelatedConfigs(boolean create) {
		try {
			if (gameAddress == null) return;

			if (serverConfig != null
					&& personsConfig != null
					&& waypointManager != null
			) return;

			final File serverConfigDir = getServerConfigDir(gameAddress, create);
			if (serverConfigDir == null) return;

			final CombatRadarHelper combatRadarHelper = new CombatRadarHelper();
			waypointManager = new WaypointManager();

			serverConfig = new ServerConfig();
			serverConfig.registerChangeHandler(combatRadarHelper);
			serverConfig.registerChangeHandler(waypointManager);
			serverConfig.load(new File(serverConfigDir, "server.json"));
			serverConfig.saveLater(null);

			personsConfig = new PersonsConfig();
			personsConfig.getPersonsRegistry().registerChangeHandler(combatRadarHelper);
			personsConfig.getPersonsRegistry().registerChangeHandler(waypointManager);
			personsConfig.load(new File(serverConfigDir, "persons.json"));
			personsConfig.saveLater(null);
		} catch (Throwable e) {
			printErrorRateLimited(e);
			serverConfig = null;
			personsConfig = null;
			waypointManager = null;
		}
	}

	/**
	 * If `create` is false and no directory matches, return null.
	 * Otherwise, reuse existing directory or create it.
	 * Allows omitting 25565 default port.
	 */
	@Nullable
	private static File getServerConfigDir(@Nonnull String gameAddress, boolean create) {
		final String[] addressTries = {
				gameAddress,
				gameAddress.endsWith(":25565")
						? gameAddress.replaceFirst(":25565$", "")
						: gameAddress + ":25565"};
		final File serversConfigsDir = new File(modConfigDir, "servers/");
		for (String addressTry : addressTries) {
			final File serverConfigDir = new File(serversConfigsDir,
					addressTry.replace(":", " "));
			if (serverConfigDir.isDirectory()) {
				return serverConfigDir;
			}
		}
		// no matching config directory exists
		if (create) {
			final File serverConfigDir = new File(serversConfigsDir,
					gameAddress.replace(":", " "));
			serverConfigDir.mkdirs();
			return serverConfigDir;
		} else {
			return null;
		}
	}

	public void showGuiAndRemember(@Nullable GuiScreen gui) {
		if (gui == null || gui instanceof GuiRoot) {
			this.gui = gui;
		} // else: some Minecraft gui; don't retain it
		if (gui instanceof GuiRoot) ((GuiRoot) gui).rebuild();
		getMc().displayGuiScreen(gui);
	}

	public void openLastGui() {
		if (gui == null) gui = new MainGui(null);
		getMc().displayGuiScreen(gui);
	}

	@Override
	public void onJoinGame(INetHandler netHandler, SPacketJoinGame joinGamePacket, ServerData serverData, RealmsServer realmsServer) {
		try {
			final String prevAddress = gameAddress;
			gameAddress = Minecraft.getMinecraft().getCurrentServerData()
					.serverIP.split("/")[0]
					.toLowerCase();
			loginTime = System.currentTimeMillis();

			if (!gameAddress.equals(prevAddress)) {
				serverConfig = null;
				personsConfig = null;
				waypointManager = null;
				focusedAccountNames.clear();
			}
			checkModActive();
		} catch (Throwable e) {
			printErrorRateLimited(e);
		}
	}

	@Override
	public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean isGameTick) {
		try {
			final boolean noGuiOpen = minecraft.currentScreen == null;
			final boolean handleKeyPresses = inGame && noGuiOpen;
			if (handleKeyPresses) {
				if (toggleEnabledKeybind.isPressed()) {
					config.setModEnabled(!config.isModEnabled());
				}
				if (setFocusEntityKeybind.isPressed()) {
					focusEntityUnderCrosshair();
				}
				if (chatPosKeybind.isPressed()) {
					final Pos pos = getEntityPosition(getMc().player);
					getMc().displayGuiScreen(new GuiChat(String.format(
							"[x:%s y:%s z:%s name:%s]",
							pos.x, pos.y, pos.z,
							getSelfAccount())));
				}
				if (openGuiKeybind.isPressed()) {
					openLastGui();
				}
			}
			if (waypointManager != null && inGame && isGameTick) {
				waypointManager.onTick();
			}
			if (lastSync < System.currentTimeMillis() - config.getSyncInterval()) {
				lastSync = System.currentTimeMillis();
				syncComms();
			}
		} catch (Throwable e) {
			printErrorRateLimited(e);
		}
	}

	private void syncComms() {
		if (!comms.isEncrypted()) return;
		if (getMc().world == null) return;
		boolean flushEveryPacket = false;
		for (EntityPlayer player : getMc().world.playerEntities) {
			if (player == getMc().player) continue; // send more info for self at the end
			// TODO don't send if pos didn't change
			comms.sendEncrypted(new JsonPacket(new PlayerState(getSelfAccount(),
					player.getName(), getEntityPosition(player), worldName)
			), flushEveryPacket);
		}
		final PlayerState selfState = new PlayerState(getSelfAccount(),
				getSelfAccount(), getEntityPosition(getMc().player), worldName);
		//selfState.heading = headingFromYawDegrees(getMc().player.rotationYawHead);
		//selfState.health = getHealth();
		//selfState.hpotCount = getNumHealthPots();
		// TODO send combat tag end, min armor dura
		comms.sendEncrypted(new JsonPacket(selfState), true);
	}

	@Override
	public void onRenderEntity(
			Render<? extends Entity> render, Entity entity,
			double xPos, double yPos, double zPos, float yaw, float partialTicks) {
		try {
			if (!isModActive()) return;
			if (entity instanceof EntityPlayer) {
				entity.setGlowing(config.isPlayerGlow() && !entity.isInvisible() && !entity.isSneaking());
			}
		} catch (Throwable e) {
			printErrorRateLimited(e);
		}
	}

	@Override
	public void onPostRenderEntity(
			Render<? extends Entity> render, Entity entity,
			double xPos, double yPos, double zPos, float yaw, float partialTicks) {
		try {
			if (!isModActive()) return;
			if (entity instanceof EntityPlayer && !entity.isInvisible() && shouldRenderPlayerDecoration(entity)) {
				try {
					prepareRenderPlayerDecorations(entity, partialTicks);
					ScorePlayerTeam team = null;
					FloatColor color = null;
					boolean computedTeam = false;
					if (config.isPlayerMiddleHoop()) {
						if (!computedTeam) {
							team = config.getStandingTeam(getStanding(entity.getName()));
							if (team != null) color = FloatColor.fromTextFormatting(team.getColor());
							computedTeam = true;
						}
						if (team != null) {
							renderHoop(entity, 0.5, 1, partialTicks, color);
						}
					}
					if (config.isPlayerOuterHoops()) {
						if (!computedTeam) {
							team = config.getStandingTeam(getStanding(entity.getName()));
							if (team != null) color = FloatColor.fromTextFormatting(team.getColor());
							computedTeam = true;
						}
						if (team != null) {
							renderHoop(entity, 0.3, 0.01, partialTicks, color);
							renderHoop(entity, 0.3, 1.8, partialTicks, color);
						}
					}
					if (config.isPlayerBox()) {
						if (!computedTeam) {
							team = config.getStandingTeam(getStanding(entity.getName()));
							if (team != null) color = FloatColor.fromTextFormatting(team.getColor());
							computedTeam = true;
						}
						if (team != null) {
							renderBox(entity, partialTicks, color);
						}
					}
				} finally {
					resetRenderPlayerDecorations();
				}
			}
		} catch (Throwable e) {
			printErrorRateLimited(e);
		}
	}

	private void prepareRenderPlayerDecorations(@Nonnull Entity entity, float partialTicks) {
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GlStateManager.glLineWidth(config.getPlayerLineWidth());

		GlStateManager.enableDepth();
		GlStateManager.depthMask(true);
		if (!entity.isSneaking()) {
			GlStateManager.depthMask(false);
			GlStateManager.disableDepth();
		}
	}

	private void resetRenderPlayerDecorations() {
		GlStateManager.enableTexture2D();
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.enableDepth();
		GlStateManager.depthMask(true);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private void renderBox(Entity entity, float partialTicks, FloatColor color) {
		final EntityPlayerSP player = getMc().player;
		double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
		double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
		double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;

		float entityX = (float) (entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks);
		float entityY = (float) (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks);
		float entityZ = (float) (entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks);

		double renderX = entityX - playerX;
		double renderY = entityY - playerY;
		double renderZ = entityZ - playerZ;

		final double halfWidth = entity.width / 2;
		final AxisAlignedBB box = new AxisAlignedBB(
				renderX - halfWidth, renderY, renderZ - halfWidth,
				renderX + halfWidth, renderY + entity.height, renderZ + halfWidth);

		RenderGlobal.drawSelectionBoundingBox(box, color.r, color.g, color.b, 1);
	}

	private void renderHoop(Entity entity, double radius, double yOffset, float partialTicks, FloatColor color) {
		final EntityPlayerSP player = getMc().player;
		float playerX = (float) (player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks);
		float playerY = (float) (player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks);
		float playerZ = (float) (player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks);

		float entityX = (float) (entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks);
		float entityY = (float) (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks);
		float entityZ = (float) (entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks);

		try {
			GlStateManager.pushMatrix();
			GlStateManager.translate(entityX - playerX, entityY - playerY, entityZ - playerZ);

			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder bufferBuilder = tessellator.getBuffer();
			bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
			double theta = 0.19634954084936207D;
			double c = Math.cos(theta);
			double s = Math.sin(theta);
			double x = radius;
			double y = 0.0D;
			for (int circleSegment = 0; circleSegment < 32; circleSegment++) {
				bufferBuilder.pos(x, yOffset, y).color(color.r, color.g, color.b, 1f).endVertex();
				double t = x;
				x = c * x - s * y;
				y = s * t + c * y;
			}
			tessellator.draw();
		} finally {
			GlStateManager.popMatrix();
		}
	}

	private static boolean shouldRenderPlayerDecoration(Entity ent) {
		if (ent != Minecraft.getMinecraft().getRenderViewEntity()) return true;
		if (Minecraft.getMinecraft().gameSettings.thirdPersonView != 0) return true;
		final GuiScreen screen = Minecraft.getMinecraft().currentScreen;
		final boolean guiShowsPlayer = screen instanceof GuiInventory || screen instanceof GuiContainerCreative;
		return guiShowsPlayer && Minecraft.getMinecraft().getRenderManager().playerViewY == 180.0F;
	}

	@Override
	public void onPreRenderHUD(int screenWidth, int screenHeight) {
		try {
			if (!isModActive()) return;
			final boolean isChatOpen = getMc().currentScreen instanceof GuiChat;
			final int chatHeight = getMc().ingameGUI.getChatGUI().getChatHeight();
			if (isChatOpen && chatHeight > getMc().currentScreen.height / 2) return;

			int y = (screenHeight - 10 * 8) / 2;
			if (config.isShowHudHealthPotCount()) {
				renderHealthPotCountHud(y += 10);
				y += 10;
			}
			if (config.isShowHudRadarPlayerCount()) {
				// TODO hide each nearby player count if zero?
				renderPlayerCountHud(y += 10, "hostile", Standing.HOSTILE);
				renderPlayerCountHud(y += 10, "friendly", Standing.FRIENDLY);
				renderPlayerCountHud(y += 10, "total", null);
				y += 10;
			}
			final int wrapWidth = 200;
			AccountPosObservation lastFocusObservation = null;
			if (config.isShowHudFocus() && !focusedAccountNames.isEmpty()) {
				lastFocusObservation = playerTracker.getMostRecentPosObservationForAccounts(focusedAccountNames);
				final int focusColor = getStandingColor(Standing.FOCUS).getRGB();
				String text = null;
				if (lastFocusObservation != null) {
					final ITextComponent msg = formatObservationWithVisibility(
							null, lastFocusObservation, null);
					if (msg != null) {
						String age = formatAge(lastFocusObservation.getTime());
						text = age + " " + msg.getFormattedText();
					}
				}
				if (text == null) {
					text = String.join(" ",
							sortedUniqListIgnoreCase(focusedAccountNames));
				}
				y += 10 * drawSplitStringWithShadow(text,
						1, y + 10, wrapWidth, focusColor);
				y += 10;
			}
		} catch (Throwable e) {
			printErrorRateLimited(e);
		}
	}

	@Override
	public void onPostRenderHUD(int screenWidth, int screenHeight) {
	}

	private static int drawSplitStringWithShadow(String msg, int x, int y, int wrapWidth, int color) {
		getMc().fontRenderer.drawSplitString(msg.replaceAll("§[0-9a-f]", ""),
				x + 1, y + 1, wrapWidth, Color.BLACK.getRGB());
		getMc().fontRenderer.drawSplitString(msg, x, y, wrapWidth, color);
		return getMc().fontRenderer.listFormattedStringToWidth(msg, wrapWidth).size();
	}

	private void renderPlayerCountHud(int y, String text, @Nullable Standing standing) {
		int numPlayers = getNumVisiblePlayersWithStanding(standing);
		if (standing == Standing.HOSTILE) {
			numPlayers += getNumVisiblePlayersWithStanding(Standing.FOCUS);
		}
		final String str = numPlayers + " " + text + " near";
		final int color = numPlayers == 0
				? Color.GRAY.getRGB()
				: getStandingColor(standing).getRGB();
		getMc().fontRenderer.drawStringWithShadow(str, 1, y, color);
	}

	private int getNumVisiblePlayersWithStanding(@Nullable Standing standing) {
		if (standing == null) return getMc().world.playerEntities.size() - 1; // don't count the player itself
		int count = 0;
		for (EntityPlayer p : getMc().world.playerEntities) {
			if (p == getMc().player) continue; // don't count the player itself
			if (getStanding(p.getName()) == standing) {
				count++;
			}
		}
		return count;
	}

	private void renderHealthPotCountHud(int y) {
		final long numHealthPots = getNumHealthPots();
		final String str = numHealthPots + " hpots";
		getMc().fontRenderer.drawStringWithShadow(str, 1, y, Color.MAGENTA.getRGB());
	}

	@Nullable
	public PersonsRegistry getPersonsRegistry() {
		if (personsConfig == null) return null;
		return personsConfig.getPersonsRegistry();
	}

	@Nonnull
	public PlayerTracker getPlayerTracker() {
		return playerTracker;
	}

	/**
	 * null means unknown observation type; caller should show original message in that case.
	 */
	@Nullable
	public String getObservationFormat(@Nonnull Observation observation) {
		if (observation instanceof SnitchHit) return config.getSnitchHitFormat();
		if (observation instanceof RadarChange) return config.getRadarFormat();
		if (observation instanceof Skynet) return config.getSkynetFormat();
		if (observation instanceof PearlLocation) return config.getPearlLocationFormat();
		if (observation instanceof CombatTagChat) return config.getCombatTagFormat();
		if (observation instanceof GroupChat) return config.getGroupChatFormat();
		if (observation instanceof FocusAnnouncement) return config.getFocusAnnouncementFormat();
		return null; // unknown observation type; use original message
	}

	/**
	 * @param fmtStr If null, it is guessed with getObservationFormat()
	 */
	@Nullable
	public ITextComponent formatObservationWithVisibility(
			@Nullable String fmtStr,
			@Nonnull Observation observation,
			@Nullable ITextComponent originalMsg
	) {
		try {
			final GlobalConfig.VisibilityFormat visibilityFormat = config.getVisibilityFormat(observation);
			switch (visibilityFormat) {
				case HIDDEN:
					return null;
				case ORIGINAL:
					return originalMsg;
				case FORMATTED:
					break;
				default:
					throw new IllegalStateException("Invalid VisibilityFormat: " + visibilityFormat);
			}

			final TextFormatting color = getChatColor(observation);
			if (color == null) return null; // invisible at this urgency level/config

			if (fmtStr == null) fmtStr = getObservationFormat(observation);

			// XXX use {format} stuff
			final ITextComponent formatted;
			if (fmtStr != null) {
				formatted = formatObservationStatic(fmtStr, observation);
				formatted.getStyle().setColor(color);
			} else if (originalMsg != null) {
				formatted = originalMsg;
			} else {
				return null; // drop remote message with unknown format
			}

			final String waypointCommandFormat = config.isUseVoxelMap() ? waypointCommandVoxelMap
					: config.isUseJourneyMap() ? waypointCommandJourneyMap : null;
			addCoordClickEvent(formatted, observation, waypointCommandFormat);

			return formatted;
		} catch (Throwable e) {
			printErrorRateLimited(e);
			return originalMsg;
		}
	}

	/**
	 * null = invisible/hide = don't show message
	 */
	// XXX get rid of method: use {format} stuff
	@Nullable
	public TextFormatting getChatColor(@Nonnull Observation observation) {
		final Visibility visibility = getObservationVisibility(observation);
		switch (visibility) {
			case ALERT:
				return TextFormatting.RED;
			default:
			case SHOW:
				return TextFormatting.WHITE;
			case DULL:
				return TextFormatting.GRAY;
			case HIDE:
				return null;
		}
	}

	@Nonnull
	public Visibility getObservationVisibility(@Nonnull Observation observation) {
		if (!(observation instanceof AccountObservation)) return Visibility.SHOW;
		final AccountObservation accObs = (AccountObservation) observation;
		final Standing standing = getStanding(accObs.getAccount());
		if (!config.matchesStandingFilter(standing, config.getStandingFilter(observation))) {
			return Visibility.HIDE;
		}
		final boolean isClose = isClose(accObs);
		return config.getChatVisibility(isClose, standing);
	}

	private boolean isClose(@Nonnull AccountObservation observation) {
		final EntityPlayer playerEntity = getMc().world.getPlayerEntityByName(observation.getAccount());
		boolean isClose = playerEntity != null;
		final EntityPlayerSP self = getMc().player;
		if (self != null) {
			Pos pos = null;
			if (playerEntity != null) {
				pos = getEntityPosition(playerEntity);
			} else if (observation instanceof AccountPosObservation) {
				pos = ((AccountPosObservation) observation).getPos();
			}
			if (pos != null) isClose = closeDistance * closeDistance > pos.distanceSq(self.posX, self.posY, self.posZ);
		}
		return isClose;
	}

	@Nonnull
	public static Color getStandingColor(@Nullable Standing standing) {
		final TextFormatting standingFmt = LiteModSynapse.instance.config
				.getStandingColor(standing);
		return FloatColor.fromTextFormatting(standingFmt).toColor();
	}

	@Nonnull
	public TextFormatting getDistanceColor(int distance) {
		if (distance < closeDistance) return TextFormatting.GOLD;
		if (distance < 500) return TextFormatting.YELLOW;
		if (distance < 1000) return TextFormatting.WHITE;
		return TextFormatting.GRAY;
	}

	public void handleObservation(@Nonnull Observation obs) {
		handleChatObservation(obs, null);
	}

	public void handleChatObservation(@Nonnull Observation obs, @Nullable ITextComponent originalChat) {
		final boolean isNew = getPlayerTracker().recordObservation(obs);

		if (obs instanceof PlayerState) {
			final PlayerState playerState = (PlayerState) obs;
			// don't update waypoint if that player is on radar anyway
			if (waypointManager != null && null == getMc().world.getPlayerEntityByName(playerState.getAccount())) {
				try {
					waypointManager.updateAccountLocation(playerState);
				} catch (Throwable e) {
					printErrorRateLimited(e);
				}
			}
			return;
		}

		if (!isNew) return;

		if (obs instanceof AccountPosObservation) {
			final AccountPosObservation apObs = (AccountPosObservation) obs;
			if (waypointManager != null) {
				try {
					waypointManager.updateAccountLocation(apObs);
				} catch (Throwable e) {
					printErrorRateLimited(e);
				}
			}
		}
		if (obs instanceof PearlLocation && waypointManager != null) {
			try {
				waypointManager.updatePearlLocation((PearlLocation) obs);
			} catch (Throwable e) {
				printErrorRateLimited(e);
			}
		}

		// ignore skynet spam at login
		final boolean skynetIgnored = obs instanceof Skynet && loginTime + 1000 > System.currentTimeMillis();
		if (!skynetIgnored) {
			try {
				final ITextComponent formattedMsg = formatObservationWithVisibility(null, obs, originalChat);
				if (formattedMsg != null) {
					getMc().ingameGUI.getChatGUI().printChatMessage(formattedMsg);
				}
			} catch (Throwable e) {
				printErrorRateLimited(e);
				if (originalChat != null) getMc().ingameGUI.getChatGUI().printChatMessage(originalChat);
			}
		}

		if (getSelfAccount().equals(obs.getWitness()) && comms != null) {
			comms.sendEncrypted(new JsonPacket(obs));
		}
	}

	public void onJoinedWorldFromChat(String world) {
		this.worldName = world;
	}

	public boolean isFocusedAccount(@Nonnull String account) {
		if (focusedAccountNames.isEmpty()) return false;
		return focusedAccountNames.contains(account.toLowerCase());
	}

	private void focusEntityUnderCrosshair() {
		try {
			if (!isModActive()) return;
			if (getMc().objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) return;
			Entity entityHit = getMc().objectMouseOver.entityHit;
			if (entityHit == null) { // do long trace only if default short trace didn't hit yet
				Vec3d traceStart = EntityUtilities.getPositionEyes(getMc().player, getMc().getRenderPartialTicks());
				final Method method = EntityUtilities.class.getDeclaredMethod("rayTraceEntities", Entity.class, double.class, float.class, double.class, Vec3d.class);
				method.setAccessible(true);
				final Object trace = method.invoke(null, getMc().player, 64.0, getMc().getRenderPartialTicks(), 64.0, traceStart);
				final Field entityField = trace.getClass().getDeclaredField("entity");
				entityField.setAccessible(true);
				entityHit = (Entity) entityField.get(trace);
			}
			if (!(entityHit instanceof EntityPlayer)) return;
			final EntityPlayer player = (EntityPlayer) entityHit;
			// allow re-sending focus message
//			if (isFocusedAccount(player.getName())) return;
			announceFocusedAccount(player.getName());
		} catch (Throwable e) {
			printErrorRateLimited(e);
		}
	}

	public void setFocusedAccountNames(@Nullable Collection<String> accounts) {
		final Collection<String> impactedAccounts = new ArrayList<>(focusedAccountNames);
		focusedAccountNames = lowerCaseSet(accounts);
		impactedAccounts.addAll(focusedAccountNames);
		final PersonsRegistry personsRegistry = getPersonsRegistry();
		if (personsRegistry != null) {
			personsRegistry.propagateLargeChange(impactedAccounts.stream()
					.map(personsRegistry::personByAccountName)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet()));
		}
		if (waypointManager != null) waypointManager.updateAllWaypoints();
	}

	public void announceFocusedAccount(@Nonnull String account) {
		announceFocusedAccounts(Collections.singletonList(account));
	}

	public void announceFocusedAccounts(@Nonnull Collection<String> focusedAccounts) {
		focusedAccounts = sortedUniqListIgnoreCase(focusedAccounts);
		setFocusedAccountNames(focusedAccounts);
		comms.sendEncrypted(new JsonPacket(new FocusAnnouncement(
				getSelfAccount(), focusedAccounts)));
		getMc().ingameGUI.addChatMessage(ChatType.CHAT, new TextComponentString(
				"Focusing: " + String.join(" ", focusedAccounts)));
	}

	@Nonnull
	public Standing getStanding(String account) {
		if (serverConfig == null) return Standing.UNSET;
		return serverConfig.getAccountStanding(account);
	}

	public ITextComponent getDisplayNameForAccount(@Nonnull String accountName) {
		accountName = accountName.replaceAll("§.", "");
		if (!isModActive() || !config.isReplaceNamePlates()) return new TextComponentString(accountName);
		if (getPersonsRegistry() == null || serverConfig == null) return new TextComponentString(accountName);
		// set standing color even when person is unknown
		final Standing standing = serverConfig.getAccountStanding(accountName);
		final TextFormatting color = config.getStandingColor(standing);
		final ITextComponent displayName = new TextComponentString(accountName)
				.setStyle(new Style().setColor(color));
		if (config.isShowPersonNextToAccount()) {
			final Person person = getPersonsRegistry().personByAccountName(accountName);
			if (person != null && !person.isMain(accountName)) {
				displayName.appendSibling(new TextComponentString(" (" + person.getName() + ")")
						.setStyle(new Style().setColor(TextFormatting.GRAY)));
			}
		}
		return displayName;
	}

	@Nullable
	public ITextComponent handleChat(ITextComponent original) {
		try {
			ObservationImpl observation = ChatHandler.observationFromChat(original);
			if (observation != null) {
				handleChatObservation(observation, original);
				return null; // formatted+printed by observation handler, if new and visible
			}
		} catch (Throwable e) {
			printErrorRateLimited(e);
		}
		return original;
	}

	@Override
	public List<Class<? extends Packet<?>>> getHandledPackets() {
		return Arrays.asList(
				SPacketSpawnPlayer.class,
				SPacketDestroyEntities.class,
				SPacketPlayerListItem.class);
	}

	@Override
	public boolean handlePacket(INetHandler netHandler, Packet<?> packetRaw) {
		final boolean allowFurtherProcessing = true;
		if (!isModActive()) return allowFurtherProcessing;
		try {
			if (!getMc().isCallingFromMinecraftThread()
					&& (packetRaw instanceof SPacketSpawnPlayer
					|| packetRaw instanceof SPacketDestroyEntities
					|| packetRaw instanceof SPacketPlayerListItem)) {
				getMc().addScheduledTask(() -> handlePacket(netHandler, packetRaw));
				return allowFurtherProcessing;
			}
			if (packetRaw instanceof SPacketSpawnPlayer) {
				// record player entering entity render distance
				handlePacketSpawnPlayer((SPacketSpawnPlayer) packetRaw);
			} else if (packetRaw instanceof SPacketDestroyEntities) {
				// record player leaving entity render distance
				handlePacketDestroyEntities((SPacketDestroyEntities) packetRaw);
			} else if (packetRaw instanceof SPacketPlayerListItem) {
				final SPacketPlayerListItem packet = (SPacketPlayerListItem) packetRaw;
				handlePacketPlayerListItem(packet);
			}
		} catch (Throwable e) {
			printErrorRateLimited(e);
		}
		return allowFurtherProcessing;
	}

	private void handlePacketSpawnPlayer(SPacketSpawnPlayer packet) {
		final UUID uuid = packet.getUniqueId();
		final String accountName = getMc().getConnection().getPlayerInfo(uuid)
				.getGameProfile().getName().replaceAll("§.", "");
		final Pos pos = new Pos(
				MathHelper.floor(packet.getX()),
				MathHelper.floor(packet.getY()),
				MathHelper.floor(packet.getZ()));
		final RadarChange observation = new RadarChange(
				getSelfAccount(), accountName, pos, worldName, Action.APPEARED);
		try {
			if (config.isPlayRadarSound()) {
				final Standing standing = mapNonNull(serverConfig, sc ->
						sc.getAccountStanding(accountName));
				final String soundName = config.getStandingSound(standing);
				if (soundName != null) playSound(soundName, uuid);
			}
		} catch (Throwable e) {
			printErrorRateLimited(e);
		}
		handleObservation(observation);
	}

	private void handlePacketDestroyEntities(SPacketDestroyEntities packet) {
		for (int eid : packet.getEntityIDs()) {
			final Entity entity = getMc().world.getEntityByID(eid);
			if (!(entity instanceof EntityPlayer)) continue;
			final EntityPlayer player = (EntityPlayer) entity;
			final RadarChange observation = new RadarChange(
					getSelfAccount(), player.getName(), getEntityPosition(player), worldName, Action.DISAPPEARED);
			handleObservation(observation);
		}
	}

	private void handlePacketPlayerListItem(SPacketPlayerListItem packet) {
		for (SPacketPlayerListItem.AddPlayerData entry : packet.getEntries()) {
			final GameProfile profile = entry.getProfile();
			final UUID uuid = profile.getId();
			if (packet.getAction() == SPacketPlayerListItem.Action.ADD_PLAYER) {
				if (profile.getName() == null || profile.getName().isEmpty()) continue;
				if (profile.getName().contains("~")) continue; // dummy entry by TabListPlus

				final NetworkPlayerInfo existingPlayerInfo = getMc().getConnection().getPlayerInfo(profile.getId());
				if (existingPlayerInfo != null) continue; // already logged in

				final String accountName = profile.getName().replaceAll("§.", "").trim();
				accountsConfig.addAccount(accountName);

				// TODO detect combat logger by comparing uuid
				Action action = Action.LOGIN;
				if (entry.getGameMode() == GameType.NOT_SET) {
					action = Action.CTLOG;
				}

				final Skynet observation = new Skynet(
						getSelfAccount(), uuid, accountName, action, entry.getGameMode().getID());
				handleObservation(observation);
			} else if (packet.getAction() == SPacketPlayerListItem.Action.REMOVE_PLAYER) {
				final NetworkPlayerInfo playerInfo = getMc().getConnection()
						.getPlayerInfo(uuid);
				if (playerInfo == null) continue;
				final GameProfile existingProfile = playerInfo.getGameProfile();
				if (existingProfile.getName() == null) continue;
				// ignore dummy entries by TabListPlus
				if (existingProfile.getName().contains("~")) continue;
				final String accountName = existingProfile.getName().replaceAll("§.", "").trim();
				if (accountName.isEmpty()) continue;

				final Skynet observation = new Skynet(
						getSelfAccount(), uuid, accountName, Action.LOGOUT, playerInfo.getGameType().getID());
				handleObservation(observation);
			}
		}
	}

	public static void playSound(@Nonnull String soundName, @Nonnull UUID playerUuid) {
		if (soundName.isEmpty() || "none".equalsIgnoreCase(soundName)) return;
		float playerPitch = 0.5F + 1.5F * (new Random(playerUuid.hashCode())).nextFloat();
		final ResourceLocation resource = new ResourceLocation(soundName);
		getMc().player.playSound(new SoundEvent(resource), 1.0F, playerPitch);
	}

	public long getLoginTime() {
		return loginTime;
	}

	public void handleCommsConnected() {
		getMc().addScheduledTask(() -> {
			// XXX store msg for gui, update gui if open
		});
	}

	public void handleCommsEncryptionSuccess(String message) {
		getMc().addScheduledTask(() -> {

			// XXX store msg for gui, update gui if open
		});
	}

	public void handleCommsDisconnected(Throwable cause) {
		getMc().addScheduledTask(() -> {
			// XXX store msg for gui, update gui if open
		});
	}

	public void handleCommsJson(Object payload) {
		getMc().addScheduledTask(() -> {
			if (payload instanceof Observation) {
				handleObservation((Observation) payload);
			}
		});
	}
}
