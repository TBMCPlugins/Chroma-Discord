package buttondevteam.discordplugin.mcchat

import buttondevteam.discordplugin.commands.Command2DCSender
import buttondevteam.discordplugin.commands.ICommand2DC
import buttondevteam.lib.chat.Command2
import buttondevteam.lib.chat.CommandClass

@CommandClass(
    helpText = [
        "MC Chat",
        "This command enables or disables the Minecraft chat in private messages.",
        "It can be useful if you don't want your messages to be visible, for example when talking in a private channel.",
        "You can also run all of the ingame commands you have access to using this command, if you have your accounts connected."
    ]
)
class MCChatCommand(private val module: MinecraftChatModule) : ICommand2DC() {
    @Command2.Subcommand override fun def(sender: Command2DCSender): Boolean {
        // TODO: If the user is logged in, don't let the object be removed from the cache (test first)
        sender.sendMessage("Private Minecraft chat is not supported in this version.")
        // TODO: Pin channel switching to indicate the current channel
        return true
    }
}