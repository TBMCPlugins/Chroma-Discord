package buttondevteam.discordplugin;

import buttondevteam.lib.TBMCPlayer;

public class DiscordPlayer extends TBMCPlayer {
	public String getDiscordID() {
		return getData();
	}

	public void setDiscordID(String id) {
		setData(id);
	}
}
