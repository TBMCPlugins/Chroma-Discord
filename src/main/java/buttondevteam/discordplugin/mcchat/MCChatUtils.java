package buttondevteam.discordplugin.mcchat;

import buttondevteam.core.ComponentManager;
import buttondevteam.core.component.channel.Channel;
import buttondevteam.discordplugin.*;
import buttondevteam.discordplugin.broadcaster.GeneralEventBroadcasterModule;
import buttondevteam.lib.TBMCSystemChatEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import io.netty.util.collection.LongObjectHashMap;
import lombok.RequiredArgsConstructor;
import lombok.experimental.var;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.AuthorNagException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MCChatUtils {
	/**
	 * May contain P&lt;DiscordID&gt; as key for public chat
	 */
	public static final HashMap<String, HashMap<Channel, DiscordSender>> UnconnectedSenders = new HashMap<>();
	public static final HashMap<String, HashMap<Channel, DiscordConnectedPlayer>> ConnectedSenders = new HashMap<>();
	/**
	 * May contain P&lt;DiscordID&gt; as key for public chat
	 */
	public static final HashMap<String, HashMap<Channel, DiscordPlayerSender>> OnlineSenders = new HashMap<>();
	static @Nullable LastMsgData lastmsgdata;
	static LongObjectHashMap<Message> lastmsgfromd = new LongObjectHashMap<>(); // Last message sent by a Discord user, used for clearing checkmarks
	private static MinecraftChatModule module;

	public static void updatePlayerList() {
		if (notEnabled()) return;
		if (lastmsgdata != null)
			updatePL(lastmsgdata);
		MCChatCustom.lastmsgCustom.forEach(MCChatUtils::updatePL);
	}

	private static boolean notEnabled() {
		return getModule() == null;
	}

	private static MinecraftChatModule getModule() {
		if (module == null) module = ComponentManager.getIfEnabled(MinecraftChatModule.class);
		else if (!module.isEnabled()) module = null; //Reset if disabled
		return module;
	}

	private static void updatePL(LastMsgData lmd) {
		String topic = lmd.channel.getTopic().orElse("");
		if (topic.length() == 0)
			topic = ".\n----\nMinecraft chat\n----\n.";
		String[] s = topic.split("\\n----\\n");
		if (s.length < 3)
			return;
		s[0] = Bukkit.getOnlinePlayers().size() + " player" + (Bukkit.getOnlinePlayers().size() != 1 ? "s" : "")
				+ " online";
		s[s.length - 1] = "Players: " + Bukkit.getOnlinePlayers().stream()
				.map(p -> DPUtils.sanitizeString(p.getDisplayName())).collect(Collectors.joining(", "));
		lmd.channel.edit(tce -> tce.setTopic(String.join("\n----\n", s)).setReason("Player list update")).subscribe(); //Don't wait
	}

	public static <T extends DiscordSenderBase> T addSender(HashMap<String, HashMap<Channel, T>> senders,
															User user, T sender) {
		return addSender(senders, user.getId().asString(), sender);
	}

	public static <T extends DiscordSenderBase> T addSender(HashMap<String, HashMap<MessageChannel, T>> senders,
															String did, T sender) {
		var map = senders.get(did);
		if (map == null)
			map = new HashMap<>();
		map.put(sender.getChannel(), sender);
		senders.put(did, map);
		return sender;
	}

	public static <T extends DiscordSenderBase> T getSender(HashMap<String, HashMap<MessageChannel, T>> senders,
															MessageChannel channel, User user) {
		var map = senders.get(user.getId().asString());
		if (map != null)
			return map.get(channel);
		return null;
	}

	public static <T extends DiscordSenderBase> T removeSender(HashMap<String, HashMap<Channel, T>> senders,
															   Channel channel, User user) {
		var map = senders.get(user.getId().asString());
		if (map != null)
			return map.remove(channel);
		return null;
	}

	public static void forAllMCChat(Consumer<MessageChannel> action) {
		if (notEnabled()) return;
		action.accept(module.chatChannel().get());
		for (LastMsgData data : MCChatPrivate.lastmsgPerUser)
			action.accept(data.channel);
		// lastmsgCustom.forEach(cc -> action.accept(cc.channel)); - Only send relevant messages to custom chat
	}

	/**
	 * For custom and all MC chat
	 *
	 * @param action  The action to act
	 * @param toggle  The toggle to check
	 * @param hookmsg Whether the message is also sent from the hook
	 */
	public static void forCustomAndAllMCChat(Consumer<MessageChannel> action, @Nullable ChannelconBroadcast toggle, boolean hookmsg) {
		if (notEnabled()) return;
		if (!GeneralEventBroadcasterModule.isHooked() || !hookmsg)
			forAllMCChat(action);
		final Consumer<MCChatCustom.CustomLMD> customLMDConsumer = cc -> action.accept(cc.channel);
		if (toggle == null)
			MCChatCustom.lastmsgCustom.forEach(customLMDConsumer);
		else
			MCChatCustom.lastmsgCustom.stream().filter(cc -> (cc.toggles & toggle.flag) != 0).forEach(customLMDConsumer);
	}

	/**
	 * Do the {@code action} for each custom chat the {@code sender} have access to and has that broadcast type enabled.
	 *
	 * @param action The action to do
	 * @param sender The sender to check perms of or null to send to all that has it toggled
	 * @param toggle The toggle to check or null to send to all allowed
	 */
	public static void forAllowedCustomMCChat(Consumer<MessageChannel> action, @Nullable CommandSender sender, @Nullable ChannelconBroadcast toggle) {
		if (notEnabled()) return;
		MCChatCustom.lastmsgCustom.stream().filter(clmd -> {
			//new TBMCChannelConnectFakeEvent(sender, clmd.mcchannel).shouldSendTo(clmd.dcp) - Thought it was this simple hehe - Wait, it *should* be this simple
			if (toggle != null && (clmd.toggles & toggle.flag) == 0)
				return false; //If null then allow
			if (sender == null)
				return true;
			return clmd.groupID.equals(clmd.mcchannel.getGroupID(sender));
		}).forEach(cc -> action.accept(cc.channel)); //TODO: Send error messages on channel connect
	}

	/**
	 * Do the {@code action} for each custom chat the {@code sender} have access to and has that broadcast type enabled.
	 *
	 * @param action  The action to do
	 * @param sender  The sender to check perms of or null to send to all that has it toggled
	 * @param toggle  The toggle to check or null to send to all allowed
	 * @param hookmsg Whether the message is also sent from the hook
	 */
	public static void forAllowedCustomAndAllMCChat(Consumer<MessageChannel> action, @Nullable CommandSender sender, @Nullable ChannelconBroadcast toggle, boolean hookmsg) {
		if (notEnabled()) return;
		if (!GeneralEventBroadcasterModule.isHooked() || !hookmsg)
			forAllMCChat(action);
		forAllowedCustomMCChat(action, sender, toggle);
	}

	public static Consumer<MessageChannel> send(String message) {
		return ch -> ch.createMessage(DPUtils.sanitizeString(message)).subscribe();
	}

	public static void forAllowedMCChat(Consumer<MessageChannel> action, TBMCSystemChatEvent event) {
		if (notEnabled()) return;
		if (event.getChannel().isGlobal())
			action.accept(module.chatChannel().get());
		for (LastMsgData data : MCChatPrivate.lastmsgPerUser)
			if (event.shouldSendTo(getSender(data.channel, data.user)))
				action.accept(data.channel);
		MCChatCustom.lastmsgCustom.stream().filter(clmd -> {
			if (!clmd.brtoggles.contains(event.getTarget()))
				return false;
			return event.shouldSendTo(clmd.dcp);
		}).map(clmd -> clmd.channel).forEach(action);
	}

	/**
	 * This method will find the best sender to use: if the player is online, use that, if not but connected then use that etc.
	 */
	static DiscordSenderBase getSender(MessageChannel channel, final User author) {
		//noinspection OptionalGetWithoutIsPresent
		return Stream.<Supplier<Optional<DiscordSenderBase>>>of( // https://stackoverflow.com/a/28833677/2703239
				() -> Optional.ofNullable(getSender(OnlineSenders, channel, author)), // Find first non-null
				() -> Optional.ofNullable(getSender(ConnectedSenders, channel, author)), // This doesn't support the public chat, but it'll always return null for it
				() -> Optional.ofNullable(getSender(UnconnectedSenders, channel, author)), //
				() -> Optional.of(addSender(UnconnectedSenders, author,
						new DiscordSender(author, channel)))).map(Supplier::get).filter(Optional::isPresent).map(Optional::get).findFirst().get();
	}

	/**
	 * Resets the last message, so it will start a new one instead of appending to it.
	 * This is used when someone (even the bot) sends a message to the channel.
	 *
	 * @param channel The channel to reset in - the process is slightly different for the public, private and custom chats
	 */
	public static void resetLastMessage(Channel channel) {
		if (notEnabled()) return;
		if (channel.getId().asLong() == module.chatChannel().get().getId().asLong()) {
			(lastmsgdata == null ? lastmsgdata = new LastMsgData(module.chatChannel().get(), null)
					: lastmsgdata).message = null;
			return;
		} // Don't set the whole object to null, the player and channel information should be preserved
		for (LastMsgData data : channel.isPrivate() ? MCChatPrivate.lastmsgPerUser : MCChatCustom.lastmsgCustom) {
			if (data.channel.getId().asLong() == channel.getId().asLong()) {
				data.message = null;
				return;
			}
		}
		//If it gets here, it's sending a message to a non-chat channel
	}

	public static void callEventExcludingSome(Event event) {
		if (notEnabled()) return;
		callEventExcluding(event, false, module.excludedPlugins().get());
	}

	/**
	 * Calls an event with the given details.
	 * <p>
	 * This method only synchronizes when the event is not asynchronous.
	 *
	 * @param event   Event details
	 * @param only    Flips the operation and <b>includes</b> the listed plugins
	 * @param plugins The plugins to exclude. Not case sensitive.
	 */
	@SuppressWarnings("WeakerAccess")
	public static void callEventExcluding(Event event, boolean only, String... plugins) { // Copied from Spigot-API and modified a bit
		if (event.isAsynchronous()) {
			if (Thread.holdsLock(Bukkit.getPluginManager())) {
				throw new IllegalStateException(
					event.getEventName() + " cannot be triggered asynchronously from inside synchronized code.");
			}
			if (Bukkit.getServer().isPrimaryThread()) {
				throw new IllegalStateException(
					event.getEventName() + " cannot be triggered asynchronously from primary server thread.");
			}
			fireEventExcluding(event, only, plugins);
		} else {
			synchronized (Bukkit.getPluginManager()) {
				fireEventExcluding(event, only, plugins);
			}
		}
	}

	private static void fireEventExcluding(Event event, boolean only, String... plugins) {
		HandlerList handlers = event.getHandlers(); // Code taken from SimplePluginManager in Spigot-API
		RegisteredListener[] listeners = handlers.getRegisteredListeners();
		val server = Bukkit.getServer();

		for (RegisteredListener registration : listeners) {
			if (!registration.getPlugin().isEnabled()
				|| Arrays.stream(plugins).anyMatch(p -> only ^ p.equalsIgnoreCase(registration.getPlugin().getName())))
				continue; // Modified to exclude plugins

			try {
				registration.callEvent(event);
			} catch (AuthorNagException ex) {
				Plugin plugin = registration.getPlugin();

				if (plugin.isNaggable()) {
					plugin.setNaggable(false);

					server.getLogger().log(Level.SEVERE,
						String.format("Nag author(s): '%s' of '%s' about the following: %s",
							plugin.getDescription().getAuthors(), plugin.getDescription().getFullName(),
							ex.getMessage()));
				}
			} catch (Throwable ex) {
				server.getLogger().log(Level.SEVERE, "Could not pass event " + event.getEventName() + " to "
					+ registration.getPlugin().getDescription().getFullName(), ex);
			}
		}
	}

	@RequiredArgsConstructor
	public static class LastMsgData {
		public Message message;
		public long time;
		public String content;
		public final TextChannel channel;
		public Channel mcchannel;
		public final User user;
	}
}
