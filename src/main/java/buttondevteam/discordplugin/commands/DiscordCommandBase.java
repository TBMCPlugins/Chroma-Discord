package buttondevteam.discordplugin.commands;

import java.util.HashMap;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import sx.blah.discord.handle.obj.IMessage;

public abstract class DiscordCommandBase {
	public abstract String getCommandName();

	public abstract void run(IMessage message, String args);

	static final HashMap<String, DiscordCommandBase> commands = new HashMap<String, DiscordCommandBase>();

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
		try {
			command.run(message, args);
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while executing command " + cmd + "!", e);
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"An internal error occured while executing this command. For more technical details see the server-issues channel on the dev Discord.");
		}
	}
}
