package buttondevteam.discordplugin

import buttondevteam.discordplugin.mcchat.MinecraftChatModule
import buttondevteam.discordplugin.util.DPState
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.filter.LevelRangeFilter
import org.apache.logging.log4j.core.layout.PatternLayout
import org.apache.logging.log4j.core.{Filter, LogEvent}

class BukkitLogWatcher private[discordplugin]() extends AbstractAppender("ChromaDiscord",
    LevelRangeFilter.createFilter(Level.INFO, Level.INFO, Filter.Result.ACCEPT, Filter.Result.DENY),
    PatternLayout.createDefaultLayout) {
    override def append(logEvent: LogEvent): Unit =
        if (logEvent.getMessage.getFormattedMessage.contains("Attempting to restart with "))
            MinecraftChatModule.state = DPState.RESTARTING_SERVER
}