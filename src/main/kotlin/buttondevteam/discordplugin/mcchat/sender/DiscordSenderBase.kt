package buttondevteam.discordplugin.mcchat.sender

import buttondevteam.discordplugin.DPUtils
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.mcchat.MCChatUtils
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.player.ChromaGamerBase
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.scheduler.BukkitTask

import java.util.UUID

/**
 *
 * @param user    May be null.
 * @param channel May not be null.
 */
abstract class DiscordSenderBase protected constructor(var user: User?, var channel: MessageChannel) :
    CommandSender { // TODO: Move most of this to DiscordUser
    private var msgtosend = ""
    private var sendtask: BukkitTask? = null

    /**
     * Returns the user. May be null.
     *
     * @return The user or null.
     */
    fun getUser(): User? = user

    fun getChannel(): MessageChannel = channel

    private var chromaUser: DiscordUser? = null

    /**
     * Loads the user data on first query.
     *
     * @return A Chroma user of Discord or a Discord user of Chroma
     */
    fun getChromaUser(): DiscordUser {
        if (chromaUser == null) chromaUser = ChromaGamerBase.getUser(user!!.id.asString(), DiscordUser::class.java)
        return chromaUser!!
    }

    override fun sendMessage(message: String): Unit = try {
        val broadcast = MCChatUtils.broadcastedMessages.contains(message);
        if (broadcast) { //We're catching broadcasts using the Bukkit event
            if (MCChatUtils.broadcastedMessages.size >= 4) { // We really don't need to store messages for long
                MCChatUtils.broadcastedMessages.entries.retainAll { (_, time) -> time > System.nanoTime() - 1000 * 1000 * 1000 }
            }
            return
        }
        val sendmsg = DPUtils.sanitizeString(message)
        synchronized(this) {
            msgtosend += "\n" + sendmsg
            if (sendtask == null) sendtask =
                Bukkit.getScheduler().runTaskLaterAsynchronously(DiscordPlugin.plugin!!, Runnable {
                    val user = user
                    channel.createMessage((if (user != null) user.mention + "\n" else "") + msgtosend.trim())
                        .subscribe()
                    sendtask = null
                    msgtosend = ""
                }, 4) // Waits a 0.2 second to gather all/most of the different messages
        }
    } catch (e: Exception) {
        TBMCCoreAPI.SendException("An error occured while sending message to DiscordSender", e, DiscordPlugin.plugin!!)
    }

    override fun sendMessage(vararg messages: String): Unit = sendMessage(messages.joinToString("\n"))

    @Deprecated("Deprecated in Java")
    override fun sendMessage(sender: UUID?, message: String): Unit = sendMessage(message)
    @Deprecated("Deprecated in Java")
    override fun sendMessage(sender: UUID?, vararg messages: String): Unit = sendMessage(*messages)
}