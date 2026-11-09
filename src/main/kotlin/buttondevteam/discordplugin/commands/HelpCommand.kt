package buttondevteam.discordplugin.commands


@CommandClass(helpText = arrayOf("Help command", //
    "Shows some info about a command or lists the available commands."))
class HelpCommand : ICommand2DC {
    @Command2.Subcommand
    fun def(sender: Command2DCSender, @Command2.TextArg @Command2.OptionalArg command: String): Boolean = {
        if (command == null || command.isEmpty) sender.sendMessage(getManager.getCommandsText)
        else {
            val ht = getManager.getCommandNode(command).getData.getHelpText(sender)
            if (ht == null) sender.sendMessage("Command not found: " + command)
            else sender.sendMessage(ht)
        }
        true
    }
}