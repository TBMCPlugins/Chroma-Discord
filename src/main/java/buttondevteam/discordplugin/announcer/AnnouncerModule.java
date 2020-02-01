package buttondevteam.discordplugin.announcer;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ComponentMetadata;
import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.architecture.ReadOnlyConfigData;
import buttondevteam.lib.player.ChromaGamerBase;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import lombok.val;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Posts new posts from Reddit to the specified channel(s). It will pin the regular posts (not the mod posts).
 */
@ComponentMetadata(enabledByDefault = false)
public class AnnouncerModule extends Component<DiscordPlugin> {
	/**
	 * Channel to post new posts.
	 */
	public ReadOnlyConfigData<Mono<MessageChannel>> channel() {
		return DPUtils.channelData(getConfig(), "channel");
	}

	/**
	 * Channel where distinguished (moderator) posts go.
	 */
	public ReadOnlyConfigData<Mono<MessageChannel>> modChannel() {
		return DPUtils.channelData(getConfig(), "modChannel");
	}

	/**
	 * Automatically unpins all messages except the last few. Set to 0 or >50 to disable
	 */
	public ConfigData<Short> keepPinned() {
		return getConfig().getData("keepPinned", (short) 40);
	}

	private ConfigData<Long> lastAnnouncementTime() {
		return getConfig().getData("lastAnnouncementTime", 0L);
	}

	private ConfigData<Long> lastSeenTime() {
		return getConfig().getData("lastSeenTime", 0L);
	}

	/**
	 * The subreddit to pull the posts from
	 */
	private ConfigData<String> subredditURL() {
		return getConfig().getData("subredditURL", "https://www.reddit.com/r/ChromaGamers");
	}

	private static boolean stop = false;

	@Override
	protected void enable() {
		if (DPUtils.disableIfConfigError(this, channel(), modChannel())) return;
		stop = false; //If not the first time
		val keepPinned = keepPinned().get();
		if (keepPinned == 0) return;
		Flux<Message> msgs = channel().get().flatMapMany(MessageChannel::getPinnedMessages);
		msgs.subscribe(Message::unpin);
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
				String body = TBMCCoreAPI.DownloadString(subredditURL().get() + "/new/.json?limit=10");
				JsonArray json = new JsonParser().parse(body).getAsJsonObject().get("data").getAsJsonObject()
					.get("children").getAsJsonArray();
				StringBuilder msgsb = new StringBuilder();
				StringBuilder modmsgsb = new StringBuilder();
				long lastanntime = lastAnnouncementTime().get();
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
					if (date > lastSeenTime().get())
						lastSeenTime().set(date);
					else if (date > lastAnnouncementTime().get()) {
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
					channel().get().flatMap(ch -> ch.createMessage(msgsb.toString()))
						.flatMap(Message::pin).subscribe();
				if (modmsgsb.length() > 0)
					modChannel().get().flatMap(ch -> ch.createMessage(modmsgsb.toString()))
						.flatMap(Message::pin).subscribe();
				if (lastAnnouncementTime().get() != lastanntime)
					lastAnnouncementTime().set(lastanntime); // If sending succeeded
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
