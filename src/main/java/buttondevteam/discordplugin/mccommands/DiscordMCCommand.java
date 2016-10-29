package buttondevteam.discordplugin.mccommands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.commands.ConnectCommand;
import buttondevteam.lib.TBMCPlayer;
import buttondevteam.lib.chat.TBMCCommandBase;

public class DiscordMCCommand extends TBMCCommandBase {

	@Override
	public String GetCommandPath() {
		return "discord";
	}

	@Override
	public String[] GetHelpText(String alias) {
		return new String[] { // TODO
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
