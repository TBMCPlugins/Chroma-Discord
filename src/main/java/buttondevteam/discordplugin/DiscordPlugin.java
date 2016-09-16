package buttondevteam.discordplugin;

import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.base.Charsets;
import sx.blah.discord.api.ClientBuilder;

/**
 * Hello world!
 *
 */
public class DiscordPlugin extends JavaPlugin {
	@Override
	public void onEnable() {
		try {
			Bukkit.getLogger().info("Initializing DiscordPlugin...");
			ClientBuilder cb = new ClientBuilder();
			cb.withToken(IOUtils.toString(getClass().getResourceAsStream("Token.txt"), Charsets.UTF_8));
			cb.login();
		} catch (Exception e) {
			e.printStackTrace();
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}
}
