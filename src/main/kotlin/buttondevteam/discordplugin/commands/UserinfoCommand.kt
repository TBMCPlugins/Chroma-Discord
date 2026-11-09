package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DPUtils.FluxExtensions
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.mcchat.sender.DiscordUser
import buttondevteam.lib.chat.{Command2, CommandClass}
import buttondevteam.lib.player.ChromaGamerBase
import buttondevteam.lib.player.ChromaGamerBase.InfoTarget
import discord4j.core.`object`.entity.{Member, Message, User}

import scala.jdk.CollectionConverters.ListHasAsScala

@CommandClass(helpText = Array("User information", //
    "Shows some information about users, from Discord, from Minecraft or from Reddit if they have these accounts connected.",
    "If used without args, shows your info."))
class UserinfoCommand extends ICommand2DC {
    @Command2.Subcommand
    def `def`(sender: Command2DCSender, @Command2.OptionalArg @Command2.TextArg user: String): Boolean = {
        var target: User = null
        if (user == null || user.isEmpty) target = sender.author
        else { // TODO: Mention option
        }
        if (target == null) {
            sender.sendMessage("An error occurred.")
            return true
        }
        val dp = ChromaGamerBase.getUser(target.getId.asString, classOf[DiscordUser])
        val uinfo = new StringBuilder("User info for ").append(target.getUsername).append(":\n")
        uinfo.append(dp.getInfo(InfoTarget.Discord))
        sender.sendMessage(uinfo.toString)
        true
    }

    private def getUsers(message: Message, args: String) = {
        val guild = message.getGuild.block
        if (guild == null) { //Private channel
            DiscordPlugin.dc.getUsers.^^().filter(u => u.getUsername.equalsIgnoreCase(args)).collectSeq().block()
        }
        else
            guild.getMembers.^^().filter(_.getUsername.equalsIgnoreCase(args)).map(_.asInstanceOf[User]).collectSeq().block()
    }
}