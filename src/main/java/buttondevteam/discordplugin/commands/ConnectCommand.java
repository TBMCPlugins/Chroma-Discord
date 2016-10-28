package buttondevteam.discordplugin.commands;

import com.google.common.collect.HashBiMap;

import buttondevteam.discordplugin.DiscordPlugin;
import sx.blah.discord.handle.obj.IMessage;

public class ConnectCommand extends DiscordCommandBase {

	@Override
	public String getCommandName() {
		return "connect";
	}

	/**
	 * Key: Minecraft name<br>
	 * Value: Discord ID
	 */
	public static HashBiMap<String, String> WaitingToConnect = HashBiMap.create();

	@Override
	public void run(IMessage message, String args) {
		if (args.length() == 0) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(), "Usage: connect <Minecraftname>");
			return;
		}
		if (args.contains(" ")) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"Too many arguments.\nUsage: connect <Minecraftname>");
			return;
		}
		if (WaitingToConnect.inverse().containsKey(message.getAuthor().getID())) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"Replacing " + WaitingToConnect.inverse().get(message.getAuthor().getID()) + " with " + args);
			WaitingToConnect.inverse().remove(message.getAuthor().getID());
		}
		WaitingToConnect.put(args, message.getAuthor().getID());
		DiscordPlugin.sendMessageToChannel(message.getChannel(),
				"Pending connection - accept connection in Minecraft from the account " + args
						+ " before the server gets restarted.");
	}

}
