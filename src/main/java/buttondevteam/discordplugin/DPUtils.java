package buttondevteam.discordplugin;

import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.architecture.IHaveConfig;
import buttondevteam.lib.architecture.ReadOnlyConfigData;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.val;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Optional;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public final class DPUtils {

	private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S*");
	private static final Pattern FORMAT_PATTERN = Pattern.compile("[*_~]");

	public static EmbedCreateSpec embedWithHead(EmbedCreateSpec ecs, String displayname, String playername, String profileUrl) {
		return ecs.setAuthor(displayname, profileUrl, "https://minotar.net/avatar/" + playername + "/32.png");
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
		StringBuilder sanitizedString = new StringBuilder();
		boolean random = false;
		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) == '§') {
				i++;// Skips the data value, the 4 in "§4Alisolarflare"
				random = string.charAt(i) == 'k';
			} else {
				if (!random) // Skip random/obfuscated characters
					sanitizedString.append(string.charAt(i));
			}
		}
		return sanitizedString.toString();
	}

	private static String escape(String message) {
		//var ts = new TreeSet<>();
		var ts = new TreeSet<int[]>(Comparator.comparingInt(a -> a[0])); //Compare the start, then check the end
		var matcher = URL_PATTERN.matcher(message);
		while (matcher.find())
			ts.add(new int[]{matcher.start(), matcher.end()});
		matcher = FORMAT_PATTERN.matcher(message);
		/*Function<MatchResult, String> aFunctionalInterface = result ->
			Optional.ofNullable(ts.floor(new int[]{result.start(), 0})).map(a -> a[1]).orElse(0) < result.start()
				? "\\\\" + result.group() : result.group();
		return matcher.replaceAll(aFunctionalInterface); //Find nearest URL match and if it's not reaching to the char then escape*/
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(sb, Optional.ofNullable(ts.floor(new int[]{matcher.start(), 0})) //Find a URL start <= our start
				.map(a -> a[1]).orElse(-1) < matcher.start() //Check if URL end < our start
				? "\\\\" + matcher.group() : matcher.group());
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	public static Logger getLogger() {
		if (DiscordPlugin.plugin == null || DiscordPlugin.plugin.getLogger() == null)
			return Logger.getLogger("DiscordPlugin");
		return DiscordPlugin.plugin.getLogger();
	}

	public static ReadOnlyConfigData<Mono<MessageChannel>> channelData(IHaveConfig config, String key) {
		return config.getReadOnlyDataPrimDef(key, 0L, id -> getMessageChannel(key, Snowflake.of((Long) id)), ch -> 0L); //We can afford to search for the channel in the cache once (instead of using mainServer)
	}

	public static ReadOnlyConfigData<Mono<Role>> roleData(IHaveConfig config, String key, String defName) {
		return roleData(config, key, defName, Mono.just(DiscordPlugin.mainServer));
	}

	/**
	 * Needs to be a {@link ConfigData} for checking if it's set
	 */
	public static ReadOnlyConfigData<Mono<Role>> roleData(IHaveConfig config, String key, String defName, Mono<Guild> guild) {
		return config.getReadOnlyDataPrimDef(key, defName, name -> {
			if (!(name instanceof String) || ((String) name).length() == 0) return Mono.empty();
			return guild.flatMapMany(Guild::getRoles).filter(r -> r.getName().equals(name)).onErrorResume(e -> {
				getLogger().warning("Failed to get role data for " + key + "=" + name + " - " + e.getMessage());
				return Mono.empty();
			}).next();
		}, r -> defName);
	}

	public static ReadOnlyConfigData<Snowflake> snowflakeData(IHaveConfig config, String key, long defID) {
		return config.getReadOnlyDataPrimDef(key, defID, id -> Snowflake.of((long) id), Snowflake::asLong);
	}

	/**
	 * Mentions the <b>bot channel</b>. Useful for help texts.
	 *
	 * @return The string for mentioning the channel
	 */
	public static String botmention() {
		if (DiscordPlugin.plugin == null) return "#bot";
		return channelMention(DiscordPlugin.plugin.commandChannel().get());
	}

	/**
	 * Disables the component if one of the given configs return null. Useful for channel/role configs.
	 *
	 * @param component The component to disable if needed
	 * @param configs   The configs to check for null
	 * @return Whether the component got disabled and a warning logged
	 */
	public static boolean disableIfConfigError(@Nullable Component<DiscordPlugin> component, ConfigData<?>... configs) {
		for (val config : configs) {
			Object v = config.get();
			if (disableIfConfigErrorRes(component, config, v))
				return true;
		}
		return false;
	}

	/**
	 * Disables the component if one of the given configs return null. Useful for channel/role configs.
	 *
	 * @param component The component to disable if needed
	 * @param config    The (snowflake) config to check for null
	 * @param result    The result of getting the value
	 * @return Whether the component got disabled and a warning logged
	 */
	public static boolean disableIfConfigErrorRes(@Nullable Component<DiscordPlugin> component, ConfigData<?> config, Object result) {
		//noinspection ConstantConditions
		if (result == null || (result instanceof Mono<?> && !((Mono<?>) result).hasElement().block())) {
			String path = null;
			try {
				if (component != null)
					Component.setComponentEnabled(component, false);
				path = config.getPath();
			} catch (Exception e) {
				if (component != null)
					TBMCCoreAPI.SendException("Failed to disable component after config error!", e, component);
				else
					TBMCCoreAPI.SendException("Failed to disable component after config error!", e, DiscordPlugin.plugin);
			}
			getLogger().warning("The config value " + path + " isn't set correctly " + (component == null ? "in global settings!" : "for component " + component.getClass().getSimpleName() + "!"));
			getLogger().warning("Set the correct ID in the config" + (component == null ? "" : " or disable this component") + " to remove this message.");
			return true;
		}
		return false;
	}

	/**
	 * Send a response in the form of "@User, message". Use Mono.empty() if you don't have a channel object.
	 *
	 * @param original The original message to reply to
	 * @param channel  The channel to send the message in, defaults to the original
	 * @param message  The message to send
	 * @return A mono to send the message
	 */
	public static Mono<Message> reply(Message original, @Nullable MessageChannel channel, String message) {
		Mono<MessageChannel> ch;
		if (channel == null)
			ch = original.getChannel();
		else
			ch = Mono.just(channel);
		return reply(original, ch, message);
	}

	/**
	 * @see #reply(Message, MessageChannel, String)
	 */
	public static Mono<Message> reply(Message original, Mono<MessageChannel> ch, String message) {
		return ch.flatMap(chan -> chan.createMessage((original.getAuthor().isPresent()
			? original.getAuthor().get().getMention() + ", " : "") + message));
	}

	public static String nickMention(Snowflake userId) {
		return "<@!" + userId.asString() + ">";
	}

	public static String channelMention(Snowflake channelId) {
		return "<#" + channelId.asString() + ">";
	}

	/**
	 * Gets a message channel for a config. Returns empty for ID 0.
	 *
	 * @param key The config key
	 * @param id  The channel ID
	 * @return A message channel
	 */
	public static Mono<MessageChannel> getMessageChannel(String key, Snowflake id) {
		if (id.asLong() == 0L) return Mono.empty();
		return DiscordPlugin.dc.getChannelById(id).onErrorResume(e -> {
			getLogger().warning("Failed to get channel data for " + key + "=" + id + " - " + e.getMessage());
			return Mono.empty();
		}).filter(ch -> ch instanceof MessageChannel).cast(MessageChannel.class);
	}

	public static Mono<MessageChannel> getMessageChannel(ConfigData<Snowflake> config) {
		return getMessageChannel(config.getPath(), config.get());
	}

	public static <T> Mono<T> ignoreError(Mono<T> mono) {
		return mono.onErrorResume(t -> Mono.empty());
	}

}
