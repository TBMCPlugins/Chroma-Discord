package buttondevteam.discordplugin.commands

import buttondevteam.lib.chat.Command2Sender
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.User
import kotlin.jvm.optionals.getOrNull

class Command2DCSender(val event: ChatInputInteractionEvent) : Command2Sender {
    val authorAsMember: Member? = event.interaction.member.getOrNull()
    val author: User = event.interaction.user

    override fun sendMessage(message: String) {
        if (message.isEmpty()) return
        event.reply(message)
    }

    override fun sendMessage(message: Array<String>) = sendMessage(message.joinToString("\n"))

    override val name: String = authorAsMember?.nickname?.getOrNull() ?: author.username
}