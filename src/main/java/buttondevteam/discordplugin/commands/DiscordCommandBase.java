package buttondevteam.discordplugin.commands;

import java.util.HashMap;

import buttondevteam.discordplugin.DiscordPlugin;
import sx.blah.discord.handle.obj.IMessage;

public abstract class DiscordCommandBase {
	public abstract String getCommandName();

	public abstract void run(IMessage message, String args);

	private static final HashMap<String, DiscordCommandBase> commands = new HashMap<String, DiscordCommandBase>();

	static {
		commands.put("connect", new ConnectCommand()); // TODO: API for adding commands?
		commands.put("userinfo", new UserinfoCommand());
	}

	public static void runCommand(String cmd, String args, IMessage message) {
		DiscordCommandBase command = commands.get(cmd);
		if (command == null) {
			// TODO: Help command
			DiscordPlugin.sendMessageToChannel(message.getChannel(), "Unknown command: " + cmd + " with args: " + args);
			return;
		}
		command.run(message, args);
	}
}
