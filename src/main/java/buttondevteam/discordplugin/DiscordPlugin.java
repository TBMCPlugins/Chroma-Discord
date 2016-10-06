package buttondevteam.discordplugin;

import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.base.Charsets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import buttondevteam.bucket.core.TBMCCoreAPI;
import sx.blah.discord.api.*;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IChannel;

/**
 * Hello world!
 *
 */
public class DiscordPlugin extends JavaPlugin implements IListener<ReadyEvent> {
	private static final String SubredditURL = "https://www.reddit.com/r/ChromaGamers";
	private static boolean stop = false;

	@Override
	public void onEnable() {
		try {
			Bukkit.getLogger().info("Initializing DiscordPlugin...");
			ClientBuilder cb = new ClientBuilder();
			cb.withToken(IOUtils.toString(getClass().getResourceAsStream("/Token.txt"), Charsets.UTF_8));
			IDiscordClient dc = cb.login();
			dc.getDispatcher().registerListener(this);
		} catch (Exception e) {
			e.printStackTrace();
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}

	private IChannel channel;

	@Override
	public void handle(ReadyEvent event) {
		try {
			channel = event.getClient().getGuilds().get(0).getChannelsByName("bot").get(0);
			channel.sendMessage("Minecraft server started up");
			Runnable r = new Runnable() {
				public void run() {
					AnnouncementGetterThreadMethod();
				}
			};
			Thread t = new Thread(r);
			t.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDisable() {
		stop = true;
	}

	private void AnnouncementGetterThreadMethod() {
		while (!stop) {
			try {
				String body = TBMCCoreAPI.DownloadString(SubredditURL + ".json?limit=10"); // TODO: Save last announcement
				JsonArray json = new JsonParser().parse(body).getAsJsonArray().get(1).getAsJsonObject().get("data")
						.getAsJsonObject().get("children").getAsJsonArray();
				for (Object obj : json) {
					JsonObject item = (JsonObject) obj;
					String author = item.get("data").getAsJsonObject().get("author").getAsString();
					String post = item.get("data").getAsJsonObject().get("body").getAsString();
					System.out.println("author: " + author);
					System.out.println("post: " + post);
					try {
						Thread.sleep(10);
					} catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
				}
				try {
					Thread.sleep(10000);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
