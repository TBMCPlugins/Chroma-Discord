package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DiscordPlugin

@CommandClass(helpText = arrayOf("Version", "Returns the plugin's version"))
object VersionCommand {
    fun getVersion(): Array[String] = {
        val desc = DiscordPlugin.plugin.getDescription
        Array[String](desc.getFullName, desc.getWebsite)
    }
}

@CommandClass(helpText = arrayOf("Version", "Returns the plugin's version"))
class VersionCommand : ICommand2DC {
    @Command2.Subcommand override fun def(sender: Command2DCSender): Boolean = {
        sender.sendMessage(VersionCommand.getVersion)
        true
    }
}