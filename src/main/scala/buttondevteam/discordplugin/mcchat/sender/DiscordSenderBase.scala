package buttondevteam.discordplugin.mcchat.sender

import buttondevteam.discordplugin.mcchat.MCChatUtils
import buttondevteam.discordplugin.{DPUtils, DiscordPlugin}
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.player.ChromaGamerBase
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.scheduler.BukkitTask

/**
 *
 * @param user    May be null.
 * @param channel May not be null.
 */
abstract class DiscordSenderBase protected(var user: User, var channel: MessageChannel) extends CommandSender {
    private var msgtosend = ""
    private var sendtask: BukkitTask = null

    /**
     * Returns the user. May be null.
     *
     * @return The user or null.
     */
    def getUser: User = user

    def getChannel: MessageChannel = channel

    private var chromaUser: DiscordPlayer = null

    /**
     * Loads the user data on first query.
     *
     * @return A Chroma user of Discord or a Discord user of Chroma
     */
    def getChromaUser: DiscordPlayer = {
        if (chromaUser == null) chromaUser = ChromaGamerBase.getUser(user.getId.asString, classOf[DiscordPlayer])
        chromaUser
    }

    override def sendMessage(message: String): Unit = try {
        val broadcast = MCChatUtils.broadcastedMessages.contains(message);
        if (broadcast) { //We're catching broadcasts using the Bukkit event
            if (MCChatUtils.broadcastedMessages.size >= 4) { // We really don't need to store messages for long
                MCChatUtils.broadcastedMessages.filterInPlace((_, time) => time > System.nanoTime() - 1000 * 1000 * 1000)
            }
            return ()
        }
        val sendmsg = DPUtils.sanitizeString(message)
        this synchronized {
            msgtosend += "\n" + sendmsg
            if (sendtask == null) sendtask = Bukkit.getScheduler.runTaskLaterAsynchronously(DiscordPlugin.plugin, (() => {
                channel.createMessage((if (user != null) user.getMention + "\n" else "") + msgtosend.trim).subscribe()
                sendtask = null
                msgtosend = ""
            }): Runnable, 4) // Waits a 0.2 second to gather all/most of the different messages
        }
    } catch {
        case e: Exception =>
            TBMCCoreAPI.SendException("An error occured while sending message to DiscordSender", e, DiscordPlugin.plugin)
    }

    override def sendMessage(messages: Array[String]): Unit = sendMessage(String.join("\n", messages: _*))
}