package buttondevteam.discordplugin.commands;

import java.util.HashMap;

import sx.blah.discord.handle.obj.IMessage;

public abstract class DiscordCommandBase {
	public abstract String getCommandName();

	public abstract void run(IMessage message, String args);

	private static final HashMap<String, DiscordCommandBase> commands = new HashMap<String, DiscordCommandBase>();
	static {
		commands.put("connect", new ConnectCommand()); // TODO: API for adding commands?
	}

	public static final void runCommand(IMessage message) {

	}
}
