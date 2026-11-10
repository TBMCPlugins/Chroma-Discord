package buttondevteam.discordplugin.mcchat.sender

import buttondevteam.core.component.channel.Channel
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.lib.player.ChromaGamerBase
import buttondevteam.lib.player.UserClass
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.reactive.asFlow

@UserClass(foldername = "discord")
class DiscordUser : ChromaGamerBase() {
    private var discordUser: User? = null

    val discordID get() = fileName

    suspend fun getDiscordUser(): User {
        val user = discordUser ?: DiscordPlugin.dc.getUserById(Snowflake.of(discordID)).asFlow().first()
        discordUser = user
        return user
    }

    /**
     * Returns true if player has the private Minecraft chat enabled. For setting the value, see
     * [[MCChatPrivate.privateMCChat]]
     */
    fun isMinecraftChatEnabled(): Boolean = MCChatPrivate.isMinecraftChatEnabled(this)

    override fun checkChannelInGroup(group: String?): Channel.RecipientTestResult = TODO()

    override fun sendMessage(message: String): Unit = TODO() // TODO: Somehow check which message is this a response to

    override fun sendMessage(message: Array<String>): Unit = TODO()

    override fun getName(): String = TODO()
}