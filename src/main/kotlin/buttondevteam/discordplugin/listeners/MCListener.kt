package buttondevteam.discordplugin.listeners

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.commands.ConnectCommand
import buttondevteam.discordplugin.mcchat.MinecraftChatModule
import buttondevteam.discordplugin.mcchat.sender.DiscordUser
import buttondevteam.discordplugin.util.DPState
import buttondevteam.lib.ScheduledServerRestartEvent
import buttondevteam.lib.player.TBMCPlayerGetInfoEvent
import discord4j.common.util.Snowflake
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.reactive.asFlow
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import kotlin.jvm.optionals.getOrNull

class MCListener : Listener {
    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        if (ConnectCommand.WaitingToConnect.containsKey(e.player.name)) {
            @SuppressWarnings("ConstantConditions") val user =
                DiscordPlugin.dc.getUserById(Snowflake.of(ConnectCommand.WaitingToConnect[e.player.name]!!)).block()
                    ?: return
            e.player.sendMessage("§bTo connect with the Discord account @" + user.username + "#" + user.discriminator + " do /discord accept")
            e.player.sendMessage("§bIf it wasn't you, do /discord decline")
        }
    }

    @EventHandler
    suspend fun onGetInfo(e: TBMCPlayerGetInfoEvent) {
        if (DiscordPlugin.SafeMode) return
        val dp = e.player?.getAs(DiscordUser::class.java)
        val user = dp?.getDiscordUser() ?: return
        e.addInfo("Discord tag: " + user.username + "#" + user.discriminator)
        val member = user.asMember(DiscordPlugin.plugin!!.mainServer!!.id).asFlow().first()
        val presence = member.presence.asFlow().first()
        e.addInfo(presence.status.toString())
        val activity = presence.activity.getOrNull()
        if (activity != null) {
            e.addInfo("${activity.type}: ${activity.name}")
        }
    }

    @EventHandler //We don't really need this with the logger stuff but hey
    fun onScheduledRestart(e: ScheduledServerRestartEvent) {
        MinecraftChatModule.state = DPState.RESTARTING_SERVER
    }

}