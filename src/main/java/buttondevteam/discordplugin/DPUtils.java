package buttondevteam.discordplugin;

import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.architecture.IHaveConfig;
import org.bukkit.Bukkit;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IIDLinkedObject;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;
import sx.blah.discord.util.RequestBuffer.IRequest;
import sx.blah.discord.util.RequestBuffer.IVoidRequest;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

public final class DPUtils {

	public static EmbedBuilder embedWithHead(EmbedBuilder builder, String playername) {
		return builder.withAuthorIcon("https://minotar.net/avatar/" + playername + "/32.png");
	}

    /**
     * Removes §[char] colour codes from strings & escapes them for Discord <br>
     * Ensure that this method only gets called once (escaping)
     */
    public static String sanitizeString(String string) {
        return escape(sanitizeStringNoEscape(string));
    }

    /**
     * Removes §[char] colour codes from strings
     */
    public static String sanitizeStringNoEscape(String string) {
        String sanitizedString = "";
        boolean random = false;
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) == '§') {
                i++;// Skips the data value, the 4 in "§4Alisolarflare"
                random = string.charAt(i) == 'k';
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
    public static <T> T perform(IRequest<T> action, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        if (DiscordPlugin.SafeMode)
            return null;
		if (Bukkit.isPrimaryThread()) // TODO: Ignore shutdown message <--
            // throw new RuntimeException("Tried to wait for a Discord request on the main thread. This could cause lag.");
            Bukkit.getLogger().warning("Waiting for a Discord request on the main thread!");
        return RequestBuffer.request(action).get(timeout, unit); // Let the pros handle this
    }

    /**
     * Performs Discord actions, retrying when ratelimited. May return null if action fails too many times or in safe mode.
     */
    @Nullable
    public static <T> T perform(IRequest<T> action) {
        if (DiscordPlugin.SafeMode)
            return null;
		if (Bukkit.isPrimaryThread()) // TODO: Ignore shutdown message <--
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
		if (Bukkit.isPrimaryThread())
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

    public static String escape(String message) {
        return message.replaceAll("([*_~])", Matcher.quoteReplacement("\\")+"$1");
    }

	public static Logger getLogger() {
		if (DiscordPlugin.plugin == null || DiscordPlugin.plugin.getLogger() == null)
			return Logger.getLogger("DiscordPlugin");
		return DiscordPlugin.plugin.getLogger();
	}

	public static ConfigData<IChannel> channelData(IHaveConfig config, String key, long defID) {
		return config.getDataPrimDef(key, defID, id -> DiscordPlugin.dc.getChannelByID((long) id), IIDLinkedObject::getLongID); //We can afford to search for the channel in the cache once (instead of using mainServer)
	}

	public static ConfigData<IRole> roleData(IHaveConfig config, String key, long defID) {
		return config.getDataPrimDef(key, defID, id -> DiscordPlugin.dc.getRoleByID((long) id), IIDLinkedObject::getLongID); //We can afford to search for the channel in the cache once (instead of using mainServer)
	}

	/**
	 * Mentions the <b>bot channel</b>. Useful for help texts.
	 *
	 * @return The string for mentioning the channel
	 */
	public static String botmention() {
		if (DiscordPlugin.plugin == null) return "#bot";
		return DiscordPlugin.plugin.CommandChannel().get().mention();
	}

}
