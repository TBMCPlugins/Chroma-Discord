package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.lib.chat.Command2
import buttondevteam.lib.chat.CommandClass

@CommandClass(helpText = ["Version", "Returns the plugin's version"])
class VersionCommand : ICommand2DC() {
    @Command2.Subcommand override fun def(sender: Command2DCSender): Boolean {
        sender.sendMessage(getVersion())
        return true
    }

    companion object {
        fun getVersion(): Array<String> {
            val desc = DiscordPlugin.plugin.description
            return arrayOf(desc.fullName, desc.website).filterNotNull().toTypedArray()
        }
    }
}