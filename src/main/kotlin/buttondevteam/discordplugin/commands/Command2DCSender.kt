package buttondevteam.discordplugin.commands

import buttondevteam.lib.chat.Command2Sender
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent

class Command2DCSender(val event: ChatInputInteractionEvent) : Command2Sender {
    val authorAsMember: Option[Member] = event.getInteraction.getMember.toScala
    val author: User = event.getInteraction.getUser
    override fun sendMessage(message: String): Unit = {
        if (message.isEmpty) return Unit
        //Some(message) map DPUtils.sanitizeString map { (msg: String) -> Character.toLowerCase(msg.charAt(0)) + msg.substring(1) } foreach event.reply - don't even need this
        event.reply(message)
    }

    override fun sendMessage(message: Array[String]): Unit = sendMessage(String.join("\n", message: _*))

    override fun getName(): String = authorAsMember.flatMap(_.getNickname.toScala).getOrElse(author.getUsername)
}