package buttondevteam.discordplugin;

import buttondevteam.discordplugin.mcchat.MinecraftChatModule;
import buttondevteam.discordplugin.playerfaker.DiscordFakePlayer;
import buttondevteam.discordplugin.playerfaker.VanillaCommandListener;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

public class DiscordConnectedPlayer extends DiscordFakePlayer implements IMCPlayer<DiscordConnectedPlayer> {
	private static int nextEntityId = 10000;
	private @Getter VanillaCommandListener<DiscordConnectedPlayer> vanillaCmdListener;
	@Getter
	@Setter
	private boolean loggedIn = false;

	public DiscordConnectedPlayer(User user, MessageChannel channel, UUID uuid, String mcname, MinecraftChatModule module) {
		super(user, channel, nextEntityId++, uuid, mcname, module);
		try {
			vanillaCmdListener = new VanillaCommandListener<>(this);
		} catch (NoClassDefFoundError e) {
			DPUtils.getLogger().warning("Vanilla commands won't be available from Discord due to a compatibility error.");
		}
	}

}
