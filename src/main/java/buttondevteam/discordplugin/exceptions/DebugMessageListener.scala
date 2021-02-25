package buttondevteam.discordplugin.exceptions

import buttondevteam.core.ComponentManager
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.lib.TBMCDebugMessageEvent
import discord4j.core.`object`.entity.channel.MessageChannel
import org.bukkit.event.{EventHandler, Listener}

object DebugMessageListener {
    private def SendMessage(message: String): Unit = {
        if (DiscordPlugin.SafeMode || !ComponentManager.isEnabled(classOf[ExceptionListenerModule])) return
        try {
            val mc = ExceptionListenerModule.getChannel
            if (mc == null) return
            val sb = new StringBuilder
            sb.append("```").append("\n")
            sb.append(if (message.length > 2000) message.substring(0, 2000) else message).append("\n")
            sb.append("```")
            mc.flatMap((ch: MessageChannel) => ch.createMessage(sb.toString)).subscribe
        } catch {
            case ex: Exception =>
                ex.printStackTrace()
        }
    }
}

class DebugMessageListener extends Listener {
    @EventHandler def onDebugMessage(e: TBMCDebugMessageEvent): Unit = {
        DebugMessageListener.SendMessage(e.getDebugMessage)
        e.setSent()
    }
}