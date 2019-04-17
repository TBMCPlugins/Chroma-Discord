package buttondevteam.discordplugin;

import buttondevteam.discordplugin.mcchat.MCChatPrivate;
import buttondevteam.lib.player.ChromaGamerBase;
import buttondevteam.lib.player.UserClass;

@UserClass(foldername = "discord")
public class DiscordPlayer extends ChromaGamerBase {
	private String did;
	// private @Getter @Setter boolean minecraftChatEnabled;

	public DiscordPlayer() {
	}

	public String getDiscordID() {
		if (did == null)
			did = plugindata.getString(getFolder() + "_id");
		return did;
	}

	/**
	 * Returns true if player has the private Minecraft chat enabled. For setting the value, see
	 * {@link MCChatPrivate#privateMCChat(sx.blah.discord.handle.obj.MessageChannel, boolean, sx.blah.discord.handle.obj.IUser, DiscordPlayer)}
	 */
	public boolean isMinecraftChatEnabled() {
		return MCChatPrivate.isMinecraftChatEnabled(this);
	}
}
