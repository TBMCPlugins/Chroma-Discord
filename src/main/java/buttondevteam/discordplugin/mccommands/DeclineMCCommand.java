package buttondevteam.discordplugin.mccommands;

import buttondevteam.discordplugin.commands.ConnectCommand;
import buttondevteam.lib.chat.CommandClass;
import org.bukkit.entity.Player;

@CommandClass(modOnly = false, path = "decline")
public class DeclineMCCommand extends DiscordMCCommandBase {

	@Override
	public String[] GetHelpText(String alias) {
		return new String[] { //
				"§6---- Decline Discord connection ----", //
				"Decline a pending connection between your Discord and Minecraft account.", //
				"To start the connection process, do §b/connect <MCname>§r in the #bot channel on Discord", //
				"Usage: /" + alias + " decline" //
		};
	}

	@Override
	public boolean OnCommand(Player player, String alias, String[] args) {
		String did = ConnectCommand.WaitingToConnect.remove(player.getName());
		if (did == null) {
			player.sendMessage("§cYou don't have a pending connection to Discord.");
			return true;
		}
		player.sendMessage("§bPending connection declined.");
		return true;
	}

}
