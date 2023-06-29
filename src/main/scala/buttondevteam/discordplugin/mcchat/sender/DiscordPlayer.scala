package buttondevteam.discordplugin.mcchat.sender

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.mcchat.MCChatPrivate
import buttondevteam.lib.player.{ChromaGamerBase, UserClass}
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User

@UserClass(foldername = "discord") class DiscordPlayer() extends ChromaGamerBase {
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
}