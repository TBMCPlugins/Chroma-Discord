package buttondevteam.discordplugin

import buttondevteam.discordplugin.mcchat.MCChatPrivate
import buttondevteam.lib.player.{ChromaGamerBase, UserClass}

@UserClass(foldername = "discord") class DiscordPlayer() extends ChromaGamerBase {
    private var did: String = null

    // private @Getter @Setter boolean minecraftChatEnabled;
    def getDiscordID: String = {
        if (did == null) did = getFileName
        did
    }

    /**
     * Returns true if player has the private Minecraft chat enabled. For setting the value, see
     * [[MCChatPrivate.privateMCChat]]
     */
    def isMinecraftChatEnabled: Boolean = MCChatPrivate.isMinecraftChatEnabled(this)
}