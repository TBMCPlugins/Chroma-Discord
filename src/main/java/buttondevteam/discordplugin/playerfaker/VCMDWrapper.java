package buttondevteam.discordplugin.playerfaker;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordSenderBase;
import buttondevteam.discordplugin.IMCPlayer;
import buttondevteam.lib.TBMCCoreAPI;
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
		try {
			Object ret;
			String mcpackage = Bukkit.getServer().getClass().getPackage().getName();
			if (mcpackage.contains("1_12"))
				ret = new VanillaCommandListener<>(player, bukkitplayer);
			else if (mcpackage.contains("1_14"))
				ret = new VanillaCommandListener14<>(player, bukkitplayer);
			else if (mcpackage.contains("1_15") || mcpackage.contains("1.16"))
				ret = VanillaCommandListener15.create(player, bukkitplayer); //bukkitplayer may be null but that's fine
			else
				ret = null;
			if (ret == null)
				compatWarning();
			return ret;
		} catch (NoClassDefFoundError | Exception e) {
			compatWarning();
			TBMCCoreAPI.SendException("Failed to create vanilla command listener", e);
			return null;
		}
	}

	private static void compatWarning() {
		DPUtils.getLogger().warning("Vanilla commands won't be available from Discord due to a compatibility error.");
	}
}
