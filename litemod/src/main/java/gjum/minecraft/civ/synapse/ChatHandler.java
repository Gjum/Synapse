package gjum.minecraft.civ.synapse;

import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.common.observations.Action;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;
import gjum.minecraft.civ.synapse.common.observations.accountpos.PearlTransport;
import gjum.minecraft.civ.synapse.common.observations.accountpos.SnitchHit;
import gjum.minecraft.civ.synapse.common.observations.game.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gjum.minecraft.civ.synapse.McUtil.getLookedAtBlockPos;
import static gjum.minecraft.civ.synapse.McUtil.getSelfAccount;
import static gjum.minecraft.civ.synapse.common.Util.getMatchGroupOrNull;
import static gjum.minecraft.civ.synapse.common.Util.mapNonNull;
import static gjum.minecraft.civ.synapse.common.observations.game.PearlLocation.isPlayerHolder;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.MULTILINE;

public class ChatHandler {
	public static final Pattern pearlLocationPattern = Pattern.compile(
			"^(?:Your pearl is )?held by (?<holder>[ _a-zA-Z0-9]+) at (?:(?<world>\\S+) )?(?<x>-?\\d+)[, ]+(?<y>-?\\d+)[, ]+(?<z>-?\\d+).*", CASE_INSENSITIVE);
	public static final Pattern pearlBroadcastPattern = Pattern.compile(
			"^(?:\\[(?<group>[^\\]]+)\\] ?)?The pearl of (?<prisoner>\\S+) is held by (?<holder>[ _a-zA-Z0-9]+) \\[x?:?(?<x>-?\\d+)[, ]+y?:?(?<y>-?\\d+)[, ]+z?:?(?<z>-?\\d+) (?<world>[\\S]+)\\]", CASE_INSENSITIVE);

	public static final Pattern snitchHitPattern = Pattern.compile(
			"^\\s*\\*\\s+([A-Za-z0-9_]{2,16})\\s+(entered|logged out|logged in) (?:in |to )?snitch at (\\S*) \\[(?:(\\S+)\\s)?\\s*(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)\\].*", CASE_INSENSITIVE);
	public static final Pattern snitchHoverPattern = Pattern.compile(
			"^(?i)\\s*Location:\\s*\\[(.+?) (-?\\d+) (-?\\d+) (-?\\d+)\\]\\s*Group:\\s*(\\S+?)\\s*Type:\\s*(Entry|Logging)\\s*(?:(?:Hours to cull|Cull):\\s*(\\d+\\.\\d+)h?)?\\s*(?:Previous name:\\s*(\\S+?))?\\s*(?:Name:\\s*(\\S+?))?\\s*", CASE_INSENSITIVE + MULTILINE);

	public static final Pattern bastionInfoNonePattern = Pattern.compile(
			"^No Bastion Block.*", CASE_INSENSITIVE);
	public static final Pattern bastionInfoFriendlyPattern = Pattern.compile(
			"^A Bastion Block prevents others from building.*", CASE_INSENSITIVE);
	public static final Pattern bastionInfoHostilePattern = Pattern.compile(
			"^A Bastion Block prevents you building.*", CASE_INSENSITIVE);
	public static final Pattern bastionRemovedBlockPattern = Pattern.compile(
			"^Bastion removed block.*", CASE_INSENSITIVE);
	public static final Pattern bastionRemovedBoatPattern = Pattern.compile(
			"^Boat blocked by bastion.*", CASE_INSENSITIVE);

	public static final Pattern combatTagPattern = Pattern.compile(
			"^You have engaged in(?:to)? combat with (?<account>[A-Za-z0-9_]{2,16}).*", CASE_INSENSITIVE);
	public static final Pattern combatEndPattern = Pattern.compile(
			"^You are no longer (?:in )?combat(?: ?tagged)?.*", CASE_INSENSITIVE);
	public static final Pattern pearledPattern = Pattern.compile(
			"^You've been bound to an? (?<pearlType>exile|prison)? *pearl by (?<account>[A-Za-z0-9_]{2,16}).*", CASE_INSENSITIVE);

	public static final Pattern groupChatPattern = Pattern.compile(
			"^\\[(?<group>\\S+)\\] (?<account>[A-Za-z0-9_]{2,16}): (?<message>.*)$", CASE_INSENSITIVE);
	public static final Pattern localChatPattern = Pattern.compile(
			"^<(?<account>[A-Za-z0-9_]{2,16})>:? +(?<message>.*)$", CASE_INSENSITIVE);
	public static final Pattern privateChatPattern = Pattern.compile(
			"^(?<direction>From|To) (?<account>[A-Za-z0-9_]{2,16}): (?<message>.*)$", CASE_INSENSITIVE);
	public static final Pattern brandNewPattern = Pattern.compile(
			"^(?<account>[A-Za-z0-9_]{2,16}) is brand new.*", CASE_INSENSITIVE);

	@Nullable
	public static ObservationImpl observationFromChat(@Nonnull ITextComponent originalMessage) {
		final String msgColored = originalMessage.getFormattedText();
		final ObservationImpl observation = observationFromChatInternal(originalMessage, msgColored);
		if (observation != null) observation.setMessagePlain(msgColored);
		return observation;
	}

	@Nullable
	private static ObservationImpl observationFromChatInternal(@Nonnull ITextComponent originalMessage, String msgColored) {
		final LiteModSynapse mod = LiteModSynapse.instance;

		msgColored = msgColored.replaceAll("§r", "").trim();
		if (msgColored.startsWith("§bJoined world: ")) {
			// CivRealms world announcement
			final String world = msgColored
					.split(": ", 2)[1]
					.replaceAll("§.", "")
					.split(" ", 2)[0];
			mod.onJoinedWorldFromChat(world);
			return new WorldJoinChat(getSelfAccount(), world);
		}

		final String msg = TextFormatting.getTextWithoutFormattingCodes(msgColored).trim();

		final Matcher snitchHitMatcher = snitchHitPattern.matcher(msg);
		if (snitchHitMatcher.matches()) {
			final String account = snitchHitMatcher.group(1);
			final Action action = Action.fromString(snitchHitMatcher.group(2).toLowerCase());
			final String snitch = snitchHitMatcher.group(3);
			final String world = snitchHitMatcher.group(4);
			final int x = Integer.parseInt(snitchHitMatcher.group(5));
			final int y = Integer.parseInt(snitchHitMatcher.group(6));
			final int z = Integer.parseInt(snitchHitMatcher.group(7));
			String group = null;
			String type = null;
			final String hover = hoverTextFromMessage(originalMessage);
			if (hover != null) {
				final Matcher hoverMatcher = snitchHoverPattern.matcher(hover);
				if (hoverMatcher.matches()) {
					group = hoverMatcher.group(5);
					type = hoverMatcher.group(6);
				}
			}
			final Pos pos = new Pos(x, y, z);
			return new SnitchHit(
					getSelfAccount(),
					account,
					pos,
					world,
					action,
					snitch,
					group,
					type);
		}

		final Matcher pearlLocationMatcher = pearlLocationPattern.matcher(msg);
		if (pearlLocationMatcher.matches()) {
			final String prisoner = getSelfAccount();
			final String holder = pearlLocationMatcher.group("holder");
			final int x = Integer.parseInt(pearlLocationMatcher.group("x"));
			final int y = Integer.parseInt(pearlLocationMatcher.group("y"));
			final int z = Integer.parseInt(pearlLocationMatcher.group("z"));
			final String world = getMatchGroupOrNull("world", pearlLocationMatcher);
			return makePearlLocationOrTransport(
					getSelfAccount(),
					new Pos(x, y, z),
					world,
					prisoner,
					holder);
		}

		final Matcher pearlBroadcastMatcher = pearlBroadcastPattern.matcher(msg);
		if (pearlBroadcastMatcher.matches()) {
			final String prisoner = pearlBroadcastMatcher.group("prisoner");
			final String holder = pearlBroadcastMatcher.group("holder");
			final int x = Integer.parseInt(pearlBroadcastMatcher.group("x"));
			final int y = Integer.parseInt(pearlBroadcastMatcher.group("y"));
			final int z = Integer.parseInt(pearlBroadcastMatcher.group("z"));
			final String world = getMatchGroupOrNull("world", pearlBroadcastMatcher);
			return makePearlLocationOrTransport(
					getSelfAccount(),
					new Pos(x, y, z),
					world,
					prisoner,
					holder);
		}

		final Matcher groupChatMatcher = groupChatPattern.matcher(msg);
		if (groupChatMatcher.matches()) {
			final String group = groupChatMatcher.group("group");
			final String account = groupChatMatcher.group("account");
			final String message = groupChatMatcher.group("message").trim();
			return new GroupChat(
					getSelfAccount(),
					group,
					account,
					message);
		}

		final Matcher localChatMatcher = localChatPattern.matcher(msg);
		if (localChatMatcher.matches()) {
			final String account = localChatMatcher.group("account");
			final String message = localChatMatcher.group("message").trim();
			return new GroupChat(
					getSelfAccount(),
					null,
					account,
					message);
		}

		final Matcher combatTagMatcher = combatTagPattern.matcher(msg);
		if (combatTagMatcher.matches()) {
			final String account = combatTagMatcher.group("account");
			return new CombatTagChat(getSelfAccount(), account);
		}

		final Matcher combatEndMatcher = combatEndPattern.matcher(msg);
		if (combatEndMatcher.matches()) {
			return new CombatEndChat(getSelfAccount());
		}

		final Matcher pearledMatcher = pearledPattern.matcher(msg);
		if (pearledMatcher.matches()) {
			final String account = pearledMatcher.group("account");
			final String pearlType = pearledMatcher.group("pearlType");
			return new PearledChat(getSelfAccount(), account, pearlType);
		}

		final Matcher bastionInfoNoneMatcher = bastionInfoNonePattern.matcher(msg);
		if (bastionInfoNoneMatcher.matches()) {
			Pos pos = mapNonNull(getLookedAtBlockPos(5), McUtil::pos);
			return new BastionChat(getSelfAccount(), pos, mod.worldName,
					BastionChat.State.NONE, BastionChat.Source.INFO);
		}
		final Matcher bastionInfoFriendlyMatcher = bastionInfoFriendlyPattern.matcher(msg);
		if (bastionInfoFriendlyMatcher.matches()) {
			Pos pos = mapNonNull(getLookedAtBlockPos(5), McUtil::pos);
			return new BastionChat(getSelfAccount(), pos, mod.worldName,
					BastionChat.State.FRIENDLY, BastionChat.Source.INFO);
		}
		final Matcher bastionInfoHostileMatcher = bastionInfoHostilePattern.matcher(msg);
		if (bastionInfoHostileMatcher.matches()) {
			Pos pos = mapNonNull(getLookedAtBlockPos(5), McUtil::pos);
			return new BastionChat(getSelfAccount(), pos, mod.worldName,
					BastionChat.State.HOSTILE, BastionChat.Source.INFO);
		}
		final Matcher bastionRemovedBlockMatcher = bastionRemovedBlockPattern.matcher(msg);
		if (bastionRemovedBlockMatcher.matches()) {
			Pos pos = mapNonNull(getLookedAtBlockPos(5), McUtil::pos);
			return new BastionChat(getSelfAccount(), pos, mod.worldName,
					BastionChat.State.HOSTILE, BastionChat.Source.BLOCK);
		}
		final Matcher bastionRemovedBoatMatcher = bastionRemovedBoatPattern.matcher(msg);
		if (bastionRemovedBoatMatcher.matches()) {
			Pos pos = mapNonNull(getLookedAtBlockPos(5), McUtil::pos);
			return new BastionChat(getSelfAccount(), pos, mod.worldName,
					BastionChat.State.HOSTILE, BastionChat.Source.BOAT);
		}

		final Matcher brandNewMatcher = brandNewPattern.matcher(msg);
		if (brandNewMatcher.matches()) {
			final String account = brandNewMatcher.group("account");
			return new BrandNew(getSelfAccount(), account);
		}

		// XXX match other chat messages - any that contain account or pos, so they can be formatted and sent to teammates

		return null;
	}

	@Nonnull
	private static ObservationImpl makePearlLocationOrTransport(String witness, Pos pos, String world, String prisoner, String holder) {
		if (isPlayerHolder(holder)) return new PearlTransport(witness, pos, world, prisoner, holder);
		else return new PearlLocation(witness, pos, world, prisoner, holder);
	}

	@Nullable
	private static String hoverTextFromMessage(ITextComponent message) {
		HoverEvent hover;
		List<ITextComponent> siblings = message.getSiblings();
		if (siblings == null || siblings.isEmpty()) {
			hover = message.getStyle().getHoverEvent();
		} else {
			ITextComponent hoverComponent = siblings.get(0);
			hover = hoverComponent.getStyle().getHoverEvent();
		}
		if (hover == null) {
			return null;
		}
		String text = hover.getValue().getUnformattedComponentText();
		if (text.trim().isEmpty()) {
			return null;
		}
		return text;
	}
}
