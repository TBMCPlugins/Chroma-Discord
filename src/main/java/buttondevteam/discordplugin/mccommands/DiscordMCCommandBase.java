package buttondevteam.discordplugin.mccommands;

import buttondevteam.lib.chat.TBMCCommandBase;

public abstract class DiscordMCCommandBase extends TBMCCommandBase {

	@Override
	public String GetCommandPath() {
		return "discord " + GetDiscordCommandPath();
	}

	public abstract String GetDiscordCommandPath();

}
