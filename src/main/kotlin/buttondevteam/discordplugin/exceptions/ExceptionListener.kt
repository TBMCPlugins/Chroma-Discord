package buttondevteam.discordplugin.exceptions

import buttondevteam.core.ComponentManager
import buttondevteam.discordplugin.DiscordChannelData
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.DiscordRoleData
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.TBMCExceptionEvent
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.apache.commons.lang3.exception.ExceptionUtils
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.*

/**
 * Listens for errors from the Chroma plugins and posts them to Discord, ignoring repeating errors so it's not that spammy.
 */
class ExceptionListener(val channel: DiscordChannelData, val pingRole: DiscordRoleData) : Listener {
    private val lastthrown = mutableListOf<Throwable>()
    private val lastsourcemsg = mutableListOf<String>()

    private suspend fun sendException(e: Throwable, sourcemessage: String) {
        val channel = channel.getEntity() ?: return
        val coderRole = pingRole.getEntity()
        val sb =
            if (TBMCCoreAPI.IsTestServer() || coderRole == null)
                StringBuilder()
            else
                StringBuilder(coderRole.mention)

        sb.append(sourcemessage).append("\n")
        sb.append("```").append("\n")
        var stackTrace = ExceptionUtils.getStackTrace(e).split("\\n")
            .filter { s -> !s.contains("\tat ") || s.contains("buttondevteam.") }
            .joinToString("\n")
        if (sb.length + stackTrace.length >= 1980) stackTrace = stackTrace.substring(0, 1980 - sb.length)
        sb.append(stackTrace).append("\n")
        sb.append("```")
        channel.createMessage(sb.toString()).awaitFirstOrNull()
    }

    @EventHandler
    suspend fun onException(e: TBMCExceptionEvent) {
        if (DiscordPlugin.SafeMode || !ComponentManager.isEnabled(ExceptionListenerModule::class.java)) return
        if (lastthrown.any { ex ->
                e.exception.stackTrace.contentEquals(ex.stackTrace)
                    && Objects.equals(e.exception.message, ex.message)
            }
            && lastsourcemsg.contains(e.sourceMessage)) {
            return
        }
        sendException(e.exception, e.sourceMessage)
        if (lastthrown.size >= 10) lastthrown.removeAt(0)
        if (lastsourcemsg.size >= 10) lastsourcemsg.removeAt(0)
        lastthrown.add(e.exception)
        lastsourcemsg.add(e.sourceMessage)
        e.isHandled = true
    }
}