package buttondevteam.discordplugin;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.Files;
import com.google.gson.*;

import buttondevteam.discordplugin.listeners.*;
import buttondevteam.discordplugin.mccommands.DiscordMCCommandBase;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.chat.TBMCChatAPI;
import net.milkbowl.vault.permission.Permission;
import sx.blah.discord.api.*;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RateLimitException;

/**
 * Hello world!
 *
 */
public class DiscordPlugin extends JavaPlugin implements IListener<ReadyEvent> {
	private static final String SubredditURL = "https://www.reddit.com/r/ChromaGamers";
	private static boolean stop = false;
	public static IDiscordClient dc;
	public static DiscordPlugin plugin;

	@Override
	public void onEnable() {
		try {
			Bukkit.getLogger().info("Initializing DiscordPlugin...");
			plugin = this;
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
				saveConfig();
			}
			ClientBuilder cb = new ClientBuilder();
			cb.withToken(Files.readFirstLine(new File("TBMC", "Token.txt"), StandardCharsets.UTF_8));
			dc = cb.login();
			dc.getDispatcher().registerListener(this);
			for (IListener<?> listener : CommandListener.getListeners())
				dc.getDispatcher().registerListener(listener);
			MCChatListener mcchat = new MCChatListener();
			dc.getDispatcher().registerListener(mcchat);
			TBMCCoreAPI.RegisterEventsForExceptions(mcchat, this);
			dc.getDispatcher().registerListener(new AutoUpdaterListener());
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
	public static IChannel issuechannel;
	public static IChannel botroomchannel;
	public static IChannel officechannel;
	public static IChannel coffeechannel;
	public static IGuild mainServer;
	public static IGuild devServer;

	@Override
	public void handle(ReadyEvent event) {
		try {
			int retryc = 0;
			do {
				mainServer = event.getClient().getGuildByID("125813020357165056");
				devServer = event.getClient().getGuildByID("219529124321034241");
				Thread.sleep(100);
			} while ((mainServer == null || devServer == null) && retryc++ < 10);
			if (!TBMCCoreAPI.IsTestServer()) {
				botchannel = mainServer.getChannelByID("209720707188260864"); // bot
				annchannel = mainServer.getChannelByID("126795071927353344"); // announcements
				genchannel = mainServer.getChannelByID("125813020357165056"); // general
				chatchannel = mainServer.getChannelByID("249663564057411596"); // minecraft_chat
				issuechannel = devServer.getChannelByID("219643416496046081"); // server-issues
				botroomchannel = devServer.getChannelByID("239519012529111040"); // bot-room
				officechannel = devServer.getChannelByID("219626707458457603"); // developers-office
				coffeechannel = devServer.getChannelByID("219530035365675010"); // coffee-table
				dc.changeStatus(Status.game("on TBMC"));
			} else {
				botchannel = devServer.getChannelByID("239519012529111040"); // bot-room
				annchannel = botchannel; // bot-room
				genchannel = botchannel; // bot-room
				botroomchannel = botchannel;// bot-room
				issuechannel = botchannel; // bot-room
				chatchannel = devServer.getChannelByID("248185455508455424"); // minecraft_chat_test
				officechannel = botchannel; // bot-room
				coffeechannel = botchannel; // bot-room
				dc.changeStatus(Status.game("testing"));
			}
			Bukkit.getScheduler().runTaskAsynchronously(this, () -> sendMessageToChannel(chatchannel, "",
					new EmbedBuilder().withColor(Color.GREEN).withTitle("Server started - chat connected.").build()));
			Runnable r = new Runnable() {
				public void run() {
					AnnouncementGetterThreadMethod();
				}
			};
			Thread t = new Thread(r);
			t.start();
			List<IMessage> msgs = genchannel.getPinnedMessages();
			for (int i = msgs.size() - 1; i >= 10; i--) { // Unpin all pinned messages except the newest 10
				genchannel.unpin(msgs.get(i));
			}
			setupProviders();
			TBMCCoreAPI.SendUnsentExceptions();
			TBMCCoreAPI.SendUnsentDebugMessages();
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while enabling DiscordPlugin!", e);
		}
	}

	/**
	 * Always true, except when running "stop" from console
	 */
	public static boolean Restart;

	@Override
	public void onDisable() {
		stop = true;
		getConfig().set("lastannouncementtime", lastannouncementtime);
		getConfig().set("lastseentime", lastseentime);
		saveConfig();
		sendMessageToChannel(chatchannel, "", new EmbedBuilder().withColor(Restart ? Color.ORANGE : Color.RED)
				.withTitle(Restart ? "Server restarting" : "Server stopping").build());
		try {
			dc.changeStatus(Status.game("on TBMC"));
			dc.logout();
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while disabling DiscordPlugin!", e);
		}
	}

	private long lastannouncementtime = 0;
	private long lastseentime = 0;
	public static final String DELIVERED_REACTION = "✅";

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
		return sendMessageToChannel(channel, message, null);
	}

	public static IMessage sendMessageToChannel(IChannel channel, String message, EmbedObject embed) {
		if (message.length() > 1900) {
			message = message.substring(0, 1900);
			Bukkit.getLogger()
					.warning("Message was too long to send to discord and got truncated. In " + channel.getName());
		}
		for (int i = 0; i < 10; i++) {
			try {
				Thread.sleep(i * 100);
			} catch (InterruptedException e2) {
				e2.printStackTrace();
			}
			try {
				final String content = TBMCCoreAPI.IsTestServer() && channel != chatchannel
						? "*The following message is from a test server*\n" + message : message;
				return embed == null ? channel.sendMessage(content) : channel.sendMessage(content, embed, false);
			} catch (RateLimitException e) {
				try {
					Thread.sleep(e.getRetryDelay());
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
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

	public static Permission perms;

	public boolean setupProviders() {
		try {
			Class.forName("net.milkbowl.vault.permission.Permission");
			Class.forName("net.milkbowl.vault.chat.Chat");
		} catch (ClassNotFoundException e) {
			return false;
		}

		RegisteredServiceProvider<Permission> permsProvider = Bukkit.getServer().getServicesManager()
				.getRegistration(Permission.class);
		perms = permsProvider.getProvider();
		return perms != null;
	}

	/** Removes §[char] colour codes from strings */
	public static String sanitizeString(String string) {
		String sanitizedString = "";
		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) == '§') {
				i++;// Skips the data value, the 4 in "§4Alisolarflare"
			} else {
				sanitizedString += string.charAt(i);
			}
		}
		return sanitizedString;
	}
}
