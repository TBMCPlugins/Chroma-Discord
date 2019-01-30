package buttondevteam.discordplugin.mcchat;

import buttondevteam.core.ComponentManager;
import buttondevteam.core.component.channel.Channel;
import buttondevteam.discordplugin.*;
import buttondevteam.discordplugin.broadcaster.GeneralEventBroadcasterModule;
import buttondevteam.lib.TBMCSystemChatEvent;
import io.netty.util.collection.LongObjectHashMap;
import lombok.RequiredArgsConstructor;
import lombok.experimental.var;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MCChatUtils {
	/**
	 * May contain P&lt;DiscordID&gt; as key for public chat
	 */
	public static final HashMap<String, HashMap<IChannel, DiscordSender>> UnconnectedSenders = new HashMap<>();
	public static final HashMap<String, HashMap<IChannel, DiscordConnectedPlayer>> ConnectedSenders = new HashMap<>();
	/**
	 * May contain P&lt;DiscordID&gt; as key for public chat
	 */
	public static final HashMap<String, HashMap<IChannel, DiscordPlayerSender>> OnlineSenders = new HashMap<>();
	static @Nullable LastMsgData lastmsgdata;
	static LongObjectHashMap<IMessage> lastmsgfromd = new LongObjectHashMap<>(); // Last message sent by a Discord user, used for clearing checkmarks
	private static MinecraftChatModule module;

	public static void updatePlayerList() {
		if (notEnabled()) return;
		DPUtils.performNoWait(() -> {
			if (lastmsgdata != null)
				updatePL(lastmsgdata);
			MCChatCustom.lastmsgCustom.forEach(MCChatUtils::updatePL);
		});
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
		String topic = lmd.channel.getTopic();
		if (topic == null || topic.length() == 0)
			topic = ".\n----\nMinecraft chat\n----\n.";
		String[] s = topic.split("\\n----\\n");
		if (s.length < 3)
			return;
		s[0] = Bukkit.getOnlinePlayers().size() + " player" + (Bukkit.getOnlinePlayers().size() != 1 ? "s" : "")
				+ " online";
		s[s.length - 1] = "Players: " + Bukkit.getOnlinePlayers().stream()
				.map(p -> DPUtils.sanitizeString(p.getDisplayName())).collect(Collectors.joining(", "));
		lmd.channel.changeTopic(String.join("\n----\n", s));
	}

	public static <T extends DiscordSenderBase> T addSender(HashMap<String, HashMap<IChannel, T>> senders,
	                                                        IUser user, T sender) {
		return addSender(senders, user.getStringID(), sender);
	}

	public static <T extends DiscordSenderBase> T addSender(HashMap<String, HashMap<IChannel, T>> senders,
	                                                        String did, T sender) {
		var map = senders.get(did);
		if (map == null)
			map = new HashMap<>();
		map.put(sender.getChannel(), sender);
		senders.put(did, map);
		return sender;
	}

	public static <T extends DiscordSenderBase> T getSender(HashMap<String, HashMap<IChannel, T>> senders,
	                                                        IChannel channel, IUser user) {
		var map = senders.get(user.getStringID());
		if (map != null)
			return map.get(channel);
		return null;
	}

	public static <T extends DiscordSenderBase> T removeSender(HashMap<String, HashMap<IChannel, T>> senders,
	                                                           IChannel channel, IUser user) {
		var map = senders.get(user.getStringID());
		if (map != null)
			return map.remove(channel);
		return null;
	}

	public static void forAllMCChat(Consumer<IChannel> action) {
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
	public static void forCustomAndAllMCChat(Consumer<IChannel> action, @Nullable ChannelconBroadcast toggle, boolean hookmsg) {
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
	public static void forAllowedCustomMCChat(Consumer<IChannel> action, @Nullable CommandSender sender, @Nullable ChannelconBroadcast toggle) {
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
	public static void forAllowedCustomAndAllMCChat(Consumer<IChannel> action, @Nullable CommandSender sender, @Nullable ChannelconBroadcast toggle, boolean hookmsg) {
		if (notEnabled()) return;
		if (!GeneralEventBroadcasterModule.isHooked() || !hookmsg)
			forAllMCChat(action);
		forAllowedCustomMCChat(action, sender, toggle);
	}

	public static Consumer<IChannel> send(String message) {
		return ch -> DiscordPlugin.sendMessageToChannel(ch, DPUtils.sanitizeString(message));
	}

	public static void forAllowedMCChat(Consumer<IChannel> action, TBMCSystemChatEvent event) {
		if (notEnabled()) return;
		if (event.getChannel().isGlobal())
			action.accept(module.chatChannel().get());
		for (LastMsgData data : MCChatPrivate.lastmsgPerUser)
			if (event.shouldSendTo(getSender(data.channel, data.user)))
				action.accept(data.channel);
		MCChatCustom.lastmsgCustom.stream().filter(clmd -> {
			if ((clmd.toggles & ChannelconBroadcast.BROADCAST.flag) == 0)
				return false;
			return event.shouldSendTo(clmd.dcp);
		}).map(clmd -> clmd.channel).forEach(action);
	}

	/**
	 * This method will find the best sender to use: if the player is online, use that, if not but connected then use that etc.
	 */
	static DiscordSenderBase getSender(IChannel channel, final IUser author) {
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
	public static void resetLastMessage(IChannel channel) {
		if (notEnabled()) return;
		if (channel.getLongID() == module.chatChannel().get().getLongID()) {
			(lastmsgdata == null ? lastmsgdata = new LastMsgData(module.chatChannel().get(), null)
					: lastmsgdata).message = null;
			return;
		} // Don't set the whole object to null, the player and channel information should be preserved
		for (LastMsgData data : channel.isPrivate() ? MCChatPrivate.lastmsgPerUser : MCChatCustom.lastmsgCustom) {
			if (data.channel.getLongID() == channel.getLongID()) {
				data.message = null;
				return;
			}
		}
		//If it gets here, it's sending a message to a non-chat channel
	}

	@RequiredArgsConstructor
	public static class LastMsgData {
		public IMessage message;
		public long time;
		public String content;
		public final IChannel channel;
		public Channel mcchannel;
		public final IUser user;
	}
}
