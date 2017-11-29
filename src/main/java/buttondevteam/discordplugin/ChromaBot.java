package buttondevteam.discordplugin;

import java.awt.Color;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;

import buttondevteam.discordplugin.listeners.MCChatListener;
import lombok.Getter;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

public class ChromaBot {
	/**
	 * May be null if it's not initialized. Initialization happens after the server is done loading (using {@link BukkitScheduler#runTaskAsynchronously(org.bukkit.plugin.Plugin, Runnable)})
	 */
	private static @Getter ChromaBot instance;
	private DiscordPlugin dp;

	/**
	 * This will set the instance field.
	 * 
	 * @param dp
	 *            The Discord plugin
	 */
	ChromaBot(DiscordPlugin dp) {
		instance = this;
		this.dp = dp;
	}

	static void delete() {
		instance = null;
	}

	/**
	 * Send a message to the chat channel and private chats.
	 * 
	 * @param message
	 *            The message to send, duh
	 */
	public void sendMessage(String message) {
		MCChatListener.forAllMCChat(ch -> DiscordPlugin.sendMessageToChannel(ch, message));
	}

	/**
	 * Send a message to the chat channel and private chats.
	 * 
	 * @param message
	 *            The message to send, duh
	 * @param embed
	 *            Custom fancy stuff, use {@link EmbedBuilder} to create one
	 */
	public void sendMessage(String message, EmbedObject embed) {
		MCChatListener.forAllMCChat(ch -> DiscordPlugin.sendMessageToChannel(ch, message, embed));
	}

	/**
	 * Send a message to an arbitrary channel. This will not send it to the private chats.
	 * 
	 * @param channel
	 *            The channel to send to, use the channel variables in {@link DiscordPlugin}
	 * @param message
	 *            The message to send, duh
	 * @param embed
	 *            Custom fancy stuff, use {@link EmbedBuilder} to create one
	 */
	public void sendMessage(IChannel channel, String message, EmbedObject embed) {
		DiscordPlugin.sendMessageToChannel(channel, message, embed);
	}

	/**
	 * Send a fancy message to the chat channel. This will show a bold text with a colored line.
	 * 
	 * @param message
	 *            The message to send, duh
	 * @param color
	 *            The color of the line before the text
	 */
	public void sendFancyMessage(String message, Color color) {
		MCChatListener.forAllMCChat(ch -> DiscordPlugin.sendMessageToChannel(ch, message,
				new EmbedBuilder().withTitle(message).withColor(color).build()));
	}

	/**
	 * Send a fancy message to the chat channel. This will show a bold text with a colored line.
	 * 
	 * @param message
	 *            The message to send, duh
	 * @param color
	 *            The color of the line before the text
	 * @param mcauthor
	 *            The name of the Minecraft player who is the author of this message
	 */
	public void sendFancyMessage(String message, Color color, String mcauthor) {
		MCChatListener.forAllMCChat(ch -> DiscordPlugin.sendMessageToChannel(ch, message,
				DPUtils.embedWithHead(new EmbedBuilder().withTitle(message).withColor(color), mcauthor).build()));
	}

	/**
	 * Send a fancy message to the chat channel. This will show a bold text with a colored line.
	 * 
	 * @param message
	 *            The message to send, duh
	 * @param color
	 *            The color of the line before the text
	 * @param authorname
	 *            The name of the author of this message
	 * @param authorimg
	 *            The URL of the avatar image for this message's author
	 */
	public void sendFancyMessage(String message, Color color, String authorname, String authorimg) {
		MCChatListener.forAllMCChat(ch -> DiscordPlugin.sendMessageToChannel(ch, message, new EmbedBuilder()
				.withTitle(message).withColor(color).withAuthorName(authorname).withAuthorIcon(authorimg).build()));
	}

	public void updatePlayerList() {
		DPUtils.performNoWait(() -> {
			String[] s = DiscordPlugin.chatchannel.getTopic().split("\\n----\\n");
			if (s.length < 3)
				return;
			s[0] = Bukkit.getOnlinePlayers().size() + " player" + (Bukkit.getOnlinePlayers().size() != 1 ? "s" : "")
					+ " online";
			s[s.length - 1] = "Players: " + Bukkit.getOnlinePlayers().stream()
					.map(p -> DPUtils.sanitizeString(p.getDisplayName())).collect(Collectors.joining(", "));
			DiscordPlugin.chatchannel.changeTopic(Arrays.stream(s).collect(Collectors.joining("\n----\n")));
		});
	}
}
