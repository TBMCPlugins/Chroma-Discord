package buttondevteam.discordplugin;

import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

@FunctionalInterface
public interface DiscordRunnable {
	public abstract void run() throws DiscordException, RateLimitException, MissingPermissionsException;
}
