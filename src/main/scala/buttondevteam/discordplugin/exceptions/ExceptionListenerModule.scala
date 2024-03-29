package buttondevteam.discordplugin.exceptions

import buttondevteam.core.ComponentManager
import buttondevteam.discordplugin.DPUtils.MonoExtensions
import buttondevteam.discordplugin.{DPUtils, DiscordPlugin}
import buttondevteam.lib.architecture.Component
import buttondevteam.lib.{TBMCCoreAPI, TBMCExceptionEvent}
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.channel.{GuildChannel, MessageChannel}
import org.apache.commons.lang.exception.ExceptionUtils
import org.bukkit.Bukkit
import org.bukkit.event.{EventHandler, Listener}
import reactor.core.scala.publisher.SMono

import java.util
import java.util.stream.Collectors

/**
 * Listens for errors from the Chroma plugins and posts them to Discord, ignoring repeating errors so it's not that spammy.
 */
object ExceptionListenerModule {
    private def SendException(e: Throwable, sourcemessage: String): Unit = {
        if (instance == null) return ()
        try getChannel.flatMap(channel => {
            val coderRole = channel match {
                case ch: GuildChannel => instance.pingRole(ch.getGuild.^^()).get
                case _ => SMono.empty
            }
            coderRole.map(role => if (TBMCCoreAPI.IsTestServer) new StringBuilder else new StringBuilder(role.getMention).append("\n")) // Ping if prod server
                .defaultIfEmpty(new StringBuilder).flatMap(sb => {
                sb.append(sourcemessage).append("\n")
                sb.append("```").append("\n")
                var stackTrace = util.Arrays.stream(ExceptionUtils.getStackTrace(e).split("\\n"))
                    .filter(s => !s.contains("\tat ") || s.contains("buttondevteam."))
                    .collect(Collectors.joining("\n"))
                if (sb.length + stackTrace.length >= 1980) stackTrace = stackTrace.substring(0, 1980 - sb.length)
                sb.append(stackTrace).append("\n")
                sb.append("```")
                channel.createMessage(sb.toString).^^()
            })
        }).subscribe()
        catch {
            case ex: Exception =>
                ex.printStackTrace()
        }
    }

    private var instance: ExceptionListenerModule = null

    def getChannel: SMono[MessageChannel] = {
        if (instance != null) return instance.channel.get
        SMono.empty
    }
}

class ExceptionListenerModule extends Component[DiscordPlugin] with Listener {
    final private val lastthrown = new util.ArrayList[Throwable]
    final private val lastsourcemsg = new util.ArrayList[String]

    @EventHandler def onException(e: TBMCExceptionEvent): Unit = {
        if (DiscordPlugin.SafeMode || !ComponentManager.isEnabled(getClass)) return ()
        if (lastthrown.stream.anyMatch(ex => e.getException.getStackTrace.sameElements(ex.getStackTrace)
            && (if (e.getException.getMessage == null) ex.getMessage == null else e.getException.getMessage == ex.getMessage))
            && lastsourcemsg.contains(e.getSourceMessage)) {
            return ()
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
    final private def channel = DPUtils.channelData(getConfig, "channel")

    /**
     * The role to ping if an error occurs. Set to empty ('') to disable.
     */
    private def pingRole(guild: SMono[Guild]) = DPUtils.roleData(getConfig, "pingRole", "Coder", guild)

    override protected def enable(): Unit = {
        if (DPUtils.disableIfConfigError(this, channel)) return ()
        ExceptionListenerModule.instance = this
        Bukkit.getPluginManager.registerEvents(new ExceptionListenerModule, getPlugin)
        TBMCCoreAPI.RegisterEventsForExceptions(new DebugMessageListener, getPlugin)
    }

    override protected def disable(): Unit = ExceptionListenerModule.instance = null
}