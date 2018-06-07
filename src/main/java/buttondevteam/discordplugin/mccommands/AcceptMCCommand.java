package buttondevteam.discordplugin.mccommands;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.commands.ConnectCommand;
import buttondevteam.discordplugin.listeners.MCChatListener;
import buttondevteam.lib.chat.CommandClass;
import buttondevteam.lib.player.ChromaGamerBase;
import buttondevteam.lib.player.TBMCPlayer;
import buttondevteam.lib.player.TBMCPlayerBase;
import org.bukkit.entity.Player;

@CommandClass(modOnly = false, path = "accept")
public class AcceptMCCommand extends DiscordMCCommandBase {

	@Override
	public String[] GetHelpText(String alias) {
		return new String[] { //
				"§6---- Accept Discord connection ----", //
				"Accept a pending connection between your Discord and Minecraft account.", //
                "To start the connection process, do §b/connect <MCname>§r in the #bot channel on Discord", //
				"Usage: /" + alias + " accept" //
		};
	}

	@Override
	public boolean OnCommand(Player player, String alias, String[] args) {
		String did = ConnectCommand.WaitingToConnect.get(player.getName());
		if (did == null) {
			player.sendMessage("§cYou don't have a pending connection to Discord.");
			return true;
		}
		DiscordPlayer dp = ChromaGamerBase.getUser(did, DiscordPlayer.class);
		TBMCPlayer mcp = TBMCPlayerBase.getPlayer(player.getUniqueId(), TBMCPlayer.class);
		dp.connectWith(mcp);
		dp.save();
		mcp.save();
		ConnectCommand.WaitingToConnect.remove(player.getName());
		MCChatListener.UnconnectedSenders.remove(did);
		player.sendMessage("§bAccounts connected.");
		return true;
	}

}
