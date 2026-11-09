package buttondevteam.discordplugin.mcchat.sender

import buttondevteam.discordplugin.mcchat.MinecraftChatModule
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import org.bukkit.entity.Player
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock

import java.lang.reflect.Modifier

object DiscordPlayerSender {
    fun create(user: User, channel: MessageChannel, player: Player, module: MinecraftChatModule): DiscordPlayerSender =
        Mockito.mock(classOf[DiscordPlayerSender], Mockito.withSettings.stubOnly.defaultAnswer((invocation: InvocationOnMock) -> {
            fun foo(invocation: InvocationOnMock): AnyRef = {
                if (!Modifier.isAbstract(invocation.getMethod.getModifiers))
                    invocation.callRealMethod
                else
                    invocation.getMethod.invoke(invocation.getMock.asInstanceOf[DiscordPlayerSender].player, invocation.getArguments)
            }

            foo(invocation)
        }).useConstructor(user, channel, player, module))
}

abstract class DiscordPlayerSender(user: User, channel: MessageChannel, var player: Player, val module: Nothing) : DiscordSenderBase(user, channel) with IMCPlayer[DiscordPlayerSender] with Player {

    override fun getVanillaCmdListener = null

    override fun sendMessage(message: String): Unit = {
        player.sendMessage(message)
        super.sendMessage(message)
    }

    override fun sendMessage(messages: String*): Unit = {
        player.sendMessage(messages: _*)
        super.sendMessage(messages: _*)
    }
}