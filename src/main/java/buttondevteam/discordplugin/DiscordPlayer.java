package buttondevteam.discordplugin;

import buttondevteam.lib.TBMCPlayer;

public class DiscordPlayer extends TBMCPlayer {
	public String getDiscordID() {
		return getData();
	}

	public void setDiscrodID(String id) {
		setData(id);
	}
}
