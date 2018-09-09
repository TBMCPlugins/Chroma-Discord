package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlugin;
import lombok.val;
import sx.blah.discord.handle.obj.IMessage;

public class VersionCommand extends DiscordCommandBase {
	@Override
	public String getCommandName() {
		return "version";
	}

	@Override
	public boolean run(IMessage message, String args) {
		DiscordPlugin.sendMessageToChannel(message.getChannel(), String.join("\n", getVersion()));
		return true;
	}

	@Override
	public String[] getHelpText() {
		return VersionCommand.getVersion(); //Heh
	}

	public static String[] getVersion() {
		val desc = DiscordPlugin.plugin.getDescription();
		return new String[]{ //
				desc.getFullName(), //
				desc.getWebsite() //
		};
	}
}
