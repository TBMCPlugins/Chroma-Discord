package buttondevteam.discordplugin.mccommands;

import buttondevteam.discordplugin.commands.VersionCommand;
import buttondevteam.lib.chat.CommandClass;
import buttondevteam.lib.chat.TBMCCommandBase;
import org.bukkit.command.CommandSender;

@CommandClass(path = "discord version")
public class VersionMCCommand extends TBMCCommandBase {
	@Override
	public boolean OnCommand(CommandSender commandSender, String s, String[] strings) {
		commandSender.sendMessage(VersionCommand.getVersion());
		return true;
	}

	@Override
	public String[] GetHelpText(String s) {
		return VersionCommand.getVersion(); //Heh
	}
}
