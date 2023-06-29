package buttondevteam.discordplugin.mcchat

import buttondevteam.core.ComponentManager
import buttondevteam.discordplugin.mcchat.MCChatUtils.LastMsgData
import buttondevteam.discordplugin.mcchat.sender.{DiscordConnectedPlayer, DiscordPlayer, DiscordSenderBase}
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.lib.player.TBMCPlayer
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.{MessageChannel, PrivateChannel}
import org.bukkit.Bukkit

import scala.collection.mutable.ListBuffer
import scala.jdk.javaapi.CollectionConverters.asScala

object MCChatPrivate {
    /**
     * Used for messages in PMs (mcchat).
     */
    private[mcchat] var lastmsgPerUser: ListBuffer[LastMsgData] = ListBuffer()

    def privateMCChat(channel: MessageChannel, start: Boolean, user: User, dp: DiscordPlayer): Unit = {
        MCChatUtils.ConnectedSenders synchronized {
            val mcp = dp.getAs(classOf[TBMCPlayer])
            if (mcp != null) { // If the accounts aren't connected, can't make a connected sender
                val p = Bukkit.getPlayer(mcp.getUniqueId)
                val op = Bukkit.getOfflinePlayer(mcp.getUniqueId)
                val mcm = ComponentManager.getIfEnabled(classOf[MinecraftChatModule])
                if (start) {
                    val sender = DiscordConnectedPlayer.create(user, channel, mcp.getUniqueId, op.getName, mcm)
                    MCChatUtils.addSenderTo(MCChatUtils.ConnectedSenders, user, sender)
                    MCChatUtils.LoggedInPlayers.put(mcp.getUniqueId, sender)
                    if (p == null) { // Player is offline - If the player is online, that takes precedence
                        MCChatUtils.callLoginEvents(sender)
                    }
                }
                else {
                    val sender = MCChatUtils.removeSenderFrom(MCChatUtils.ConnectedSenders, channel.getId, user)
                    assert(sender != null)
                    Bukkit.getScheduler.runTask(DiscordPlugin.plugin, () => {
                        def foo(): Unit = {
                            if ((p == null || p.isInstanceOf[DiscordSenderBase]) // Player is offline - If the player is online, that takes precedence
                                && sender.isLoggedIn) { //Don't call the quit event if login failed
                                MCChatUtils.callLogoutEvent(sender, needsSync = false) //The next line has to run *after* this one, so can't use the needsSync parameter
                            }

                            MCChatUtils.LoggedInPlayers.remove(sender.getUniqueId)
                            sender.setLoggedIn(false)
                        }

                        foo()
                    }
                    )
                }
                // ---- PermissionsEx warning is normal on logout ----
            }
            if (!start) MCChatUtils.lastmsgfromd.remove(channel.getId.asLong)
            if (start) lastmsgPerUser += new MCChatUtils.LastMsgData(channel, user) // Doesn't support group DMs
            else lastmsgPerUser.filterInPlace(_.channel.getId.asLong != channel.getId.asLong) //Remove
        }
    }

    def isMinecraftChatEnabled(dp: DiscordPlayer): Boolean = isMinecraftChatEnabled(dp.getDiscordID)

    def isMinecraftChatEnabled(did: String): Boolean = { // Don't load the player data just for this
        lastmsgPerUser.exists(_.channel.asInstanceOf[PrivateChannel].getRecipientIds.stream.anyMatch(u => u.asString == did))
    }

    def logoutAll(): Unit = {
        MCChatUtils.ConnectedSenders synchronized {
            for ((_, userMap) <- MCChatUtils.ConnectedSenders) {
                for (valueEntry <- asScala(userMap.entrySet)) {
                    if (MCChatUtils.getSenderFrom(MCChatUtils.OnlineSenders, valueEntry.getKey, valueEntry.getValue.getUser) == null) { //If the player is online then the fake player was already logged out
                        MCChatUtils.callLogoutEvent(valueEntry.getValue, !Bukkit.isPrimaryThread)
                    }
                }
            }
        }
    }
}