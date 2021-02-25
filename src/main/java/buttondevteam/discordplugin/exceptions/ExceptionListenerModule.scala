package buttondevteam.discordplugin.exceptions

import buttondevteam.core.ComponentManager
import buttondevteam.discordplugin.exceptions.ExceptionListenerModule.SendException
import buttondevteam.discordplugin.{DPUtils, DiscordPlugin}
import buttondevteam.lib.architecture.Component
import buttondevteam.lib.{TBMCCoreAPI, TBMCExceptionEvent}
import discord4j.core.`object`.entity.channel.{GuildChannel, MessageChannel}
import discord4j.core.`object`.entity.{Guild, Role}
import org.apache.commons.lang.exception.ExceptionUtils
import org.bukkit.Bukkit
import org.bukkit.event.{EventHandler, Listener}
import reactor.core.publisher.Mono

import java.util.stream.Collectors

/**
 * Listens for errors from the Chroma plugins and posts them to Discord, ignoring repeating errors so it's not that spammy.
 */
object ExceptionListenerModule {
    private def SendException(e: Throwable, sourcemessage: String): Unit = {
        if (instance == null) return
        try getChannel.flatMap((channel: MessageChannel) => {
            def foo(channel: MessageChannel) = {
                var coderRole: Mono[Role] = channel match {
                    case ch: GuildChannel => instance.pingRole(ch.getGuild).get
                    case _ => Mono.empty
                }
                coderRole.map((role: Role) => if (TBMCCoreAPI.IsTestServer) new StringBuilder
                else new StringBuilder(role.getMention).append("\n")).defaultIfEmpty(new StringBuilder).flatMap((sb: StringBuilder) => {
                    def foo(sb: StringBuilder) = {
                        sb.append(sourcemessage).append("\n")
                        sb.append("```").append("\n")
                        var stackTrace = util.Arrays.stream(ExceptionUtils.getStackTrace(e).split("\\n")).filter((s: String) => !s.contains("\tat ") || s.contains("\tat buttondevteam.")).collect(Collectors.joining("\n"))
                        if (sb.length + stackTrace.length >= 1980) stackTrace = stackTrace.substring(0, 1980 - sb.length)
                        sb.append(stackTrace).append("\n")
                        sb.append("```")
                        channel.createMessage(sb.toString)
                    }

                    foo(sb)
                })
            }

            foo(channel)
        }).subscribe
        catch {
            case ex: Exception =>
                ex.printStackTrace()
        }
    }

    private var instance: ExceptionListenerModule = null

    def getChannel: Mono[MessageChannel] = {
        if (instance != null) return instance.channel.get
        Mono.empty
    }
}

class ExceptionListenerModule extends Component[DiscordPlugin] with Listener {
    final private val lastthrown = new util.ArrayList[Throwable]
    final private val lastsourcemsg = new util.ArrayList[String]

    @EventHandler def onException(e: TBMCExceptionEvent): Unit = {
        if (DiscordPlugin.SafeMode || !ComponentManager.isEnabled(getClass)) return
        if (lastthrown.stream.anyMatch((ex: Throwable) => util.Arrays.equals(e.getException.getStackTrace, ex.getStackTrace) && (if (e.getException.getMessage == null) ex.getMessage == null
        else e.getException.getMessage == ex.getMessage)) // e.Exception.Message==ex.Message && lastsourcemsg.contains(e.getSourceMessage))  { return }
            ExceptionListenerModule
        .SendException(e.getException, e.getSourceMessage)
        if (lastthrown.size >= 10) lastthrown.remove(0)
        if (lastsourcemsg.size >= 10) lastsourcemsg.remove(0)
        lastthrown.add(e.getException)
        lastsourcemsg.add(e.getSourceMessage)
        e.setHandled()
    }

    /**
     * The channel to post the errors to.
     */
    final private val channel = DPUtils.channelData(getConfig, "channel")

    /**
     * The role to ping if an error occurs. Set to empty ('') to disable.
     */
    private def pingRole(guild: Mono[Guild]) = DPUtils.roleData(getConfig, "pingRole", "Coder", guild)

    override protected def enable(): Unit = {
        if (DPUtils.disableIfConfigError(this, channel)) return
        ExceptionListenerModule.instance = this
        Bukkit.getPluginManager.registerEvents(new ExceptionListenerModule, getPlugin)
        TBMCCoreAPI.RegisterEventsForExceptions(new DebugMessageListener, getPlugin)
    }

    override protected def disable(): Unit = ExceptionListenerModule.instance = null
}