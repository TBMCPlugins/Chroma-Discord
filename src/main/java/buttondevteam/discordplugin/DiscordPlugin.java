package buttondevteam.discordplugin;

import java.awt.Color;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

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
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

public class DiscordPlugin extends JavaPlugin implements IListener<ReadyEvent> {
	private static final String SubredditURL = "https://www.reddit.com/r/ChromaGamers";
	private static boolean stop = false;
	public static IDiscordClient dc;
	public static DiscordPlugin plugin;
	public static boolean SafeMode = true;
	public static List<String> GameRoles;

	@SuppressWarnings("unchecked")
	@Override
	public void onEnable() {
		try {
			Bukkit.getLogger().info("Initializing DiscordPlugin...");
			plugin = this;
			lastannouncementtime = getConfig().getLong("lastannouncementtime");
			lastseentime = getConfig().getLong("lastseentime");
			GameRoles = (List<String>) getConfig().getList("gameroles", new ArrayList<String>());
			saveConfig();
			ClientBuilder cb = new ClientBuilder();
			cb.withToken(Files.readFirstLine(new File("TBMC", "Token.txt"), StandardCharsets.UTF_8));
			dc = cb.login();
			dc.getDispatcher().registerListener(this);
		} catch (Exception e) {
			e.printStackTrace();
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}

	public static IChannel botchannel;
	public static IChannel annchannel;
	public static IChannel genchannel;
	public static IChannel chatchannel;
	public static IChannel botroomchannel;
	/**
	 * Don't send messages, just receive, the same channel is used when testing
	 */
	public static IChannel officechannel;
	public static IChannel updatechannel;
	public static IGuild mainServer;
	public static IGuild devServer;

	private static volatile BukkitTask task;
	private static volatile boolean sent = false;

	@Override
	public void handle(ReadyEvent event) {
		try {
			task = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
				if (mainServer == null || devServer == null) {
					mainServer = event.getClient().getGuildByID("125813020357165056");
					devServer = event.getClient().getGuildByID("219529124321034241");
				}
				if (mainServer == null || devServer == null)
					return; // Retry
				if (!TBMCCoreAPI.IsTestServer()) {
					botchannel = mainServer.getChannelByID("209720707188260864"); // bot
					annchannel = mainServer.getChannelByID("126795071927353344"); // announcements
					genchannel = mainServer.getChannelByID("125813020357165056"); // general
					chatchannel = mainServer.getChannelByID("249663564057411596"); // minecraft_chat
					botroomchannel = devServer.getChannelByID("239519012529111040"); // bot-room
					officechannel = devServer.getChannelByID("219626707458457603"); // developers-office
					updatechannel = devServer.getChannelByID("233724163519414272"); // server-updates
					dc.changeStatus(Status.game("on TBMC"));
				} else {
					botchannel = devServer.getChannelByID("239519012529111040"); // bot-room
					annchannel = botchannel; // bot-room
					genchannel = botchannel; // bot-room
					botroomchannel = botchannel;// bot-room
					chatchannel = botchannel;// bot-room
					officechannel = devServer.getChannelByID("219626707458457603"); // developers-office
					updatechannel = botchannel;
					dc.changeStatus(Status.game("testing"));
				}
				if (botchannel == null || annchannel == null || genchannel == null || botroomchannel == null
						|| chatchannel == null || officechannel == null || updatechannel == null)
					return; // Retry
				SafeMode = false;
				if (task != null)
					task.cancel();
				if (!sent) {
					sendMessageToChannel(chatchannel, "", new EmbedBuilder().withColor(Color.GREEN)
							.withTitle("Server started - chat connected.").build());
					try {
						List<IMessage> msgs = genchannel.getPinnedMessages();
						for (int i = msgs.size() - 1; i >= 10; i--) { // Unpin all pinned messages except the newest 10
							genchannel.unpin(msgs.get(i));
							Thread.sleep(10);
						}
					} catch (Exception e) {
						TBMCCoreAPI.SendException("Error occured while unpinning messages!", e);
					}
					sent = true;
				}
			}, 0, 10);
			for (IListener<?> listener : CommandListener.getListeners())
				dc.getDispatcher().registerListener(listener);
			MCChatListener mcchat = new MCChatListener();
			dc.getDispatcher().registerListener(mcchat);
			TBMCCoreAPI.RegisterEventsForExceptions(mcchat, this);
			dc.getDispatcher().registerListener(new AutoUpdaterListener());
			Bukkit.getPluginManager().registerEvents(new ExceptionListener(), this);
			TBMCCoreAPI.RegisterEventsForExceptions(new MCListener(), this);
			TBMCChatAPI.AddCommands(this, DiscordMCCommandBase.class);
			new Thread(() -> AnnouncementGetterThreadMethod()).start();
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
		getConfig().set("gameroles", GameRoles);
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
				if (SafeMode) {
					Thread.sleep(10000);
					continue;
				}
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
		try {
			if (channel == chatchannel)
				MCChatListener.resetLastMessage(); // If this is a chat message, it'll be set again
			final String content = TBMCCoreAPI.IsTestServer() && channel != chatchannel
					? "*The following message is from a test server*\n" + message : message;
			return perform(
					() -> embed == null ? channel.sendMessage(content) : channel.sendMessage(content, embed, false));
		} catch (Exception e) {
			Bukkit.getLogger().warning(
					"Failed to deliver message to Discord! Channel: " + channel.getName() + " Message: " + message);
			throw new RuntimeException(e);
		}
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
		boolean random = false;
		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) == '§') {
				i++;// Skips the data value, the 4 in "§4Alisolarflare"
				if (string.charAt(i) == 'k')
					random = true;
				else
					random = false;
			} else {
				if (!random) // Skip random/obfuscated characters
					sanitizedString += string.charAt(i);
			}
		}
		return sanitizedString;
	}

	/**
	 * Performs Discord actions, retrying when ratelimited. May return null if action fails too many times or in safe mode.
	 */
	public static <T extends IDiscordObject<T>> T perform(DiscordSupplier<T> action)
			throws DiscordException, MissingPermissionsException {
		for (int i = 0; i < 20; i++)
			try {
				if (SafeMode)
					return null;
				return action.get();
			} catch (RateLimitException e) {
				try {
					Thread.sleep(e.getRetryDelay() > 0 ? e.getRetryDelay() : 10);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		return null;
	}

	/**
	 * Performs Discord actions, retrying when ratelimited.
	 */
	public static void perform(DiscordRunnable action) throws DiscordException, MissingPermissionsException {
		for (int i = 0; i < 20; i++)
			try {
				if (SafeMode)
					return;
				action.run();
				return; // Gotta escape that loop
			} catch (RateLimitException e) {
				try {
					Thread.sleep(e.getRetryDelay() > 0 ? e.getRetryDelay() : 10);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
	}
}
