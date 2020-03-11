package buttondevteam.discordplugin.playerfaker;

import buttondevteam.discordplugin.DiscordSenderBase;
import buttondevteam.discordplugin.IMCPlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class VCMDWrapper {
	@Getter //Needed to mock the player
	private final Object listener;

	/**
	 * This constructor will only send raw vanilla messages to the sender in plain text.
	 *
	 * @param player The Discord sender player (the wrapper)
	 */
	public static <T extends DiscordSenderBase & IMCPlayer<T>> Object createListener(T player) {
		return createListener(player, null);
	}

	/**
	 * This constructor will send both raw vanilla messages to the sender in plain text and forward the raw message to the provided player.
	 *
	 * @param player       The Discord sender player (the wrapper)
	 * @param bukkitplayer The Bukkit player to send the raw message to
	 */
	public static <T extends DiscordSenderBase & IMCPlayer<T>> Object createListener(T player, Player bukkitplayer) {
		String mcpackage = Bukkit.getServer().getClass().getPackage().getName();
		if (mcpackage.contains("1_12"))
			return bukkitplayer == null ? new VanillaCommandListener<>(player) : new VanillaCommandListener<>(player, bukkitplayer);
		else if (mcpackage.contains("1_14") || mcpackage.contains("1_15"))
			return bukkitplayer == null ? new VanillaCommandListener14<>(player) : new VanillaCommandListener14<>(player, bukkitplayer);
		else
			return null;
	}
}
