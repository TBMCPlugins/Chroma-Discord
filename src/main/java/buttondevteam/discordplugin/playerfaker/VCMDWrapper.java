package buttondevteam.discordplugin.playerfaker;

import buttondevteam.discordplugin.DiscordSenderBase;
import buttondevteam.discordplugin.IMCPlayer;
import buttondevteam.lib.TBMCCoreAPI;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

@RequiredArgsConstructor
public class VCMDWrapper {
	@Getter //Needed to mock the player
	@Nullable
	private final Object listener;

	/**
	 * This constructor will only send raw vanilla messages to the sender in plain text.
	 *
	 * @param player The Discord sender player (the wrapper)
	 */
	public static <T extends DiscordSenderBase & IMCPlayer<T>> Object createListener(T player, MinecraftChatModule module) {
		return createListener(player, null, module);
	}

	/**
	 * This constructor will send both raw vanilla messages to the sender in plain text and forward the raw message to the provided player.
	 *
	 * @param player       The Discord sender player (the wrapper)
	 * @param bukkitplayer The Bukkit player to send the raw message to
	 * @param module       The Minecraft chat module
	 */
	public static <T extends DiscordSenderBase & IMCPlayer<T>> Object createListener(T player, Player bukkitplayer, MinecraftChatModule module) {
		try {
			Object ret;
			String mcpackage = Bukkit.getServer().getClass().getPackage().getName();
			if (mcpackage.contains("1_12"))
				ret = new VanillaCommandListener<>(player, bukkitplayer);
			else if (mcpackage.contains("1_14"))
				ret = new VanillaCommandListener14<>(player, bukkitplayer);
			else if (mcpackage.contains("1_15") || mcpackage.contains("1_16"))
				ret = VanillaCommandListener15.create(player, bukkitplayer); //bukkitplayer may be null but that's fine
			else
				ret = null;
			if (ret == null)
				compatWarning(module);
			return ret;
		} catch (NoClassDefFoundError | Exception e) {
			compatWarning(module);
			TBMCCoreAPI.SendException("Failed to create vanilla command listener", e, module);
			return null;
		}
	}

	private static void compatWarning(MinecraftChatModule module) {
		module.logWarn("Vanilla commands won't be available from Discord due to a compatibility error. Disable vanilla command support to remove this message.");
	}

	static boolean compatResponse(DiscordSenderBase dsender) {
		dsender.sendMessage("Vanilla commands are not supported on this Minecraft version.");
		return true;
	}
}
