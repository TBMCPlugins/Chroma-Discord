package buttondevteam.discordplugin;

import buttondevteam.lib.player.ChromaGamerBase;
import buttondevteam.lib.player.UserClass;

@UserClass(foldername = "discord")
public class DiscordPlayer extends ChromaGamerBase {
	private String did;

	public DiscordPlayer() {
	}

	public String getDiscordID() {
		return did;
	}

	@Override
	public String getFileName() {
		return did;
	}
}
