package buttondevteam.discordplugin

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
        val broadcast = new Exception().getStackTrace()(2).getMethodName.contains("broadcast")
        if (broadcast) { //We're catching broadcasts using the Bukkit event
            return
        }
        val sendmsg = DPUtils.sanitizeString(message)
        this synchronized {
            msgtosend += "\n" + sendmsg
            if (sendtask == null) sendtask = Bukkit.getScheduler.runTaskLaterAsynchronously(DiscordPlugin.plugin, () => {
                def foo(): Unit = {
                    channel.createMessage((if (user != null) user.getMention + "\n"
                    else "") + msgtosend.trim).subscribe
                    sendtask = null
                    msgtosend = ""
                }

                foo()
            }, 4) // Waits a 0.2 second to gather all/most of the different messages
        }
    } catch {
        case e: Exception =>
            TBMCCoreAPI.SendException("An error occured while sending message to DiscordSender", e, DiscordPlugin.plugin)
    }

    override def sendMessage(messages: Array[String]): Unit = sendMessage(String.join("\n", messages: _*))
}