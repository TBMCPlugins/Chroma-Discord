package buttondevteam.discordplugin.listeners;

import java.awt.Color;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import buttondevteam.discordplugin.*;
import buttondevteam.discordplugin.playerfaker.VanillaCommandListener;
import buttondevteam.lib.*;
import buttondevteam.lib.chat.Channel;
import buttondevteam.lib.chat.ChatRoom;
import buttondevteam.lib.chat.TBMCChatAPI;
import buttondevteam.lib.player.TBMCPlayer;
import lombok.val;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

public class MCChatListener implements Listener, IListener<MessageReceivedEvent> {
	@EventHandler // Minecraft
	public void onMCChat(TBMCChatEvent e) {
		if (e.isCancelled())
			return;
		if (e.getSender() instanceof DiscordSender || e.getSender() instanceof DiscordPlayerSender)
			return;
		Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin, () -> {
			synchronized (this) {
				final String authorPlayer = DiscordPlugin.sanitizeString(e.getSender() instanceof Player //
						? ((Player) e.getSender()).getDisplayName() //
						: e.getSender().getName());
				final EmbedBuilder embed = new EmbedBuilder().withAuthorName(authorPlayer)
						.withDescription(e.getMessage()).withColor(new Color(e.getChannel().color.getRed(),
								e.getChannel().color.getGreen(), e.getChannel().color.getBlue()));
				if (e.getSender() instanceof Player)
					embed.withAuthorIcon("https://minotar.net/avatar/" + ((Player) e.getSender()).getName() + "/32.png")
							.withAuthorUrl("https://tbmcplugins.github.io/profile.html?type=minecraft&id="
									+ ((Player) e.getSender()).getUniqueId()); // TODO: Constant/method to get URLs like this
				final long nanoTime = System.nanoTime();
				Consumer<LastMsgData> doit = lastmsgdata -> {
					final EmbedObject embedObject = embed.build();
					String msg = lastmsgdata.channel.isPrivate()
							? DiscordPlugin.sanitizeString(e.getChannel().DisplayName) : "";
					if (lastmsgdata.message == null || lastmsgdata.message.isDeleted()
							|| !authorPlayer.equals(lastmsgdata.message.getEmbeds().get(0).getAuthor().getName())
							|| lastmsgdata.time / 1000000000f < nanoTime / 1000000000f - 120
							|| !lastmsgdata.mcchannel.ID.equals(e.getChannel().ID)) {
						lastmsgdata.message = DiscordPlugin.sendMessageToChannel(lastmsgdata.channel, msg, embedObject);
						lastmsgdata.time = nanoTime;
						lastmsgdata.mcchannel = e.getChannel();
						lastmsgdata.content = embedObject.description;
					} else
						try {
							lastmsgdata.content = embedObject.description = lastmsgdata.content + "\n"
									+ embedObject.description;// The message object doesn't get updated
							final LastMsgData _lastmsgdata = lastmsgdata;
							DiscordPlugin.perform(() -> _lastmsgdata.message.edit(msg, embedObject));
						} catch (MissingPermissionsException | DiscordException e1) {
							TBMCCoreAPI.SendException("An error occured while editing chat message!", e1);
						}
				};
				if (e.getChannel().equals(Channel.GlobalChat))
					doit.accept(lastmsgdata == null
							? lastmsgdata = new LastMsgData(DiscordPlugin.chatchannel, null, null) : lastmsgdata);

				for (LastMsgData data : lastmsgPerUser) {
					if (data.dp.isMinecraftChatEnabled() && e.shouldSendTo(getSender(data.channel, data.user, data.dp)))
						doit.accept(data);
				}
			}
		});
	}

	private static class LastMsgData {
		public IMessage message;
		public long time;
		public String content;
		public IChannel channel;
		public Channel mcchannel;
		public IUser user;
		public DiscordPlayer dp;

		public LastMsgData(IChannel channel, IUser user, DiscordPlayer dp) {
			this.channel = channel;
		}
	}

	@EventHandler
	public void onChatPreprocess(TBMCChatPreprocessEvent event) {
		int start = -1;
		while ((start = event.getMessage().indexOf('@', start + 1)) != -1) {
			int mid = event.getMessage().indexOf('#', start + 1);
			if (mid == -1)
				return;
			int end_ = event.getMessage().indexOf(' ', mid + 1);
			if (end_ == -1)
				end_ = event.getMessage().length();
			final int end = end_;
			final int startF = start;
			DiscordPlugin.dc.getUsersByName(event.getMessage().substring(start + 1, mid)).stream()
					.filter(u -> u.getDiscriminator().equals(event.getMessage().substring(mid + 1, end))).findAny()
					.ifPresent(user -> event.setMessage(event.getMessage().substring(0, startF) + "@" + user.getName()
							+ (event.getMessage().length() > end ? event.getMessage().substring(end)
									: ""))); // TODO: Add formatting
			start = end; // Skip any @s inside the mention
		}
	}

	private static final String[] UnconnectedCmds = new String[] { "list", "u", "shrug", "tableflip", "unflip", "mwiki",
			"yeehaw" };

	private static LastMsgData lastmsgdata;
	private static short lastlist = 0;
	private static short lastlistp = 0;
	/**
	 * Used for messages in PMs (mcchat).
	 */
	private static ArrayList<LastMsgData> lastmsgPerUser = new ArrayList<LastMsgData>();

	public static boolean privateMCChat(IChannel channel, boolean start, IUser user, DiscordPlayer dp) {
		TBMCPlayer mcp = dp.getAs(TBMCPlayer.class);
		if (mcp != null) { // If the accounts aren't connected, can't make a connected sender
			val p = Bukkit.getPlayer(mcp.getUUID());
			if (start) {
				val sender = new DiscordConnectedPlayer(user, channel, mcp.getUUID());
				ConnectedSenders.put(user.getStringID(), sender);
				if (p == null)// Player is offline - If the player is online, that takes precedence
					MCListener.callEventExcluding(new PlayerJoinEvent(sender, ""), "ProtocolLib");
			} else {
				val sender = ConnectedSenders.remove(user.getStringID());
				if (p == null)// Player is offline - If the player is online, that takes precedence
					MCListener.callEventExcluding(new PlayerQuitEvent(sender, ""), "ProtocolLib");
			}
		}
		return start //
				? lastmsgPerUser.add(new LastMsgData(channel, user, dp)) // Doesn't support group DMs
				: lastmsgPerUser.removeIf(lmd -> lmd.channel.getLongID() == channel.getLongID());
	}

	// ......................DiscordSender....DiscordConnectedPlayer.DiscordPlayerSender
	// Offline public chat......x............................................
	// Online public chat.......x...........................................x
	// Offline private chat.....x.......................x....................
	// Online private chat......x.......................x...................x
	// If online and enabling private chat, don't login
	// If leaving the server and private chat is enabled (has ConnectedPlayer), call login in a task on lowest priority
	// If private chat is enabled and joining the server, logout the fake player on highest priority
	// If online and disabling private chat, don't logout
	// The maps may not contain the senders for UnconnectedSenders

	public static boolean isMinecraftChatEnabled(DiscordPlayer dp) {
		return lastmsgPerUser.stream().anyMatch(
				lmd -> ((IPrivateChannel) lmd.channel).getRecipient().getStringID().equals(dp.getDiscordID()));
	}

	/**
	 * May contain P&lt;DiscordID&gt; as key for public chat
	 */
	public static final HashMap<String, DiscordSender> UnconnectedSenders = new HashMap<>();
	public static final HashMap<String, DiscordConnectedPlayer> ConnectedSenders = new HashMap<>();
	/**
	 * May contain P&lt;DiscordID&gt; as key for public chat
	 */
	public static final HashMap<String, DiscordPlayerSender> OnlineSenders = new HashMap<>();
	public static short ListC = 0;

	public static void resetLastMessage() {
		(lastmsgdata == null ? lastmsgdata = new LastMsgData(DiscordPlugin.chatchannel, null, null)
				: lastmsgdata).message = null;
	} // Don't set the whole object to null, the player and channel information should be preserved

	/**
	 * This overload sends it to the global chat.
	 */
	public static void sendSystemMessageToChat(String msg) {
		DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel, msg);
		for (LastMsgData data : lastmsgPerUser)
			DiscordPlugin.sendMessageToChannel(data.channel, msg);
	}

	public static void sendSystemMessageToChat(TBMCSystemChatEvent event) {
		if (Channel.GlobalChat.ID.equals(event.getChannel().ID))
			DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel, event.getMessage());
		for (LastMsgData data : lastmsgPerUser)
			if (event.shouldSendTo(getSender(data.channel, data.user, data.dp)))
				DiscordPlugin.sendMessageToChannel(data.channel, event.getMessage());
	}

	@Override // Discord
	public void handle(MessageReceivedEvent event) {
		val author = event.getMessage().getAuthor();
		val user = DiscordPlayer.getUser(author.getStringID(), DiscordPlayer.class);
		if (!event.getMessage().getChannel().getStringID().equals(DiscordPlugin.chatchannel.getStringID())
				&& !(event.getMessage().getChannel().isPrivate() && user.isMinecraftChatEnabled()
						&& !DiscordPlugin.checkIfSomeoneIsTestingWhileWeArent()))
			return;
		resetLastMessage();
		lastlist++;
		if (author.isBot())
			return;
		if (CommandListener.runCommand(event.getMessage(), true))
			return;
		String dmessage = event.getMessage().getContent();
		synchronized (this) {
			try {
				final DiscordSenderBase dsender = getSender(event.getMessage().getChannel(), author, user);

				for (IUser u : event.getMessage().getMentions()) {
					dmessage = dmessage.replace(u.mention(false), "@" + u.getName()); // TODO: IG Formatting
					final String nick = u.getNicknameForGuild(DiscordPlugin.mainServer);
					dmessage = dmessage.replace(u.mention(true), "@" + (nick != null ? nick : u.getName()));
				}

				BiConsumer<Channel, String> sendChatMessage = (channel, msg) -> TBMCChatAPI.SendChatMessage(channel,
						dsender,
						msg + (event.getMessage().getAttachments().size() > 0 ? "\n" + event.getMessage()
								.getAttachments().stream().map(a -> a.getUrl()).collect(Collectors.joining("\n"))
								: ""));

				if (dmessage.startsWith("/")) { // Ingame command
					DiscordPlugin.perform(() -> {
						if (!event.getMessage().isDeleted() && !event.getChannel().isPrivate())
							event.getMessage().delete();
					});
					final String cmd = dmessage.substring(1).toLowerCase();
					if (dsender instanceof DiscordSender && !Arrays.stream(UnconnectedCmds)
							.anyMatch(s -> cmd.equals(s) || cmd.startsWith(s + " "))) {
						// Command not whitelisted
						dsender.sendMessage("Sorry, you can only access these commands:\n"
								+ Arrays.stream(UnconnectedCmds).map(uc -> "/" + uc).collect(Collectors.joining(", "))
								+ (user.getConnectedID(TBMCPlayer.class) == null
										? "\nTo access your commands, first please connect your accounts, using @ChromaBot connect in "
												+ DiscordPlugin.botchannel.mention()
												+ "\nThen you can access all of your regular commands (even offline) in private chat: DM me `mcchat`!"
										: "\nYou can access all of your regular commands (even offline) in private chat: DM me `mcchat`!"));
						return;
					}
					if (lastlist > 5) {
						ListC = 0;
						lastlist = 0;
					}
					if (cmd.equals("list") && Bukkit.getOnlinePlayers().size() == lastlistp && ListC++ > 2) // Lowered already
					{
						dsender.sendMessage("Stop it. You know the answer.");
						lastlist = 0;
					} else {
						int spi = cmd.indexOf(' ');
						final String topcmd = spi == -1 ? cmd : cmd.substring(0, spi);
						Optional<Channel> ch = Channel.getChannels().stream().filter(c -> c.ID.equalsIgnoreCase(topcmd))
								.findAny();
						if (!ch.isPresent())
							VanillaCommandListener.runBukkitOrVanillaCommand(dsender, cmd);
						else {
							Channel chc = ch.get();
							if (!chc.ID.equals(Channel.GlobalChat.ID) && !event.getMessage().getChannel().isPrivate())
								dsender.sendMessage(
										"You can only talk in global in the public chat. DM `mcchat` to enable private chat to talk in the other channels.");
							else {
								if (spi == -1) // Switch channels
								{
									val oldch = dsender.getMcchannel();
									if (oldch instanceof ChatRoom)
										((ChatRoom) oldch).leaveRoom(dsender);
									dsender.setMcchannel(chc);
									if (chc instanceof ChatRoom)
										((ChatRoom) chc).joinRoom(dsender);
									dsender.sendMessage("You're now talking in: "
											+ DiscordPlugin.sanitizeString(dsender.getMcchannel().DisplayName));
								} else // Send single message
									sendChatMessage.accept(chc, cmd.substring(spi + 1));
							}
						}
					}
					lastlistp = (short) Bukkit.getOnlinePlayers().size();
				} else {// Not a command
					if (dmessage.length() == 0 && event.getMessage().getAttachments().size() == 0
							&& !event.getChannel().isPrivate())
						TBMCChatAPI.SendChatMessage(Channel.GlobalChat, dsender, "pinned a message on Discord."); // TODO: Not chat message
					else
						sendChatMessage.accept(dsender.getMcchannel(), dmessage);
					event.getMessage().getChannel().getMessageHistory().stream().forEach(m -> {
						try {
							final IReaction reaction = m.getReactionByUnicode(DiscordPlugin.DELIVERED_REACTION);
							if (reaction != null)
								DiscordPlugin.perform(() -> m.removeReaction(reaction));
						} catch (Exception e) {
							TBMCCoreAPI.SendException("An error occured while removing reactions from chat!", e);
						}
					});
					DiscordPlugin.perform(() -> event.getMessage().addReaction(DiscordPlugin.DELIVERED_REACTION));
				}
			} catch (Exception e) {
				TBMCCoreAPI.SendException("An error occured while handling message \"" + dmessage + "\"!", e);
				return;
			}
		}
	}

	/**
	 * This method will find the best sender to use: if the player is online, use that, if not but connected then use that etc.
	 */
	private static DiscordSenderBase getSender(IChannel channel, final IUser author, DiscordPlayer dp) {
		val key = (channel.isPrivate() ? "" : "P") + author.getStringID();
		return Stream.<Supplier<Optional<DiscordSenderBase>>>of( // https://stackoverflow.com/a/28833677/2703239
				() -> Optional.ofNullable(OnlineSenders.get(key)), // Find first non-null
				() -> Optional.ofNullable(ConnectedSenders.get(key)), // This doesn't support the public chat, but it'll always return null for it
				() -> Optional.ofNullable(UnconnectedSenders.get(key)), () -> {
					val dsender = new DiscordSender(author, channel);
					UnconnectedSenders.put(key, dsender);
					return Optional.of(dsender);
				}).map(Supplier::get).filter(Optional::isPresent).map(Optional::get).findFirst().get();
	}
}
