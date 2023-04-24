package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DPUtils
import buttondevteam.lib.chat.Command2Sender
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.entity.{Member, Message, User}
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent

import java.util.Optional
import scala.jdk.OptionConverters._

class Command2DCSender(val event: ChatInputInteractionEvent) extends Command2Sender {
    val authorAsMember: Option[Member] = event.getInteraction.getMember.toScala
    val author: User = event.getInteraction.getUser
    override def sendMessage(message: String): Unit = {
        if (message.isEmpty) return ()
        //Some(message) map DPUtils.sanitizeString map { (msg: String) => Character.toLowerCase(msg.charAt(0)) + msg.substring(1) } foreach event.reply - don't even need this
        event.reply(message);
    }

    override def sendMessage(message: Array[String]): Unit = sendMessage(String.join("\n", message: _*))

    override def getName: String = authorAsMember.flatMap(_.getNickname.toScala).getOrElse(author.getUsername)
}