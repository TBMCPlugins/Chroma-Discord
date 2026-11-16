package buttondevteam.discordplugin.exceptions

import buttondevteam.discordplugin.DPUtils
import buttondevteam.discordplugin.DPUtils.channelData
import buttondevteam.discordplugin.DPUtils.roleData
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.lib.architecture.Component
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import discord4j.core.`object`.entity.channel.GuildChannel
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bukkit.Bukkit
import org.bukkit.event.Listener

/**
 * Listens for errors from the Chroma plugins and posts them to Discord, ignoring repeating errors so it's not that spammy.
 */
class ExceptionListenerModule : Component<DiscordPlugin>(), Listener {
    companion object {

        private var instance: ExceptionListenerModule? = null
    }

    /**
     * The channel to post the errors to.
     */
    private val channel = config.channelData("channel")

    /**
     * The role to ping if an error occurs. Set to empty ('') to disable.
     */
    private val pingRole = config.roleData("pingRole", suspend {
        val channel = channel.getEntity()
        if (channel is GuildChannel) channel.guild.awaitFirstOrNull()
        else null
    })

    override fun enable() {
        plugin.launch {
            enableAsync()
        }
    }

    private suspend fun enableAsync() { // TODO: Suspending components
        if (DPUtils.disableIfConfigError(this, channel)) return
        instance = this
        Bukkit.getPluginManager().registerSuspendingEvents(ExceptionListener(channel, pingRole), plugin)
        Bukkit.getPluginManager().registerSuspendingEvents(DebugMessageListener(channel), plugin) // TODO: Register for exceptions (not the one above)
    }

    override fun disable() { instance = null }
}