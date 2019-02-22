package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.listeners.CommonListeners;
import buttondevteam.lib.chat.Command2;
import buttondevteam.lib.chat.CommandClass;

@CommandClass(helpText = {
	"Switches debug mode."
})
public class DebugCommand extends ICommand2DC {
	@Command2.Subcommand
	public boolean def(Command2DCSender sender, String args) {
		if (sender.getMessage().getAuthor().hasRole(DiscordPlugin.mainServer.getRoleByID(126030201472811008L))) //TODO: Make configurable
			sender.sendMessage("debug " + (CommonListeners.debug() ? "enabled" : "disabled"));
        else
			sender.sendMessage("you need to be a moderator to use this command.");
        return true;
    }
}
