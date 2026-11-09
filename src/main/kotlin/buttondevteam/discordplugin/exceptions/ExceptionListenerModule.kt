package buttondevteam.discordplugin.exceptions

import buttondevteam.core.ComponentManager
import buttondevteam.lib.architecture.Component
import discord4j.core.`object`.entity.Guild
import org.apache.commons.lang.exception.ExceptionUtils
import org.bukkit.Bukkit
import reactor.core.scala.publisher.SMono

import java.util
import java.util.stream.Collectors

/**
 * Listens for errors from the Chroma plugins and posts them to Discord, ignoring repeating errors so it's not that spammy.
 */
object ExceptionListenerModule {
    private fun SendException(e: Throwable, sourcemessage: String): Unit = {
        if (instance == null) return Unit
        try getChannel.flatMap(channel -> {
            val coderRole = channel match {
                case ch: GuildChannel -> instance.pingRole(ch.getGuild.^^()).get
                case _ -> SMono.empty
            }
            coderRole.map(role -> if (TBMCCoreAPI.IsTestServer) StringBuilder else StringBuilder(role.getMention).append("\n")) // Ping if prod server
                .defaultIfEmpty(StringBuilder).flatMap(sb -> {
                sb.append(sourcemessage).append("\n")
                sb.append("```").append("\n")
                var stackTrace = util.Arrays.stream(ExceptionUtils.getStackTrace(e).split("\\n"))
                    .filter(s -> !s.contains("\tat ") || s.contains("buttondevteam."))
                    .collect(Collectors.joining("\n"))
                if (sb.length + stackTrace.length >= 1980) stackTrace = stackTrace.substring(0, 1980 - sb.length)
                sb.append(stackTrace).append("\n")
                sb.append("```")
                channel.createMessage(sb.toString).^^()
            })
        }).subscribe()
        catch {
            case ex: Exception ->
                ex.printStackTrace()
        }
    }

    private var instance: ExceptionListenerModule = null

    fun getChannel(): SMono[MessageChannel] = {
        if (instance != null) return instance.channel.get
        SMono.empty
    }
}

class ExceptionListenerModule : Component<DiscordPlugin> with Listener {
    final private val lastthrown = util.ArrayList[Throwable]
    final private val lastsourcemsg = util.ArrayList[String]

    @EventHandler fun onException(e: TBMCExceptionEvent): Unit = {
        if (DiscordPlugin.SafeMode || !ComponentManager.isEnabled(getClass)) return Unit
        if (lastthrown.stream.anyMatch(ex -> e.getException.getStackTrace.sameElements(ex.getStackTrace)
            && (if (e.getException.getMessage == null) ex.getMessage == null else e.getException.getMessage == ex.getMessage))
            && lastsourcemsg.contains(e.getSourceMessage)) {
            return Unit
        }
        ExceptionListenerModule.SendException(e.getException, e.getSourceMessage)
        if (lastthrown.size >= 10) lastthrown.remove(0)
        if (lastsourcemsg.size >= 10) lastsourcemsg.remove(0)
        lastthrown.add(e.getException)
        lastsourcemsg.add(e.getSourceMessage)
        e.setHandled(true)
    }

    /**
     * The channel to post the errors to.
     */
    final private val channel = DPUtils.channelData(getConfig, "channel")

    /**
     * The role to ping if an error occurs. Set to empty ('') to disable.
     */
    private val pingRole(guild: SMono[Guild]) = DPUtils.roleData(getConfig, "pingRole", "Coder", guild)

    override protected fun enable(): Unit = {
        if (DPUtils.disableIfConfigError(this, channel)) return Unit
        ExceptionListenerModule.instance = this
        Bukkit.getPluginManager.registerEvents(ExceptionListenerModule, getPlugin)
        TBMCCoreAPI.RegisterEventsForExceptions(DebugMessageListener, getPlugin)
    }

    override protected fun disable(): Unit = ExceptionListenerModule.instance = null
}