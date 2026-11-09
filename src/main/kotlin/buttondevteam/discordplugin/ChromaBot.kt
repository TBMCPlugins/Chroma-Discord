package buttondevteam.discordplugin

import buttondevteam.discordplugin.mcchat.MCChatUtils

object ChromaBot {
    private var _enabled = false

    val enabled get() = _enabled

    fun updatePlayerList(): Unit =
        MCChatUtils.updatePlayerList()
}
