package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.lib.chat.{Command2, CommandClass}

@CommandClass(helpText = Array("Version", "Returns the plugin's version"))
object VersionCommand {
    def getVersion: Array[String] = {
        val desc = DiscordPlugin.plugin.getDescription
        Array[String](desc.getFullName, desc.getWebsite)
    }
}

@CommandClass(helpText = Array("Version", "Returns the plugin's version"))
class VersionCommand extends ICommand2DC {
    @Command2.Subcommand override def `def`(sender: Command2DCSender): Boolean = {
        sender.sendMessage(VersionCommand.getVersion)
        true
    }
}