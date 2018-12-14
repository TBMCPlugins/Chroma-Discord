package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import static buttondevteam.discordplugin.listeners.CommonListeners.debug;

public abstract class DiscordCommandBase {
	public abstract String getCommandName();

	public abstract boolean run(IMessage message, String args);

	public abstract String[] getHelpText();

	static final HashMap<String, DiscordCommandBase> commands = new HashMap<String, DiscordCommandBase>();

	public static void registerCommands() {
		commands.put("connect", new ConnectCommand()); // TODO: API for adding commands?
		commands.put("userinfo", new UserinfoCommand());
		commands.put("help", new HelpCommand());
		commands.put("role", new RoleCommand());
		commands.put("mcchat", new MCChatCommand());
		commands.put("channelcon", new ChannelconCommand());
		commands.put("debug", new DebugCommand());
		commands.put("version", new VersionCommand());
	}

	public static void runCommand(String cmd, String args, IMessage message) {
		debug("F"); //Not sure if needed
		DiscordCommandBase command = commands.get(cmd);
		if (command == null) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"Unknown command: " + cmd + " with args: " + args + "\nDo '"
							+ (message.getChannel().isPrivate() ? "" : message.getClient().getOurUser().mention() + " ")
							+ "help' for help");
			return;
		}
		debug("G");
		try {
			if (!command.run(message, args))
				DiscordPlugin.sendMessageToChannel(message.getChannel(), Arrays.stream(command.getHelpText()).collect(Collectors.joining("\n")));
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while executing command " + cmd + "!", e);
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"An internal error occured while executing this command. For more technical details see the server-issues channel on the dev Discord.");
		}
		debug("H");
	}

	protected String[] splitargs(String args) {
		return args.split("\\s+");
	}
}
