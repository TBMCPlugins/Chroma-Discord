package buttondevteam.discordplugin;

import sx.blah.discord.handle.obj.IDiscordObject;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

@FunctionalInterface
public interface DiscordSupplier<T extends IDiscordObject<T>> {
	public abstract T get() throws DiscordException, RateLimitException, MissingPermissionsException;
}
