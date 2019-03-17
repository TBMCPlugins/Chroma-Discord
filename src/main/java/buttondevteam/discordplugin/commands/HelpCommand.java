package buttondevteam.discordplugin.commands;

import buttondevteam.lib.chat.CommandClass;

@CommandClass(helpText = {
	"Help command", //
	"Shows some info about a command or lists the available commands.", //
})
public class HelpCommand extends ICommand2DC {
	@Override
	public boolean def(Command2DCSender sender, String args) {
		if (args.length() == 0)
			sender.sendMessage(getManager().getCommandsText());
		else
			sender.sendMessage("Soon:tm:"); //TODO
        return true;
	}
}
