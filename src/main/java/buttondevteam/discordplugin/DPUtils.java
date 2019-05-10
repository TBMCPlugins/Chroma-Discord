package buttondevteam.discordplugin;

import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.architecture.IHaveConfig;
import buttondevteam.lib.architecture.ReadOnlyConfigData;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.val;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.logging.Logger;
import java.util.regex.Matcher;

public final class DPUtils {

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
		return message.replaceAll("([*_~])", Matcher.quoteReplacement("\\") + "$1");
	}

	public static Logger getLogger() {
		if (DiscordPlugin.plugin == null || DiscordPlugin.plugin.getLogger() == null)
			return Logger.getLogger("DiscordPlugin");
		return DiscordPlugin.plugin.getLogger();
	}

	public static ReadOnlyConfigData<Mono<MessageChannel>> channelData(IHaveConfig config, String key, long defID) {
		return config.getReadOnlyDataPrimDef(key, defID, id -> getMessageChannel(key, Snowflake.of((Long) id)), ch -> defID); //We can afford to search for the channel in the cache once (instead of using mainServer)
	}

	public static ReadOnlyConfigData<Mono<Role>> roleData(IHaveConfig config, String key, String defName) {
		return roleData(config, key, defName, Mono.just(DiscordPlugin.mainServer));
	}

	/**
	 * Needs to be a {@link ConfigData} for checking if it's set
	 */
	public static ReadOnlyConfigData<Mono<Role>> roleData(IHaveConfig config, String key, String defName, Mono<Guild> guild) {
		return config.getReadOnlyDataPrimDef(key, defName, name -> {
			if (!(name instanceof String)) return Mono.empty();
			return guild.flatMapMany(Guild::getRoles).filter(r -> r.getName().equals(name)).next();
		}, r -> defName);
	}

	public static ConfigData<Snowflake> snowflakeData(IHaveConfig config, String key, long defID) {
		return config.getDataPrimDef(key, defID, id -> Snowflake.of((long) id), Snowflake::asLong);
	}

	/**
	 * Mentions the <b>bot channel</b>. Useful for help texts.
	 *
	 * @return The string for mentioning the channel
	 */
	public static String botmention() {
		if (DiscordPlugin.plugin == null) return "#bot";
		return channelMention(DiscordPlugin.plugin.CommandChannel().get());
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
			//noinspection ConstantConditions
			if (v == null || (v instanceof Mono<?> && !((Mono<?>) v).hasElement().block())) {
				String path = null;
				try {
					if (component != null)
						Component.setComponentEnabled(component, false);
					path = config.getPath();
				} catch (Exception e) {
					TBMCCoreAPI.SendException("Failed to disable component after config error!", e);
				}
				getLogger().warning("The config value " + path + " isn't set correctly " + (component == null ? "in global settings!" : "for component " + component.getClass().getSimpleName() + "!"));
				getLogger().warning("Set the correct ID in the config" + (component == null ? "" : " or disable this component") + " to remove this message.");
				return true;
			}
		}
		return false;
	}

	public static Mono<Message> reply(Message original, @Nullable MessageChannel channel, String message) {
		Mono<MessageChannel> ch;
		if (channel == null)
			ch = original.getChannel();
		else
			ch = Mono.just(channel);
		return ch.flatMap(chan -> chan.createMessage((original.getAuthor().isPresent()
			? original.getAuthor().get().getMention() + ", " : "") + message));
	}

	public static String nickMention(Snowflake userId) {
		return "<@!" + userId.asString() + ">";
	}

	public static String channelMention(Snowflake channelId) {
		return "<#" + channelId.asString() + ">";
	}

	public static Mono<MessageChannel> getMessageChannel(String key, Snowflake id) {
		return DiscordPlugin.dc.getChannelById(id).onErrorResume(e -> {
			getLogger().warning("Failed to get channel data for " + key + "=" + id + " - " + e.getMessage());
			return Mono.empty();
		}).filter(ch -> ch instanceof MessageChannel).cast(MessageChannel.class);
	}

	public static Mono<MessageChannel> getMessageChannel(ConfigData<Snowflake> config) {
		return getMessageChannel(config.getPath(), config.get());
	}

}
