package buttondevteam.discordplugin.mcchat

import buttondevteam.discordplugin.commands.{Command2DCSender, ICommand2DC}
import buttondevteam.discordplugin.mcchat.sender.DiscordPlayer
import buttondevteam.discordplugin.{DPUtils, DiscordPlugin}
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
        // TODO: If the user is logged in, don't let the object be removed from the cache (test first)
        if (!module.allowPrivateChat.get) {
            sender.sendMessage("Using the private chat is not allowed on this Minecraft server.")
            return true
        }
        val channel = sender.event.getInteraction.getChannel.block()
        if (!channel.isInstanceOf[PrivateChannel]) {
            sender.sendMessage("This command can only be issued in a direct message with the bot.")
            return true
        }
        val user: DiscordPlayer = ChromaGamerBase.getUser(sender.author.getId.asString, classOf[DiscordPlayer])
        val mcchat: Boolean = !user.isMinecraftChatEnabled
        MCChatPrivate.privateMCChat(channel, mcchat, sender.author, user)
        sender.sendMessage("Minecraft chat " +
            (if (mcchat) "enabled. Use '" + DiscordPlugin.getPrefix + "mcchat' again to turn it off."
            else "disabled."))
        true
        // TODO: Pin channel switching to indicate the current channel
    }
}