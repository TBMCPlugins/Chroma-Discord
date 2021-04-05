package buttondevteam.discordplugin.mcchat

import buttondevteam.discordplugin.commands.{Command2DCSender, ICommand2DC}
import buttondevteam.discordplugin.{DPUtils, DiscordPlayer, DiscordPlugin}
import buttondevteam.lib.chat.{Command2, CommandClass}
import buttondevteam.lib.player.ChromaGamerBase
import discord4j.core.`object`.entity.channel.PrivateChannel

@CommandClass(helpText = Array(
    "MC Chat",
    "This command enables or disables the Minecraft chat in private messages.", //
    "It can be useful if you don't want your messages to be visible, for example when talking in a private channel.",
    "You can also run all of the ingame commands you have access to using this command, if you have your accounts connected." //
))
class MCChatCommand(private val module: MinecraftChatModule) extends ICommand2DC {
    @Command2.Subcommand override def `def`(sender: Command2DCSender): Boolean = {
        if (!(module.allowPrivateChat.get)) {
            sender.sendMessage("using the private chat is not allowed on this Minecraft server.")
            return true
        }
        val message = sender.getMessage
        val channel = message.getChannel.block
        @SuppressWarnings(Array("OptionalGetWithoutIsPresent")) val author = message.getAuthor.get
        if (!((channel.isInstanceOf[PrivateChannel]))) {
            DPUtils.reply(message, channel, "this command can only be issued in a direct message with the bot.").subscribe
            return true
        }
        val user: DiscordPlayer = ChromaGamerBase.getUser(author.getId.asString, classOf[DiscordPlayer])
        val mcchat: Boolean = !(user.isMinecraftChatEnabled)
        MCChatPrivate.privateMCChat(channel, mcchat, author, user)
        DPUtils.reply(message, channel, "Minecraft chat " +
            (if (mcchat) "enabled. Use '" + DiscordPlugin.getPrefix + "mcchat' again to turn it off."
            else "disabled.")).subscribe
        true
        // TODO: Pin channel switching to indicate the current channel
    }
}