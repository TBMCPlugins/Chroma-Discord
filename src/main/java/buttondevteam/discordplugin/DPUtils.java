package buttondevteam.discordplugin;

import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.architecture.IHaveConfig;
import discord4j.core.object.entity.*;
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

	public static String escape(String message) {
		return message.replaceAll("([*_~])", Matcher.quoteReplacement("\\") + "$1");
	}

	public static Logger getLogger() {
		if (DiscordPlugin.plugin == null || DiscordPlugin.plugin.getLogger() == null)
			return Logger.getLogger("DiscordPlugin");
		return DiscordPlugin.plugin.getLogger();
	}

	public static ConfigData<MessageChannel> channelData(IHaveConfig config, String key, long defID) {
		return config.getDataPrimDef(key, defID, id -> {
			Channel ch = DiscordPlugin.dc.getChannelById(Snowflake.of((long) id)).onErrorResume(e -> {
				getLogger().warning("Failed to get channel data for " + key + "=" + id + " - " + e.getMessage());
				return Mono.empty();
			}).block();
			if (ch instanceof MessageChannel)
				return (MessageChannel) ch;
			else
				return null;
		}, ch -> ch.getId().asLong()); //We can afford to search for the channel in the cache once (instead of using mainServer)
	}

	public static ConfigData<Role> roleData(IHaveConfig config, String key, String defName) {
		return roleData(config, key, defName, DiscordPlugin.mainServer);
	}

	public static ConfigData<Role> roleData(IHaveConfig config, String key, String defName, Guild guild) {
		return config.getDataPrimDef(key, defName, name -> {
			if (!(name instanceof String)) return null;
			return guild.getRoles().filter(r -> r.getName().equals(name)).blockFirst();
		}, r -> r.getId().asLong());
	}

	/**
	 * Mentions the <b>bot channel</b>. Useful for help texts.
	 *
	 * @return The string for mentioning the channel
	 */
	public static String botmention() {
		Channel channel;
		if (DiscordPlugin.plugin == null
			|| (channel = DiscordPlugin.plugin.CommandChannel().get()) == null) return "#bot";
		return channel.getMention();
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
			if (config.get() == null) {
				String path = null;
				try {
					if (component != null)
						Component.setComponentEnabled(component, false);
					val f = ConfigData.class.getDeclaredField("path");
					f.setAccessible(true); //Hacking my own plugin
					path = (String) f.get(config);
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

}
