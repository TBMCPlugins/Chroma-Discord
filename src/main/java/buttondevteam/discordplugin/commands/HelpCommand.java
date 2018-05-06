package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlugin;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Arrays;
import java.util.stream.Collectors;

public class HelpCommand extends DiscordCommandBase {

	@Override
	public String getCommandName() {
		return "help";
	}

	@Override
	public void run(IMessage message, String args) {
		DiscordCommandBase argdc;
		if (args.length() == 0)
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"Available commands:\n" + DiscordCommandBase.commands.values().stream()
							.map(dc -> dc.getCommandName()).collect(Collectors.joining("\n")));
		else
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					(argdc = DiscordCommandBase.commands.get(args)) == null ? "Command not found: " + args
							: Arrays.stream(argdc.getHelpText()).collect(Collectors.joining("\n")));
	}

	@Override
	public String[] getHelpText() {
		return new String[] { //
				"---- Help command ----", //
				"Shows some info about a command or lists the available commands.", //
				"Usage: help [command]"//
		};
	}

}
