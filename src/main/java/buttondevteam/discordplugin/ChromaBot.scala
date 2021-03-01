package buttondevteam.discordplugin

import buttondevteam.discordplugin.ChannelconBroadcast.ChannelconBroadcast
import buttondevteam.discordplugin.mcchat.MCChatUtils
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import reactor.core.publisher.Mono

import javax.annotation.Nullable

object ChromaBot {
    private var _enabled = false

    def enabled = _enabled

    private[discordplugin] def enabled_=(en: Boolean): Unit = _enabled = en

    /**
     * Send a message to the chat channels and private chats.
     *
     * @param message The message to send, duh (use {@link MessageChannel# createMessage ( String )})
     */
    def sendMessage(message: java.util.function.Function[Mono[MessageChannel], Mono[Message]]): Unit =
        MCChatUtils.forPublicPrivateChat(message.apply(_)).subscribe

    /**
     * Send a message to the chat channels, private chats and custom chats.
     *
     * @param message The message to send, duh
     * @param toggle  The toggle type for channelcon
     */
    def sendMessageCustomAsWell(message: Function[Mono[MessageChannel], Mono[Message]], @Nullable toggle: ChannelconBroadcast): Unit =
        MCChatUtils.forCustomAndAllMCChat(message.apply, toggle, false).subscribe

    def updatePlayerList(): Unit =
        MCChatUtils.updatePlayerList()
}
