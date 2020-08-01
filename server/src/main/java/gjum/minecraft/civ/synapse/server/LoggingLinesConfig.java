package gjum.minecraft.civ.synapse.server;

import gjum.minecraft.civ.synapse.common.LinesConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;

public abstract class LoggingLinesConfig extends LinesConfig {
	protected static final Logger logger = LoggerFactory.getLogger("Config");

	@Override
	public void saveNow(@Nullable File file) {
		logger.info("Saving " + this.getClass().getSimpleName() + " to " + file);
		super.saveNow(file);
	}
}
