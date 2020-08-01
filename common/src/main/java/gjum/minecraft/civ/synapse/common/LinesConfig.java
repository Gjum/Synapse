package gjum.minecraft.civ.synapse.common;

import javax.annotation.Nullable;
import java.io.*;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class LinesConfig {
	public static long saveLaterTimeout = 300;
	private long lastSaveTime = 0;
	private final ScheduledExecutorService delayedSaveServicePool = Executors.newScheduledThreadPool(1);

	File configFile;

	protected boolean isLoading = false;

	protected abstract Collection<String> getLines();

	protected abstract void setLines(Stream<String> lines);

	public void load(@Nullable File file) {
		configFile = file != null ? file : configFile;
		try {
			try (
					FileReader fReader = new FileReader(configFile);
					BufferedReader bReader = new BufferedReader(fReader)
			) {
				isLoading = true;
				setLines(bReader.lines());
			} finally {
				isLoading = false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveLater(@Nullable File file) {
		configFile = file != null ? file : configFile;
		final long originalSaveRequestTime = System.currentTimeMillis();
		delayedSaveServicePool.schedule(() -> {
			if (lastSaveTime > originalSaveRequestTime) return; // already saved while waiting
			saveNow(configFile);
		}, saveLaterTimeout, TimeUnit.MILLISECONDS);
	}

	public void saveNow(@Nullable File file) {
		configFile = file != null ? file : configFile;
		if (isLoading) throw new ConcurrentModificationException("Cannot save while loading");
		lastSaveTime = System.currentTimeMillis();
		try (FileOutputStream fos = new FileOutputStream(configFile)) {
			fos.write(String.join("\n", getLines()).getBytes(UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
