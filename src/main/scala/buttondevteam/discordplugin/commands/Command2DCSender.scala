package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DPUtils
import buttondevteam.lib.chat.Command2Sender
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.entity.{Message, User}

class Command2DCSender(val message: Message) extends Command2Sender {
    def getMessage: Message = this.message

    override def sendMessage(message: String): Unit = {
        if (message.isEmpty) return
        var msg = DPUtils.sanitizeString(message)
        msg = Character.toLowerCase(message.charAt(0)) + message.substring(1)
        this.message.getChannel.flatMap((ch: MessageChannel) => ch.createMessage(this.message.getAuthor.map((u: User) => DPUtils.nickMention(u.getId) + ", ").orElse("") + msg)).subscribe
    }

    override def sendMessage(message: Array[String]): Unit = sendMessage(String.join("\n", message: _*))

    override def getName: String = Option(message.getAuthor.orElse(null)).map(_.getUsername).getOrElse("Discord")
}