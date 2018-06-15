package buttondevteam.discordplugin;

import buttondevteam.lib.TBMCCoreAPI;
import org.bukkit.Bukkit;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;
import sx.blah.discord.util.RequestBuffer.IRequest;
import sx.blah.discord.util.RequestBuffer.IVoidRequest;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

public final class DPUtils {

	public static EmbedBuilder embedWithHead(EmbedBuilder builder, String playername) {
		return builder.withAuthorIcon("https://minotar.net/avatar/" + playername + "/32.png");
	}

	/** Removes §[char] colour codes from strings */
	public static String sanitizeString(String string) {
		String sanitizedString = "";
		boolean random = false;
		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) == '§') {
				i++;// Skips the data value, the 4 in "§4Alisolarflare"
				if (string.charAt(i) == 'k')
					random = true;
				else
					random = false;
			} else {
				if (!random) // Skip random/obfuscated characters
					sanitizedString += string.charAt(i);
			}
		}
		return sanitizedString;
	}

	/**
	 * Performs Discord actions, retrying when ratelimited. May return null if action fails too many times or in safe mode.
	 */
    @Nullable
    public static <T> T perform(IRequest<T> action, long timeout, TimeUnit unit) {
        if (DiscordPlugin.SafeMode)
            return null;
        if (Thread.currentThread() == DiscordPlugin.mainThread) // TODO: Ignore shutdown message <--
            // throw new RuntimeException("Tried to wait for a Discord request on the main thread. This could cause lag.");
            Bukkit.getLogger().warning("Waiting for a Discord request on the main thread!");
        try {
            return RequestBuffer.request(action).get(timeout, unit); // Let the pros handle this
        } catch (Exception e) {
            TBMCCoreAPI.SendException("Couldn't perform Discord action!", e);
            return null;
        }
    }

    /**
     * Performs Discord actions, retrying when ratelimited. May return null if action fails too many times or in safe mode.
     */
    @Nullable
    public static <T> T perform(IRequest<T> action) {
        if (DiscordPlugin.SafeMode)
            return null;
        if (Thread.currentThread() == DiscordPlugin.mainThread) // TODO: Ignore shutdown message <--
            // throw new RuntimeException("Tried to wait for a Discord request on the main thread. This could cause lag.");
            Bukkit.getLogger().warning("Waiting for a Discord request on the main thread!");
        return RequestBuffer.request(action).get(); // Let the pros handle this
    }

	/**
	 * Performs Discord actions, retrying when ratelimited.
	 */
	public static Void perform(IVoidRequest action) {
		if (DiscordPlugin.SafeMode)
			return null;
		if (Thread.currentThread() == DiscordPlugin.mainThread)
			throw new RuntimeException("Tried to wait for a Discord request on the main thread. This could cause lag.");
		return RequestBuffer.request(action).get(); // Let the pros handle this
	}

	public static void performNoWait(IVoidRequest action) {
		if (DiscordPlugin.SafeMode)
			return;
		RequestBuffer.request(action);
	}

	public static <T> void performNoWait(IRequest<T> action) {
		if (DiscordPlugin.SafeMode)
			return;
		RequestBuffer.request(action);
	}

}
