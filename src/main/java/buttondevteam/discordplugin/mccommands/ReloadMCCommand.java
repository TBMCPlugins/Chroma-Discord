package buttondevteam.discordplugin.mccommands;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.chat.CommandClass;
import buttondevteam.lib.chat.TBMCCommandBase;
import org.bukkit.command.CommandSender;

@CommandClass(path = "discord reload")
public class ReloadMCCommand extends TBMCCommandBase {
	@Override
	public boolean OnCommand(CommandSender sender, String alias, String[] args) {
		if (DiscordPlugin.plugin.tryReloadConfig())
			sender.sendMessage("§bConfig reloaded."); //TODO: Convert to new command system
		else
			sender.sendMessage("§cFailed to reload config.");
		return true;
	}

	@Override
	public String[] GetHelpText(String alias) {
		return new String[]{
			"Reload",
			"Reloads the config. To apply some changes, you may need to also run /discord reset."
		};
	}
}
