package buttondevteam.discordplugin;

import buttondevteam.discordplugin.mcchat.MinecraftChatModule;
import buttondevteam.discordplugin.playerfaker.DiscordFakePlayer;
import buttondevteam.discordplugin.playerfaker.VanillaCommandListener;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import lombok.Getter;

import java.util.UUID;

public class DiscordConnectedPlayer extends DiscordFakePlayer implements IMCPlayer<DiscordConnectedPlayer> {
	private static int nextEntityId = 10000;
	private @Getter VanillaCommandListener<DiscordConnectedPlayer> vanillaCmdListener;

	public DiscordConnectedPlayer(User user, MessageChannel channel, UUID uuid, String mcname, MinecraftChatModule module) {
		super(user, channel, nextEntityId++, uuid, mcname ,module);
		vanillaCmdListener = new VanillaCommandListener<>(this);
	}

}
