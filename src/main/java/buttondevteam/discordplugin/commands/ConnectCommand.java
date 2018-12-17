package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.player.TBMCPlayer;
import buttondevteam.lib.player.TBMCPlayerBase;
import com.google.common.collect.HashBiMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
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
	public boolean run(IMessage message, String args) {
		if (args.length() == 0)
			return false;
		if (args.contains(" ")) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"Too many arguments.\nUsage: " + DiscordPlugin.getPrefix() + "connect <Minecraftname>");
			return true;
		}
		if (WaitingToConnect.inverse().containsKey(message.getAuthor().getStringID())) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"Replacing " + WaitingToConnect.inverse().get(message.getAuthor().getStringID()) + " with " + args);
			WaitingToConnect.inverse().remove(message.getAuthor().getStringID());
		}
		@SuppressWarnings("deprecation")
		OfflinePlayer p = Bukkit.getOfflinePlayer(args);
		if (p == null) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(), "The specified Minecraft player cannot be found");
			return true;
		}
		try (TBMCPlayer pl = TBMCPlayerBase.getPlayer(p.getUniqueId(), TBMCPlayer.class)) {
			DiscordPlayer dp = pl.getAs(DiscordPlayer.class);
			if (dp != null && message.getAuthor().getStringID().equals(dp.getDiscordID())) {
				DiscordPlugin.sendMessageToChannel(message.getChannel(), "You already have this account connected.");
				return true;
			}
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while connecting a Discord account!", e);
			DiscordPlugin.sendMessageToChannel(message.getChannel(), "An internal error occured!\n" + e);
		}
		WaitingToConnect.put(p.getName(), message.getAuthor().getStringID());
		DiscordPlugin.sendMessageToChannel(message.getChannel(),
				"Alright! Now accept the connection in Minecraft from the account " + args
						+ " before the next server restart. You can also adjust the Minecraft name you want to connect to with the same command.");
		if (p.isOnline())
			((Player) p).sendMessage("§bTo connect with the Discord account " + message.getAuthor().getName() + "#"
					+ message.getAuthor().getDiscriminator() + " do /discord accept");
		return true;
	}

	@Override
	public String[] getHelpText() {
		return new String[] { //
				"---- Connect command ----", //
				"This command lets you connect your account with a Minecraft account. This allows using the Minecraft chat and other things.", //
				"Usage: /connect <Minecraftname>" //
		};
	}

}
