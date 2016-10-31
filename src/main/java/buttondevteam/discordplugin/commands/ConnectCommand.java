package buttondevteam.discordplugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.google.common.collect.HashBiMap;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.TBMCDiscordAPI;
import buttondevteam.lib.TBMCPlayer;
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
		@SuppressWarnings("deprecation")
		OfflinePlayer p = Bukkit.getOfflinePlayer(args);
		if (p == null) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(), "The specified Minecraft player cannot be found");
			return;
		}
		try (TBMCPlayer pl = TBMCPlayer.getPlayer(p)) {
			if (message.getAuthor().getID().equals(pl.asPluginPlayer(DiscordPlayer.class).getDiscordID())) {
				DiscordPlugin.sendMessageToChannel(message.getChannel(), "You already have this account connected.");
				return;
			}
		} catch (Exception e) {
			TBMCDiscordAPI.SendException(e, "An error occured while connecting a Discord account!");
			DiscordPlugin.sendMessageToChannel(message.getChannel(), "An internal error occured!\n" + e);
		}
		WaitingToConnect.put(args, message.getAuthor().getID());
		DiscordPlugin.sendMessageToChannel(message.getChannel(),
				"Pending connection - accept connection in Minecraft from the account " + args
						+ " before the server gets restarted.");
		if (p.isOnline())
			((Player) p).sendMessage(
					"§bTo connect with the Discord account " + message.getAuthor().getName() + " do /discord accept");
	}

}
