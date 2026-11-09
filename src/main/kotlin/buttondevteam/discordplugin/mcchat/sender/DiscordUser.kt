package buttondevteam.discordplugin.mcchat.sender

import buttondevteam.core.component.channel.Channel
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.mcchat.MCChatPrivate
import buttondevteam.lib.player.{ChromaGamerBase, UserClass}
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User

@UserClass(foldername = "discord") class DiscordUser() extends ChromaGamerBase {
    private var did: String = null
    private var discordUser: User = null

    // private @Getter @Setter boolean minecraftChatEnabled;
    def getDiscordID: String = {
        if (did == null) did = getFileName
        did
    }

    def getDiscordUser: User = {
        if (discordUser == null) discordUser = DiscordPlugin.dc.getUserById(Snowflake.of(getDiscordID)).block() // TODO: Don't do it blocking
        discordUser
    }

    /**
     * Returns true if player has the private Minecraft chat enabled. For setting the value, see
     * [[MCChatPrivate.privateMCChat]]
     */
    def isMinecraftChatEnabled: Boolean = MCChatPrivate.isMinecraftChatEnabled(this)

    override def checkChannelInGroup(s: String): Channel.RecipientTestResult = ???

    override def sendMessage(message: String): Unit = ??? // TODO: Somehow check which message is this a response to

    override def sendMessage(message: Array[String]): Unit = ???

    override def getName: String = ???
}