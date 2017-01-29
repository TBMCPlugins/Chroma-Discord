package buttondevteam.discordplugin;

import buttondevteam.lib.player.ChromaGamerBase;

public class DiscordPlayer extends ChromaGamerBase {
	public String getDiscordID() {
		return plugindata.getString("id");
	}

	public void setDiscordID(String id) {
		plugindata.set("id", id);
	}

	@Override
	public String getFileName() {
		return "discord";
	}
}
