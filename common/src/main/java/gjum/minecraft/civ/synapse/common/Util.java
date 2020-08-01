package gjum.minecraft.civ.synapse.common;

import org.apache.commons.text.similarity.LevenshteinDistance;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class Util {
	public static final String separators = ",;/";
	public static final String accountsSepRegex = "[" + separators + " ]+";
	public static final String factionsSepRegex = "[" + separators + "]+";
	public static final String accountNameRegex = "[A-Za-z0-9_]{2,16}";

	public static final SimpleDateFormat dateFmtHms = new SimpleDateFormat("HH:mm:ss");

	private static final LevenshteinDistance levenshtein = new LevenshteinDistance();

	public static final Function<Float, Float> identityFloatFunction = f -> f;

	public static float scoreSimilarity(String s1, String s2) {
		final String lower1 = s1.toLowerCase().replaceAll("[_]+", "");
		final String lower2 = s2.toLowerCase().replaceAll("[_]+", "");
		if (lower1.equals(lower2)) return 3;
		if (lower1.startsWith(lower2) || lower2.startsWith(lower1)) return 2;
		if (lower1.contains(lower2) || lower2.contains(lower1)) return 1;
		int distAbs = levenshtein.apply(s1, s2);
		return 1 - ((float) distAbs) / Math.max(s1.length(), s2.length());
	}

	@Nonnull
	public static String getLastWord(@Nonnull String words) {
		int lastWordIdx = getLastWordIndex(words);
		return words.substring(lastWordIdx);
	}

	@Nonnull
	public static String replaceLastWord(@Nonnull String words, @Nonnull String word) {
		int lastWordIdx = getLastWordIndex(words);
		return words.substring(0, lastWordIdx) + word;
	}

	private static int getLastWordIndex(@Nonnull String words) {
		return 1 + Math.max(Math.max(
				words.lastIndexOf(' '),
				words.lastIndexOf(',')), Math.max(
				words.lastIndexOf(';'),
				words.lastIndexOf('/')));
	}

	@Nonnull
	public static List<String> sortedUniqListIgnoreCase(@Nullable Collection<String> strings) {
		if (strings == null) return Collections.emptyList();
		return strings.stream()
				.sorted(Comparator.comparing(String::toLowerCase))
				.distinct()
				.collect(Collectors.toList());
	}

	@Nonnull
	public static Set<String> lowerCaseSet(@Nullable Collection<String> strings) {
		if (strings == null) return Collections.emptySet();
		return strings.stream().map(String::toLowerCase).collect(Collectors.toSet());
	}

	@Nullable
	public static String containsIgnoreCase(@Nullable String query, @Nonnull Collection<String> candidates) {
		if (query == null) return null;
		query = query.toLowerCase();
		for (String candidate : candidates) {
			if (candidate.toLowerCase().equals(query)) {
				return candidate;
			}
		}
		return null;
	}

	@Nullable
	public static <T extends Enum<T>> T enumOrNull(@Nonnull Class<T> enumType, @Nonnull String name) {
		try {
			return Enum.valueOf(enumType, name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	@Nullable
	public static String getMatchGroupOrNull(String key, Matcher matcher) {
		try {
			return matcher.group(key);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static HashMap<String, Long> lastTimeSeenError = new HashMap<>();

	public static void printErrorRateLimited(@Nonnull Throwable e) {
		try {
			final long now = System.currentTimeMillis();
			final String key = e.getMessage();
			if (lastTimeSeenError.getOrDefault(key, 0L) > now - 10000L) return;
			lastTimeSeenError.put(key, now);
			e.printStackTrace();
		} catch (Throwable e2) {
			e2.printStackTrace();
		}
	}

	public static final String[] headings8 = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	@Nonnull
	public static String headingFromDelta(final int dx, final int dy, final int dz) {
		final double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length < 10) return "near";

		// check if up or down rather than to the side
		final double horizLen = Math.sqrt(dx * dx + dz * dz);
		if (horizLen < 5) return dy > 0 ? "up" : "down";

		// cross = Vec(dx, dy, dz).crossProduct(dx, 0, dz)
		final double crossX = dy * dz;
		// crossY = 0
		final double crossZ = 0 - dy * dx;
		final double crossLen = Math.sqrt(crossX * crossX + crossZ * crossZ);
		final double pitch = Math.asin(crossLen / length / horizLen);
		if (pitch > Math.PI / 4) return dy > 0 ? "up" : "down";

		// to the side: calculate bearing
		final double yawRadians = Math.atan2(-dz, dx);
		return headingFromYawRadians(yawRadians);
	}

	@Nonnull
	public static String headingFromYawRadians(double yawRadians) {
		final double yawEights = yawRadians * 8 / (2 * Math.PI);
		int alignedIndex = 2 - (int) Math.round(yawEights);
		while (alignedIndex < 0) alignedIndex += 8 * 8 * 8;
		return headings8[(alignedIndex + 8) % 8];
	}

	@Nonnull
	public static String headingFromYawDegrees(double yawDegrees) {
		final double yawEights = yawDegrees * 8 / 360;
		int alignedIndex = 4 + (int) Math.round(yawEights);
		while (alignedIndex < 0) alignedIndex += 8 * 8 * 8;
		return headings8[(alignedIndex + 8) % 8];
	}

	@Nonnull
	public static String addDashesToUuid(@Nonnull String s) {
		return s.replaceFirst(
				"(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
				"$1-$2-$3-$4-$5");
	}

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

	@Nonnull
	public static String bytesToHex(@Nullable byte[] bytes) {
		if (bytes == null) return "null";
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}

	@Nonnull
	public static String repeatString(@Nonnull String s, @Nonnull String sep, int count) {
		if (count == 0) return "";
		if (count == 1) return s;
		StringBuilder out = new StringBuilder(s);
		for (int i = 1; i < count; i++) {
			out.append(sep);
			out.append(s);
		}
		return out.toString();
	}

	@Nonnull
	public static String formatAge(long timestamp) {
		final long age = System.currentTimeMillis() - timestamp;
		if (age < 0) {
			return "future";
		} else if (age < 10 * 1000) {
			return "now";
		} else if (age < 60 * 1000) {
			return "" + (age / 1000 / 10) * 10 + "s";
		} else if (age < 3600 * 1000) {
			return "" + age / 1000 / 60 + "min";
		} else if (age < 24 * 3600 * 1000) {
			return "" + age / 3600 / 1000 + "h" + (age / 1000 / 60) % 60 + "min";
		} else {
			return new SimpleDateFormat("MM/dd HH:mm").format(new Date(timestamp));
		}
	}

	@Nullable
	public static <T, U> U mapNonNull(@Nullable T input, @Nonnull Function<T, U> transform) {
		if (input == null) return null;
		return transform.apply(input);
	}

	@Nonnull
	public static <T> T nonNullOr(@Nullable T input, @Nonnull T defaultVal) {
		if (input == null) return defaultVal;
		return input;
	}

	@Nullable
	public static Integer intOrNull(@Nullable String s) {
		if (s == null) return null;
		try {
			return Integer.parseInt(s);
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}
}
