package buttondevteam.discordplugin;

import java.util.UUID;

import buttondevteam.discordplugin.playerfaker.DiscordFakePlayer;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

public class DiscordConnectedPlayer extends DiscordFakePlayer {
	private static int nextEntityId = 0;

	public DiscordConnectedPlayer(IUser user, IChannel channel, UUID uuid) {
		super(user, channel, nextEntityId++, uuid);
	}

}
