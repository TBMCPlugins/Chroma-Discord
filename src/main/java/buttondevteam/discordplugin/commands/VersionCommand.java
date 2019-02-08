package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.chat.CommandClass;
import lombok.val;

@CommandClass(helpText = {
	"Version",
	"Returns the plugin's version"
})
public class VersionCommand extends ICommand2DC {
	public boolean def(Command2DCSender sender) {
		sender.sendMessage(getVersion());
		return true;
	}

	public static String[] getVersion() {
		val desc = DiscordPlugin.plugin.getDescription();
		return new String[]{ //
				desc.getFullName(), //
				desc.getWebsite() //
		};
	}
}
