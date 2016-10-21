package buttondevteam.discordplugin;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import buttondevteam.bucket.core.TBMCCoreAPI;
import sx.blah.discord.api.*;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Status;

/**
 * Hello world!
 *
 */
public class DiscordPlugin extends JavaPlugin implements IListener<ReadyEvent> {
	private static final String SubredditURL = "https://www.reddit.com/r/ChromaGamers";
	private static boolean stop = false;
	public static IDiscordClient dc;

	@Override
	public void onEnable() {
		try {
			Bukkit.getLogger().info("Initializing DiscordPlugin...");
			final File file = new File("TBMC", "DiscordRedditLastAnnouncement.txt");
			if (file.exists()) {
				BufferedReader reader = Files.newReader(file, StandardCharsets.UTF_8);
				String line = reader.readLine();
				lastannouncementtime = Long.parseLong(line);
			}
			ClientBuilder cb = new ClientBuilder();
			cb.withToken(Files.readFirstLine(new File("TBMC", "Token.txt"), StandardCharsets.UTF_8));
			dc = cb.login();
			dc.getDispatcher().registerListener(this);
			for (IListener<?> listener : CommandListener.getListeners())
				dc.getDispatcher().registerListener(listener);
			Bukkit.getPluginManager().registerEvents(new MCListener(), this);
		} catch (Exception e) {
			e.printStackTrace();
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}

	public static IChannel botchannel;
	public static IChannel annchannel;
	public static IChannel genchannel;

	@Override
	public void handle(ReadyEvent event) {
		try {
			botchannel = event.getClient().getGuilds().get(0).getChannelByID("209720707188260864"); // bot
			annchannel = event.getClient().getGuilds().get(0).getChannelByID("126795071927353344"); // announcements
			genchannel = event.getClient().getGuilds().get(0).getChannelByID("125813020357165056"); // general
			dc.changeStatus(Status.game("on TBMC"));
			botchannel.sendMessage("Minecraft server started up");
			Runnable r = new Runnable() {
				public void run() {
					AnnouncementGetterThreadMethod();
				}
			};
			Thread t = new Thread(r);
			t.start();
			List<IMessage> msgs = genchannel.getPinnedMessages();
			for (int i = msgs.size() - 1; i >= 10; i--) {
				genchannel.unpin(msgs.get(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDisable() {
		stop = true;
		try {
			dc.logout();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private long lastannouncementtime = 0;

	private void AnnouncementGetterThreadMethod() {
		while (!stop) {
			try {
				String body = TBMCCoreAPI.DownloadString(SubredditURL + "/new/.json?limit=10");
				JsonArray json = new JsonParser().parse(body).getAsJsonObject().get("data").getAsJsonObject()
						.get("children").getAsJsonArray();
				StringBuilder msgsb = new StringBuilder();
				StringBuilder modmsgsb = new StringBuilder();
				long lastanntime = lastannouncementtime;
				for (int i = json.size() - 1; i >= 0; i--) {
					JsonObject item = json.get(i).getAsJsonObject();
					final JsonObject data = item.get("data").getAsJsonObject();
					String author = data.get("author").getAsString();
					// String title = data.get("title").getAsString();
					// String stickied = data.get("stickied").getAsString();
					JsonElement flairjson = data.get("link_flair_text");
					String flair;
					if (flairjson.isJsonNull())
						flair = null;
					else
						flair = flairjson.getAsString();
					JsonElement distinguishedjson = data.get("distinguished");
					String distinguished;
					if (distinguishedjson.isJsonNull())
						distinguished = null;
					else
						distinguished = distinguishedjson.getAsString();
					String permalink = "https://www.reddit.com" + data.get("permalink").getAsString();
					long date = data.get("created_utc").getAsLong();
					if (date <= lastannouncementtime)
						continue;
					(distinguished != null && distinguished.equals("moderator") ? modmsgsb : msgsb)
							.append("A new post was submitted to the subreddit by ").append(author).append("\n")
							.append(permalink).append("\n");
					lastanntime = date;
				}
				if (msgsb.length() > 0)
					genchannel.pin(genchannel.sendMessage(msgsb.toString()));
				if (modmsgsb.length() > 0) // TODO: Wait for distinguish
					annchannel.sendMessage(modmsgsb.toString());
				lastannouncementtime = lastanntime; // If sending succeeded
				File file = new File("TBMC", "DiscordRedditLastAnnouncement.txt");
				Files.write(lastannouncementtime + "", file, StandardCharsets.UTF_8);
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(10000);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}
}
