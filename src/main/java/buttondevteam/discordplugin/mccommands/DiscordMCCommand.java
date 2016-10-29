package buttondevteam.discordplugin.mccommands;

import org.bukkit.command.CommandSender;
import buttondevteam.lib.chat.TBMCChatAPI;
import buttondevteam.lib.chat.TBMCCommandBase;

public class DiscordMCCommand extends TBMCCommandBase {

	@Override
	public String GetCommandPath() {
		return "discord";
	}

	@Override
	public String[] GetHelpText(String alias) {
		return TBMCChatAPI.GetSubCommands(this);
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
		return false;
	}

}
