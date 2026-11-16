package buttondevteam.discordplugin.exceptions

import buttondevteam.core.ComponentManager
import buttondevteam.discordplugin.DiscordChannelData
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.lib.TBMCDebugMessageEvent
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class DebugMessageListener(val channel: DiscordChannelData) : Listener {
    private suspend fun sendMessage(message: String) {
        if (DiscordPlugin.SafeMode || !ComponentManager.isEnabled(ExceptionListenerModule::class.java)) return
        val mc = channel.getEntity() ?: return
        val sb = StringBuilder()
        sb.append("```").append("\n")
        sb.append(if (message.length > 2000) message.substring(0, 2000) else message).append("\n")
        sb.append("```")
        mc.createMessage(sb.toString()).awaitFirstOrNull()
    }

    @EventHandler
    suspend fun onDebugMessage(e: TBMCDebugMessageEvent) {
        sendMessage(e.debugMessage)
        e.setSent()
    }
}