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

import buttondevteam.discordplugin.listeners.ChatListener;
import buttondevteam.discordplugin.listeners.CommandListener;
import buttondevteam.discordplugin.listeners.ExceptionListener;
import buttondevteam.discordplugin.listeners.MCListener;
import buttondevteam.discordplugin.mccommands.DiscordMCCommandBase;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.chat.TBMCChatAPI;
import sx.blah.discord.api.*;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
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
				reader.close();
				file.delete();
			} else {
				lastannouncementtime = getConfig().getLong("lastannouncementtime");
				lastseentime = getConfig().getLong("lastseentime");
				Test = getConfig().getBoolean("test", true);
				saveConfig();
			}
			ClientBuilder cb = new ClientBuilder();
			cb.withToken(Files.readFirstLine(new File("TBMC", "Token.txt"), StandardCharsets.UTF_8));
			dc = cb.login();
			dc.getDispatcher().registerListener(this);
			for (IListener<?> listener : CommandListener.getListeners())
				dc.getDispatcher().registerListener(listener);
			dc.getDispatcher().registerListener(new ChatListener());
			Bukkit.getPluginManager().registerEvents(new ExceptionListener(), this);
			TBMCCoreAPI.RegisterEventsForExceptions(new MCListener(), this);
			TBMCChatAPI.AddCommands(this, DiscordMCCommandBase.class);
		} catch (Exception e) {
			e.printStackTrace();
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}

	public static IChannel botchannel;
	public static IChannel annchannel;
	public static IChannel genchannel;
	public static IChannel chatchannel;

	public static boolean Test = true;

	@Override
	public void handle(ReadyEvent event) {
		try {
			IGuild mainServer;
			IGuild devServer;
			do {
				mainServer = event.getClient().getGuildByID("125813020357165056");
				devServer = event.getClient().getGuildByID("219529124321034241");
				Thread.sleep(100);
			} while (mainServer == null || devServer == null);
			if (!Test) {
				botchannel = mainServer.getChannelByID("209720707188260864"); // bot
				annchannel = mainServer.getChannelByID("126795071927353344"); // announcements
				genchannel = mainServer.getChannelByID("125813020357165056"); // general
				chatchannel = mainServer.getChannelByID("249663564057411596"); // minecraft_chat
				dc.changeStatus(Status.game("on TBMC"));
			} else {
				botchannel = devServer.getChannelByID("239519012529111040"); // bottest
				annchannel = devServer.getChannelByID("239519012529111040"); // bottest
				genchannel = devServer.getChannelByID("239519012529111040"); // bottest
				chatchannel = devServer.getChannelByID("248185455508455424"); // minecraft_chat_test
				dc.changeStatus(Status.game("testing"));
			}
			// sendMessageToChannel(botchannel, "Minecraft server started up");
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
			TBMCCoreAPI.SendUnsentExceptions();
			TBMCCoreAPI.SendUnsentDebugMessages();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDisable() {
		stop = true;
		getConfig().set("lastannouncementtime", lastannouncementtime);
		getConfig().set("lastseentime", lastseentime);
		saveConfig();
		try {
			dc.changeStatus(Status.game("on TBMC"));
			dc.logout();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private long lastannouncementtime = 0;
	private long lastseentime = 0;

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
					JsonElement distinguishedjson = data.get("distinguished");
					String distinguished;
					if (distinguishedjson.isJsonNull())
						distinguished = null;
					else
						distinguished = distinguishedjson.getAsString();
					String permalink = "https://www.reddit.com" + data.get("permalink").getAsString();
					long date = data.get("created_utc").getAsLong();
					if (date > lastseentime)
						lastseentime = date;
					else if (date > lastannouncementtime) {
						(distinguished != null && distinguished.equals("moderator") ? modmsgsb : msgsb)
								.append("A new post was submitted to the subreddit by ").append(author).append("\n")
								.append(permalink).append("\n");
						lastanntime = date;
					}
				}
				if (msgsb.length() > 0)
					genchannel.pin(sendMessageToChannel(genchannel, msgsb.toString()));
				if (modmsgsb.length() > 0)
					sendMessageToChannel(annchannel, modmsgsb.toString());
				if (lastannouncementtime != lastanntime) {
					lastannouncementtime = lastanntime; // If sending succeeded
					getConfig().set("lastannouncementtime", lastannouncementtime);
					getConfig().set("lastseentime", lastseentime);
					saveConfig();
				}
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

	public static IMessage sendMessageToChannel(IChannel channel, String message) {
		for (int i = 0; i < 10; i++) {
			try {
				Thread.sleep(i * 100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				return channel.sendMessage(message);
			} catch (Exception e) {
				if (i == 9) {
					Bukkit.getLogger().warning("Failed to deliver message to Discord! Channel: " + channel.getName()
							+ " Message: " + message);
					throw new RuntimeException(e);
				} else
					continue;
			}
		}
		return null;
	}
}
