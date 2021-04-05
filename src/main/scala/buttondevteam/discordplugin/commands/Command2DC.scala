package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.lib.chat.Command2

import java.lang.reflect.Method

class Command2DC extends Command2[ICommand2DC, Command2DCSender] {
    override def registerCommand(command: ICommand2DC): Unit =
        super.registerCommand(command, DiscordPlugin.getPrefix) //Needs to be configurable for the helps
    override def hasPermission(sender: Command2DCSender, command: ICommand2DC, method: Method): Boolean = {
        //return !command.isModOnly() || sender.getMessage().getAuthor().hasRole(DiscordPlugin.plugin.modRole().get()); //TODO: modRole may be null; more customisable way?
        true
    }
}