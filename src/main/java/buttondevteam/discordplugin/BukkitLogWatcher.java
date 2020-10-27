package buttondevteam.discordplugin;

import buttondevteam.discordplugin.mcchat.MinecraftChatModule;
import buttondevteam.discordplugin.util.DPState;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.filter.LevelRangeFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class BukkitLogWatcher extends AbstractAppender {
	protected BukkitLogWatcher() {
		super("ChromaDiscord",
			LevelRangeFilter.createFilter(Level.INFO, Level.INFO, Filter.Result.ACCEPT, Filter.Result.DENY),
			PatternLayout.createDefaultLayout());
	}

	@Override
	public void append(LogEvent logEvent) {
		if (logEvent.getMessage().getFormattedMessage().contains("Attempting to restart with "))
			MinecraftChatModule.state = DPState.RESTARTING_SERVER;
	}
}
