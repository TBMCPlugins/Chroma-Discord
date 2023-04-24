package buttondevteam.discordplugin.mcchat

import buttondevteam.discordplugin.*
import buttondevteam.discordplugin.mcchat.sender.{DiscordConnectedPlayer, DiscordPlayer, DiscordPlayerSender}
import buttondevteam.lib.TBMCSystemChatEvent
import buttondevteam.lib.player.{TBMCPlayer, TBMCPlayerBase, TBMCYEEHAWEvent}
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Role
import discord4j.core.`object`.entity.channel.MessageChannel
import net.ess3.api.events.{AfkStatusChangeEvent, MuteStatusChangeEvent, NickChangeEvent, VanishStatusChangeEvent}
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import org.bukkit.event.player.PlayerLoginEvent.Result
import org.bukkit.event.server.{BroadcastMessageEvent, TabCompleteEvent}
import org.bukkit.event.{EventHandler, EventPriority, Listener}
import reactor.core.publisher.{Flux, Mono}

class MCListener(val module: MinecraftChatModule) extends Listener {
    final private val muteRole = DPUtils.roleData(module.getConfig, "muteRole", "Muted")

    @EventHandler(priority = EventPriority.HIGHEST) def onPlayerLogin(e: PlayerLoginEvent): Unit = {
        if (e.getResult ne Result.ALLOWED) return ()
        if (e.getPlayer.isInstanceOf[DiscordConnectedPlayer]) return ()
        val dcp = MCChatUtils.LoggedInPlayers.get(e.getPlayer.getUniqueId)
        if (dcp.nonEmpty) MCChatUtils.callLogoutEvent(dcp.get, needsSync = false)
    }

    @EventHandler(priority = EventPriority.MONITOR) def onPlayerJoin(e: PlayerJoinEvent): Unit = {
        if (e.getPlayer.isInstanceOf[DiscordConnectedPlayer]) return () // Don't show the joined message for the fake player
        Bukkit.getScheduler.runTaskAsynchronously(DiscordPlugin.plugin, () => {
            def foo(): Unit = {
                val p = e.getPlayer
                val dp = TBMCPlayerBase.getPlayer(p.getUniqueId, classOf[TBMCPlayer]).getAs(classOf[DiscordPlayer])
                if (dp != null)
                    DiscordPlugin.dc.getUserById(Snowflake.of(dp.getDiscordID)).flatMap(user =>
                        user.getPrivateChannel.flatMap(chan => module.chatChannelMono.flatMap(cc => {
                            MCChatUtils.addSender(MCChatUtils.OnlineSenders, dp.getDiscordID, DiscordPlayerSender.create(user, chan, p, module))
                            MCChatUtils.addSender(MCChatUtils.OnlineSenders, dp.getDiscordID, DiscordPlayerSender.create(user, cc, p, module)) //Stored per-channel
                            Mono.empty
                        }))).subscribe()
                val message = e.getJoinMessage
                sendJoinLeaveMessage(message, e.getPlayer)
                ChromaBot.updatePlayerList()
            }

            foo()
        })
    }

    private def sendJoinLeaveMessage(message: String, player: Player): Unit =
        if (message != null && message.trim.nonEmpty)
            MCChatUtils.forAllowedCustomAndAllMCChat(MCChatUtils.send(message), player, ChannelconBroadcast.JOINLEAVE, hookmsg = true).subscribe()

    @EventHandler(priority = EventPriority.MONITOR) def onPlayerLeave(e: PlayerQuitEvent): Unit = {
        if (e.getPlayer.isInstanceOf[DiscordConnectedPlayer]) return () // Only care about real users
        MCChatUtils.OnlineSenders.filterInPlace((_, userMap) => userMap.entrySet.stream.noneMatch(_.getValue.getUniqueId.equals(e.getPlayer.getUniqueId)))
        Bukkit.getScheduler.runTaskAsynchronously(DiscordPlugin.plugin, () => MCChatUtils.LoggedInPlayers.get(e.getPlayer.getUniqueId).foreach(MCChatUtils.callLoginEvents))
        Bukkit.getScheduler.runTaskLaterAsynchronously(DiscordPlugin.plugin, () => ChromaBot.updatePlayerList(), 5)
        val message = e.getQuitMessage
        sendJoinLeaveMessage(message, e.getPlayer)
    }

    @EventHandler(priority = EventPriority.HIGHEST) def onPlayerKick(e: PlayerKickEvent): Unit = {
        /*if (!DiscordPlugin.hooked && !e.getReason().equals("The server is restarting")
                        && !e.getReason().equals("Server closed")) // The leave messages errored with the previous setup, I could make it wait since I moved it here, but instead I have a special
                    MCChatListener.forAllowedCustomAndAllMCChat(e.getPlayer().getName() + " left the game"); // message for this - Oh wait this doesn't even send normally because of the hook*/
    }

    @EventHandler(priority = EventPriority.LOW) def onPlayerDeath(e: PlayerDeathEvent): Unit =
        MCChatUtils.forAllowedCustomAndAllMCChat(MCChatUtils.send(e.getDeathMessage), e.getEntity, ChannelconBroadcast.DEATH, hookmsg = true).subscribe()

    @EventHandler def onPlayerAFK(e: AfkStatusChangeEvent): Unit = {
        val base = e.getAffected.getBase
        if (e.isCancelled || !base.isOnline) return ()
        val msg = base.getDisplayName + " is " + (if (e.getValue) "now"
        else "no longer") + " AFK."
        MCChatUtils.forAllowedCustomAndAllMCChat(MCChatUtils.send(msg), base, ChannelconBroadcast.AFK, hookmsg = false).subscribe()
    }

    @EventHandler def onPlayerMute(e: MuteStatusChangeEvent): Unit = {
        val role = muteRole.get
        if (role == null) return ()
        val source = e.getAffected.getSource
        if (!source.isPlayer) return ()
        val p = TBMCPlayerBase.getPlayer(source.getPlayer.getUniqueId, classOf[TBMCPlayer]).getAs(classOf[DiscordPlayer])
        if (p == null) return ()
        DPUtils.ignoreError(DiscordPlugin.dc.getUserById(Snowflake.of(p.getDiscordID))
            .flatMap(user => user.asMember(DiscordPlugin.mainServer.getId))
            .flatMap(user => role.flatMap((r: Role) => {
                def foo(r: Role): Mono[_] = {
                    if (e.getValue) user.addRole(r.getId)
                    else user.removeRole(r.getId)
                    val modlog = module.modlogChannel.get
                    val msg = s"${(if (e.getValue) "M" else "Unm")}uted user: ${user.getUsername}#${user.getDiscriminator}"
                    module.log(msg)
                    if (modlog != null) return modlog.flatMap((ch: MessageChannel) => ch.createMessage(msg))
                    Mono.empty
                }

                foo(r)
            }))).subscribe()
    }

    @EventHandler def onChatSystemMessage(event: TBMCSystemChatEvent): Unit =
        MCChatUtils.forAllowedMCChat(MCChatUtils.send(event.getMessage), event).subscribe()

    @EventHandler def onBroadcastMessage(event: BroadcastMessageEvent): Unit = {
        MCChatUtils.broadcastedMessages += ((event.getMessage, System.nanoTime()))
        MCChatUtils.forCustomAndAllMCChat(MCChatUtils.send(event.getMessage), ChannelconBroadcast.BROADCAST, hookmsg = false).subscribe()
    }

    @EventHandler def onYEEHAW(event: TBMCYEEHAWEvent): Unit = { //TODO: Inherit from the chat event base to have channel support
        val name = event.getSender match {
            case player: Player => player.getDisplayName
            case _ => event.getSender.getName
        }
        //Channel channel = ChromaGamerBase.getFromSender(event.getSender()).channel().get(); - TODO
        DiscordPlugin.mainServer.getEmojis.filter(e => "YEEHAW" == e.getName).take(1).singleOrEmpty
            .map(Option.apply).defaultIfEmpty(Option.empty)
            .flatMap(yeehaw => MCChatUtils.forPublicPrivateChat(MCChatUtils.send(name +
                yeehaw.map(guildEmoji => " <:YEEHAW:" + guildEmoji.getId.asString + ">s").getOrElse(" YEEHAWs")))).subscribe()
    }

    @EventHandler def onNickChange(event: NickChangeEvent): Unit = MCChatUtils.updatePlayerList()

    @EventHandler def onTabComplete(event: TabCompleteEvent): Unit = {
        val i = event.getBuffer.lastIndexOf(' ')
        val t = event.getBuffer.substring(i + 1) //0 if not found
        if (!t.startsWith("@")) return ()
        val token = t.substring(1)
        val x = DiscordPlugin.mainServer.getMembers.flatMap(m => Flux.just(m.getUsername, m.getNickname.orElse("")))
            .filter(_.startsWith(token)).map("@" + _).doOnNext(event.getCompletions.add(_)).blockLast()
    }

    @EventHandler def onVanish(event: VanishStatusChangeEvent): Unit = {
        if (event.isCancelled) return ()
        Bukkit.getScheduler.runTask(DiscordPlugin.plugin, () => MCChatUtils.updatePlayerList())
    }
}