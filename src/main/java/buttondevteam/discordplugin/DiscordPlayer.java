package buttondevteam.discordplugin;

import buttondevteam.lib.player.ChromaGamerBase;
import buttondevteam.lib.player.UserClass;

@UserClass(foldername = "discord")
public class DiscordPlayer extends ChromaGamerBase {
	private String did;

	public DiscordPlayer() {
	}

	public String getDiscordID() {
		if (did == null)
			did = plugindata.getString(getFolder() + "_id");
		return did;
	}
}
