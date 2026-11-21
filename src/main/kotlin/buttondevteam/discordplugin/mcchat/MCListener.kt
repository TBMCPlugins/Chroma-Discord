package buttondevteam.discordplugin.mcchat

import buttondevteam.discordplugin.ChannelconBroadcast
import buttondevteam.discordplugin.DPUtils.asString
import buttondevteam.discordplugin.DPUtils.roleData
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.mcchat.sender.DiscordUser
import buttondevteam.lib.TBMCSystemChatEvent
import buttondevteam.lib.player.ChromaGamerBase
import buttondevteam.lib.player.TBMCPlayer
import buttondevteam.lib.player.TBMCPlayerBase
import buttondevteam.lib.player.TBMCYEEHAWEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import net.ess3.api.events.AfkStatusChangeEvent
import net.ess3.api.events.MuteStatusChangeEvent
import net.ess3.api.events.NickChangeEvent
import net.ess3.api.events.VanishStatusChangeEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.BroadcastMessageEvent
import org.bukkit.event.server.TabCompleteEvent
import reactor.core.publisher.Flux
import kotlin.time.Duration.Companion.milliseconds

class MCListener(val module: MinecraftChatModule) : Listener {
    private val muteRole = module.config.roleData("muteRole")

    @EventHandler(priority = EventPriority.MONITOR)
    suspend fun onPlayerJoin(e: PlayerJoinEvent) {
        val message = e.joinMessage()?.asString()
        sendJoinLeaveMessage(message, e.player)
        MCChatUtils.updatePlayerList()
    }

    private suspend fun sendJoinLeaveMessage(message: String?, player: Player) {
        if (message != null && message.trim().isNotEmpty())
            MCChatUtils.forAllowedCustomAndAllMCChat(
                MCChatUtils.send(message), ChromaGamerBase.getFromSender(player),
                ChannelconBroadcast.JOINLEAVE, hookmsg = true
            )
    }

    @EventHandler(priority = EventPriority.MONITOR)
    suspend fun onPlayerLeave(e: PlayerQuitEvent) {
        delay(5000.milliseconds)
        MCChatUtils.updatePlayerList()
        val message = e.quitMessage()?.asString()
        sendJoinLeaveMessage(message, e.player)
    }

    @EventHandler(priority = EventPriority.LOW)
    suspend fun onPlayerDeath(e: PlayerDeathEvent) =
        e.deathMessage()?.let {
            MCChatUtils.forAllowedCustomAndAllMCChat(
                MCChatUtils.send(it.asString()),
                ChromaGamerBase.getFromSender(e.entity),
                ChannelconBroadcast.DEATH,
                hookmsg = true
            )
        }

    @EventHandler
    suspend fun onPlayerAFK(e: AfkStatusChangeEvent) { // TODO: Requires Essentials, the mcchat module fails without it
        val base = e.affected.base
        if (e.isCancelled || !base.isOnline) return
        val msg = base.displayName().asString() + " is " + (if (e.value) "now" else "no longer") + " AFK."
        MCChatUtils.forAllowedCustomAndAllMCChat(
            MCChatUtils.send(msg),
            ChromaGamerBase.getFromSender(base),
            ChannelconBroadcast.AFK,
            hookmsg = false
        )
    }

    @EventHandler
    suspend fun onPlayerMute(e: MuteStatusChangeEvent) {
        val role = muteRole.getEntity() ?: return
        val source = e.affected.source
        if (!source.isPlayer) return
        val p = TBMCPlayerBase.getPlayer(source.player.uniqueId, TBMCPlayer::class.java).getAs(DiscordUser::class.java)
            ?: return
        val user = p.getDiscordUser()
            .asMember(DiscordPlugin.plugin.mainServer.id) // TODO: Utility to get as member & error handling
            .awaitFirst()
        if (e.value) user.addRole(role.id).awaitFirst()
        else user.removeRole(role.id)
        val modlog = module.modlogChannel.getEntity()
        val msg = "${(if (e.value) "M" else "Unm")}uted user: ${user.username}"
        module.log(msg)
        modlog?.createMessage(msg)?.awaitSingle()
    }

    @EventHandler
    suspend fun onChatSystemMessage(event: TBMCSystemChatEvent) =
        MCChatUtils.forAllowedMCChat(MCChatUtils.send(event.message.asString()), event)

    @EventHandler
    suspend fun onBroadcastMessage(event: BroadcastMessageEvent) {
        val message = event.message().asString()
        MCChatUtils.broadcastedMessages[message] = System.nanoTime()
        MCChatUtils.forCustomAndAllMCChat(MCChatUtils.send(message), ChannelconBroadcast.BROADCAST, hookmsg = false)
    }

    @EventHandler
    suspend fun onYEEHAW(event: TBMCYEEHAWEvent) { //TODO: Implement as a colored system message to have channel support
        val name = when (val sender = event.sender) {
            is Player -> sender.displayName().asString()
            else -> event.sender.name
        }
        //Channel channel = ChromaGamerBase.getFromSender(event.getSender()).channel().get(); - TODO
        val emoji = DiscordPlugin.plugin.mainServer.emojis.filter { e -> "YEEHAW" == e.name }.awaitFirstOrNull()
        MCChatUtils.forPublicPrivateChat(MCChatUtils.send(name + (emoji?.let { " <:YEEHAW:" + emoji.id.asString() + ">" }
            ?: " YEEHAWs")))
    }

    @EventHandler
    fun onNickChange(event: NickChangeEvent) = MCChatUtils.updatePlayerList()

    @EventHandler
    suspend fun onTabComplete(event: TabCompleteEvent) {
        val i = event.buffer.lastIndexOf(' ')
        val t = event.buffer.substring(i + 1) //0 if not found
        if (!t.startsWith("@")) return
        val token = t.substring(1)
        DiscordPlugin.plugin.mainServer.members.flatMap { m -> Flux.just(m.username, m.nickname.orElse("")) }
            .filter { it.startsWith(token) }.map { "@$it" }.asFlow().collect { event.completions.add(it) }
    }

    @EventHandler
    suspend fun onVanish(event: VanishStatusChangeEvent) {
        if (event.isCancelled) return
        delay(1.milliseconds)
        MCChatUtils.updatePlayerList()
    }
}