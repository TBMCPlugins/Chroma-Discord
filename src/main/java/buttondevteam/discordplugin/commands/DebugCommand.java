package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.listeners.CommonListeners;
import buttondevteam.lib.chat.Command2;
import buttondevteam.lib.chat.CommandClass;
import sx.blah.discord.handle.obj.IMessage;

@CommandClass(helpText = {
	"Switches debug mode."
})
public class DebugCommand extends ICommand2DC {
	@Command2.Subcommand
	public boolean def(IMessage message, String args) {
		if (message.getAuthor().hasRole(DiscordPlugin.mainServer.getRoleByID(126030201472811008L))) //TODO: Make configurable
	        message.reply("Debug " + (CommonListeners.debug() ? "enabled" : "disabled"));
        else
            message.reply("You need to be a moderator to use this command.");
        return true;
    }
}
