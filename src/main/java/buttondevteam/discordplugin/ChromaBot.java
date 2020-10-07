package buttondevteam.discordplugin;

import buttondevteam.discordplugin.mcchat.MCChatUtils;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import lombok.Getter;
import org.bukkit.scheduler.BukkitScheduler;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.function.Function;

public class ChromaBot {
	/**
	 * May be null if it's not initialized. Initialization happens after the server is done loading (using {@link BukkitScheduler#runTaskAsynchronously(org.bukkit.plugin.Plugin, Runnable)})
	 */
	private static @Getter ChromaBot instance;
	private DiscordPlugin dp;

	/**
	 * This will set the instance field.
	 *
	 * @param dp The Discord plugin
	 */
	ChromaBot(DiscordPlugin dp) {
		instance = this;
		this.dp = dp;
	}

	static void delete() {
		instance = null;
	}

	/**
	 * Send a message to the chat channels and private chats.
	 *
	 * @param message The message to send, duh (use {@link MessageChannel#createMessage(String)})
	 */
	public void sendMessage(Function<Mono<MessageChannel>, Mono<Message>> message) {
		MCChatUtils.forAllMCChat(ch -> message.apply(ch).subscribe());
	}

	/**
	 * Send a message to the chat channels, private chats and custom chats.
	 *
	 * @param message The message to send, duh
	 * @param toggle  The toggle type for channelcon
	 */
	public void sendMessageCustomAsWell(Function<Mono<MessageChannel>, Mono<Message>> message, @Nullable ChannelconBroadcast toggle) {
		MCChatUtils.forCustomAndAllMCChat(ch -> message.apply(ch).subscribe(), toggle, false);
	}

	public void updatePlayerList() {
		MCChatUtils.updatePlayerList();
	}
}
