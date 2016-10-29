package buttondevteam.discordplugin.mccommands;

import org.bukkit.command.CommandSender;
import buttondevteam.discordplugin.commands.ConnectCommand;

public class DeclineMCCommand extends DiscordMCCommandBase {

	@Override
	public String GetDiscordCommandPath() {
		return "decline";
	}

	@Override
	public String[] GetHelpText(String alias) {
		return new String[] { //
				"§6---- Decline Discord connection ----", //
				"Decline a pending connection between your Discord and Minecraft account.", //
				"To start the connection process, do §b@ChromaBot connect <MCname>§r in the #bot channel on Discord", //
				"Usage: /" + alias + " decline" //
		};
	}

	@Override
	public boolean GetModOnly() {
		return false;
	}

	@Override
	public boolean GetPlayerOnly() {
		return true;
	}

	@Override
	public boolean OnCommand(CommandSender sender, String alias, String[] args) {
		String did = ConnectCommand.WaitingToConnect.remove(sender.getName());
		if (did == null) {
			sender.sendMessage("§cYou don't have a pending connection to Discord.");
			return true;
		}
		return true;
	}

}
