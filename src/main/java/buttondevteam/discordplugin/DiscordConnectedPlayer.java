package buttondevteam.discordplugin;

import java.util.UUID;

import buttondevteam.discordplugin.playerfaker.DiscordFakePlayer;
import buttondevteam.discordplugin.playerfaker.VanillaCommandListener;
import lombok.Getter;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

public class DiscordConnectedPlayer extends DiscordFakePlayer implements IMCPlayer<DiscordConnectedPlayer> {
	private static int nextEntityId = 10000;
	private @Getter VanillaCommandListener<DiscordConnectedPlayer> vanillaCmdListener;

	public DiscordConnectedPlayer(IUser user, IChannel channel, UUID uuid, String mcname) {
		super(user, channel, nextEntityId++, uuid, mcname);
		vanillaCmdListener = new VanillaCommandListener<>(this);
	}

}
