package buttondevteam.discordplugin;

import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.player.ChromaGamerBase;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.val;
import org.bukkit.configuration.file.YamlConfiguration;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;

import java.io.File;
import java.util.List;

public class AnnouncerModule extends Component<DiscordPlugin> {
	public ConfigData<IChannel> channel() {
		return DPUtils.channelData(getConfig(), "channel", 239519012529111040L);
	}

	public ConfigData<IChannel> modChannel() {
		return DPUtils.channelData(getConfig(), "modChannel", 239519012529111040L);
	}

	/**
	 * Set to 0 or >50 to disable
	 */
	public ConfigData<Short> keepPinned() {
		return getConfig().getData("keepPinned", (short) 40);
	}

	private ConfigData<Long> lastannouncementtime() {
		return getConfig().getData("lastAnnouncementTime", 0L);
	}

	private ConfigData<Long> lastseentime() {
		return getConfig().getData("lastSeenTime", 0L);
	}

	private static final String SubredditURL = "https://www.reddit.com/r/ChromaGamers";
	private static boolean stop = false;

	@Override
	protected void enable() {
		if (DPUtils.disableIfConfigError(this, channel(), modChannel())) return;
		stop = false; //If not the first time
		DPUtils.performNoWait(() -> {
			try {
				val keepPinned = keepPinned().get();
				if (keepPinned == 0) return;
				val channel = channel().get();
				List<IMessage> msgs = channel.getPinnedMessages();
				for (int i = msgs.size() - 1; i >= keepPinned; i--) { // Unpin all pinned messages except the newest 10
					channel.unpin(msgs.get(i));
					Thread.sleep(10);
				}
			} catch (InterruptedException ignore) {
			}
		});
		val yc = YamlConfiguration.loadConfiguration(new File("plugins/DiscordPlugin", "config.yml")); //Name change
		if (lastannouncementtime().get() == 0) //Load old data
			lastannouncementtime().set(yc.getLong("lastannouncementtime"));
		if (lastseentime().get() == 0)
			lastseentime().set(yc.getLong("lastseentime"));
		new Thread(this::AnnouncementGetterThreadMethod).start();
	}

	@Override
	protected void disable() {
		stop = true;
	}

	private void AnnouncementGetterThreadMethod() {
		while (!stop) {
			try {
				if (!isEnabled()) {
					Thread.sleep(10000);
					continue;
				}
				String body = TBMCCoreAPI.DownloadString(SubredditURL + "/new/.json?limit=10");
				JsonArray json = new JsonParser().parse(body).getAsJsonObject().get("data").getAsJsonObject()
					.get("children").getAsJsonArray();
				StringBuilder msgsb = new StringBuilder();
				StringBuilder modmsgsb = new StringBuilder();
				long lastanntime = lastannouncementtime().get();
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
					if (date > lastseentime().get())
						lastseentime().set(date);
					else if (date > lastannouncementtime().get()) {
						do {
							val reddituserclass = ChromaGamerBase.getTypeForFolder("reddit");
							if (reddituserclass == null)
								break;
							val user = ChromaGamerBase.getUser(author, reddituserclass);
							String id = user.getConnectedID(DiscordPlayer.class);
							if (id != null)
								author = "<@" + id + ">";
						} while (false);
						if (!author.startsWith("<"))
							author = "/u/" + author;
						(distinguished != null && distinguished.equals("moderator") ? modmsgsb : msgsb)
							.append("A new post was submitted to the subreddit by ").append(author).append("\n")
							.append(permalink).append("\n");
						lastanntime = date;
					}
				}
				if (msgsb.length() > 0)
					channel().get().pin(DiscordPlugin.sendMessageToChannelWait(channel().get(), msgsb.toString()));
				if (modmsgsb.length() > 0)
					DiscordPlugin.sendMessageToChannel(modChannel().get(), modmsgsb.toString());
				if (lastannouncementtime().get() != lastanntime) {
					lastannouncementtime().set(lastanntime); // If sending succeeded
					getPlugin().saveConfig(); //TODO: Won't be needed if I implement auto-saving
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
}
