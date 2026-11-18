package buttondevteam.discordplugin.commands

import buttondevteam.lib.chat.Command2
import buttondevteam.lib.chat.CommandClass


@CommandClass(helpText = ["Help command", "Shows some info about a command or lists the available commands."])
class HelpCommand : ICommand2DC() {
    @Command2.Subcommand
    fun def(sender: Command2DCSender, @Command2.TextArg @Command2.OptionalArg command: String? = null): Boolean {
        if (command.isNullOrEmpty()) sender.sendMessage(manager.getCommandList(sender))
        else {
            val ht = manager.getCommandNode(command)?.data?.getHelpText(sender)
            if (ht == null) sender.sendMessage("Command not found: $command")
            else sender.sendMessage(ht)
        }
        return true
    }
}