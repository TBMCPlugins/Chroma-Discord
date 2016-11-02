package buttondevteam.discordplugin.commands;

import java.util.stream.Collectors;

import buttondevteam.discordplugin.DiscordPlugin;
import sx.blah.discord.handle.obj.IMessage;

public class HelpCommand extends DiscordCommandBase {

	@Override
	public String getCommandName() {
		return "help";
	}

	@Override
	public void run(IMessage message, String args) {
		DiscordPlugin.sendMessageToChannel(message.getChannel(), "Available commands:\n" + DiscordCommandBase.commands
				.values().stream().map(dc -> dc.getCommandName()).collect(Collectors.joining("\n")));
	}

}
