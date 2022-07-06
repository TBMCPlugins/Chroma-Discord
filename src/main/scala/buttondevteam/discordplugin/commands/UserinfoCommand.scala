package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.mcchat.sender.DiscordPlayer
import buttondevteam.lib.chat.{Command2, CommandClass}
import buttondevteam.lib.player.ChromaGamerBase
import buttondevteam.lib.player.ChromaGamerBase.InfoTarget
import discord4j.core.`object`.entity.{Message, User}
import reactor.core.scala.publisher.SFlux

import scala.jdk.CollectionConverters.ListHasAsScala

@CommandClass(helpText = Array("User information", //
    "Shows some information about users, from Discord, from Minecraft or from Reddit if they have these accounts connected.",
    "If used without args, shows your info."))
class UserinfoCommand extends ICommand2DC {
    @Command2.Subcommand
    def `def`(sender: Command2DCSender, @Command2.OptionalArg @Command2.TextArg user: String): Boolean = {
        val message = sender.getMessage
        var target: User = null
        val channel = message.getChannel.block
        assert(channel != null)
        if (user == null || user.isEmpty) target = message.getAuthor.orElse(null)
        else {
            val firstmention = message.getUserMentions.asScala.find((m: User) => !(m.getId.asString == DiscordPlugin.dc.getSelfId.asString))
            if (firstmention.isDefined) target = firstmention.get
            else if (user.contains("#")) {
                val targettag = user.split("#")
                val targets = getUsers(message, targettag(0))
                if (targets.isEmpty) {
                    channel.createMessage("The user cannot be found (by name): " + user).subscribe()
                    return true
                }
                targets.collectFirst {
                    case user => user.getDiscriminator.equalsIgnoreCase(targettag(1))
                }
                if (target == null) {
                    channel.createMessage("The user cannot be found (by discriminator): " + user + "(Found " + targets.size + " users with the name.)").subscribe()
                    return true
                }
            }
            else {
                val targets = getUsers(message, user)
                if (targets.isEmpty) {
                    channel.createMessage("The user cannot be found on Discord: " + user).subscribe()
                    return true
                }
                if (targets.size > 1) {
                    channel.createMessage("Multiple users found with that (nick)name. Please specify the whole tag, like ChromaBot#6338 or use a ping.").subscribe()
                    return true
                }
                target = targets.head
            }
        }
        if (target == null) {
            sender.sendMessage("An error occurred.")
            return true
        }
        val dp = ChromaGamerBase.getUser(target.getId.asString, classOf[DiscordPlayer])
        val uinfo = new StringBuilder("User info for ").append(target.getUsername).append(":\n")
        uinfo.append(dp.getInfo(InfoTarget.Discord))
        channel.createMessage(uinfo.toString).subscribe()
        true
    }

    private def getUsers(message: Message, args: String) = {
        val guild = message.getGuild.block
        if (guild == null) { //Private channel
            SFlux(DiscordPlugin.dc.getUsers).filter(u => u.getUsername.equalsIgnoreCase(args)).collectSeq().block()
        }
        else
            SFlux(guild.getMembers).filter(_.getUsername.equalsIgnoreCase(args)).map(_.asInstanceOf[User]).collectSeq().block()
    }
}