package buttondevteam.discordplugin.commands;

import buttondevteam.lib.chat.Command2;
import buttondevteam.lib.chat.CommandClass;

@CommandClass(helpText = {
	"Help command", //
	"Shows some info about a command or lists the available commands.", //
})
public class HelpCommand extends ICommand2DC {
	@Command2.Subcommand
	public boolean def(Command2DCSender sender, @Command2.TextArg @Command2.OptionalArg String args) {
		if (args == null || args.length() == 0)
			sender.sendMessage(getManager().getCommandsText());
		else {
			String[] ht = getManager().getHelpText(args);
			if (ht == null)
				sender.sendMessage("Command not found: " + args);
			else
				sender.sendMessage(ht);
		}
		return true;
	}
}
