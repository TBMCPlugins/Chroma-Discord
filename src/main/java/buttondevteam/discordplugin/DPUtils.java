package buttondevteam.discordplugin;

import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.architecture.IHaveConfig;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Snowflake;
import lombok.val;
import sx.blah.discord.util.EmbedBuilder;

import javax.annotation.Nullable;
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

    public static String escape(String message) {
        return message.replaceAll("([*_~])", Matcher.quoteReplacement("\\")+"$1");
    }

	public static Logger getLogger() {
		if (DiscordPlugin.plugin == null || DiscordPlugin.plugin.getLogger() == null)
			return Logger.getLogger("DiscordPlugin");
		return DiscordPlugin.plugin.getLogger();
	}

	public static ConfigData<Channel> channelData(IHaveConfig config, String key, long defID) {
		return config.getDataPrimDef(key, defID, id -> DiscordPlugin.dc.getChannelById(Snowflake.of((long) id)).block(), ch -> ch.getId().asLong()); //We can afford to search for the channel in the cache once (instead of using mainServer)
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

}
